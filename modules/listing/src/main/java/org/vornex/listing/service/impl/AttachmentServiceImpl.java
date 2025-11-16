package org.vornex.listing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.vornex.events.entity.OutboxEvent;
import org.vornex.events.repository.OutboxEventRepository;
import org.vornex.exception.BadRequestException;
import org.vornex.exception.NotFoundException;
import org.vornex.listing.dto.attachmentDto.AttachmentCompletedResponseDto;
import org.vornex.listing.dto.attachmentDto.AttachmentRequestDto;
import org.vornex.listing.dto.attachmentDto.AttachmentResponseDto;
import org.vornex.listing.entity.Attachment;
import org.vornex.listing.entity.Listing;
import org.vornex.listing.repository.AttachmentRepository;
import org.vornex.listing.service.AttachmentService;
import org.vornex.listing.service.StorageService;
import org.vornex.listing.util.AttachmentProperties;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final StorageService storage;
    private final AttachmentProperties props;
    private final OutboxEventRepository outboxEventRepository;   // общая таблица outbox (shared module)
    private final ObjectMapper objectMapper;                     // для сериализации payload в JSON
    private final EntityManager entityManager;

    private static final String AGGREGATE_TYPE = "attachment";
    private static final String EVENT_TYPE_DELETE = "ATTACHMENT_DELETE_REQUESTED";
    private static final int DEFAULT_MAX_ATTEMPTS = 10;

    /**
     * Выдаёт presigned PUT URL и создаёт "черновую" запись Attachment в БД.
     * <p>
     * Порядок действий:
     * 1. Валидация входа: fileName, contentType, size (и проверка разрешённых типов/максимума).
     * 2. Определение расширения: из имени файла или по contentType.
     * 3. Генерация уникального storageKey (например attachments/{uuid}{ext}).
     * 4. Создание записи Attachment в БД: processed=false, uploadedAt=null — чтобы можно было отслеживать "незавершённые" загрузки.
     * 5. Запрос presigned PUT у StorageService и возврат DTO с uploadUrl, key и expiresAt.
     * <p>
     * Замечания:
     * - ListingId в этом моменте может отсутствовать; привязка происходит при создании/публикации объявления.
     * - Не сохраняем presigned URL в БД (он временный).
     */
    @Transactional
    public AttachmentResponseDto presignUrl(AttachmentRequestDto request) {
        validateRequestForPresign(request);

        String ext = extensionFromFilename(request.fileName())
                .orElseGet(() -> guessExtFromContentType(request.contentType())); // для ключа attachments/{uuid}{ext} ext = fileName если нет то расширение.
        String key = buildStorageKey(ext);

        // Сохраняем запись до загрузки. Это позволяет:
        // - отследить "брошенные" загрузки и удалить мусор
        // - вернуть ключ клиенту, чтобы он позже подтвердил загрузку (complete)
        Attachment attachment = new Attachment(); // спец. не указываем size т.к. если будет сравнивать ожидаемый и реальный много лишних удалений будет. Основная валидация уже есть в validateRequestForPresign
        attachment.setStorageKey(key);
        attachment.setListing(entityManager.getReference(Listing.class, request.listingId()));
        attachment.setContentType(request.contentType());
        attachment.setProcessed(false);
        attachment.setUploadedAt(null);
        attachmentRepository.save(attachment);

        StorageService.PresignResult presign = storage.presignPut(key, request.contentType(), props.getPresignTtl());

        return new AttachmentResponseDto(presign.url(), key, presign.expiresAt());
    }

    /**
     * Завершает загрузку: подтверждает, что объект существует и соответствует ожиданиям.
     * <p>
     * Шаги:
     * 1. Находит Attachment по storageKey. Если не найден — 404.
     * 2. Idempotency: если уже processed=true — возвращаем текущие данные.
     * 3. Делает HEAD в storage, проверяет наличие и получает contentLength/contentType.
     * 4. Проверяет ожидаемый size (если он задан в Attachment) — при несоответствии очищает объект и запись.
     * 5. Проверяет contentType — если запрещён, очищает и падает.
     * 6. Валидирует содержимое: берёт первые N байт через storage.getRange и пытается распарсить ImageIO.
     * Если невалидно — удаляет объект и запись.
     * 7. Если всё ок — помечает attachment.processed=true, записывает uploadedAt и сохраняет запись.
     * 8. Возвращает id + публичный URL (storage.publicUrl(key)).
     * <p>
     * Замечания:
     * - Метод синхронный; для высокой нагрузки лучше обрабатывать асинхронно через S3 events + worker.
     * - Валидация ImageIO — базовая; для продакшена можно добавить проверку mime-bytes, скан антивирусом, ограничение разрешения.
     */
    @Transactional
    public AttachmentCompletedResponseDto completeUpload(String key) {
        // 1) Найти attachment по key
        Attachment attachment = attachmentRepository.findByStorageKey(key)
                .orElseThrow(() -> new NotFoundException("Attachment not found for key: " + key));

        if (Boolean.TRUE.equals(attachment.isProcessed())) {
            // idempotency: повторный complete — возвращаем текущие данные
            return new AttachmentCompletedResponseDto(attachment.getId(), storage.publicUrl(key));
        }

        // 2) HEAD в хранилище (проверяем наличие)
        StorageService.HeadResult head;
        try {
            head = storage.head(key);
        } catch (NotFoundException e) {
            // пользователь не загрузил файл или удалил
            throw new BadRequestException("Object not found in storage for key: " + key);
        }


        attachment.setSize(head.contentLength());


        // 4) Content-Type проверка
        if (!isAllowedContentType(head.contentType())) {
            safeDeleteAndRemove(key, attachment);
            throw new BadRequestException("Uploaded content type is not allowed: " + head.contentType());
        }
        attachment.setContentType(head.contentType());

        // 5) Basic image validation: читаем первые N байт и пробуем ImageIO
        final long maxCheckBytes = Math.min(head.contentLength(), 2L * 1024 * 1024); // например 2MB (читаем не больше 2 МБ (или меньше, если файл меньше))
        try (InputStream is = storage.getRange(key, maxCheckBytes)) {
            BufferedImage img = ImageIO.read(is); //попытка распарсить картинку, если не получилось значит невалидное/битое изображение.
            if (img == null) {
                safeDeleteAndRemove(key, attachment);
                throw new BadRequestException("Uploaded file is not a valid image");
            }
        } catch (Exception e) {
            safeDeleteAndRemove(key, attachment);
            throw new BadRequestException("Failed to validate image: " + e.getMessage());
        }

        // 6) Всё в порядке — пометим как processed, сохраним uploadedAt и вернём public URL
        attachment.setProcessed(true);
        attachment.setUploadedAt(Instant.now());
        attachmentRepository.save(attachment);

        String publicUrl = storage.publicUrl(key);
        return new AttachmentCompletedResponseDto(attachment.getId(), publicUrl);
    }

    /**
     * Запрашивает удаление attachment: создаёт outbox-событие и (опционально) помечает запись.
     * Метод транзакционный — сохранение outbox и изменение attachment происходят атомарно.
     */
    @Transactional
    public void delete(Attachment attachment) {
        // --- 0. базовая валидация входа ---
        if (attachment == null) throw new IllegalArgumentException("attachment is required");
        Long attachmentId = attachment.getId();
        if (attachmentId == null) throw new IllegalArgumentException("attachment id is null");

        String storageKey = attachment.getStorageKey();
        if (!StringUtils.hasText(storageKey)) {
            throw new IllegalStateException("storageKey is empty for attachmentId=" + attachmentId);
        }

        // --- 1. защита от явных дублей: есть ли уже pending delete для этого attachment? ---
        boolean alreadyScheduled = outboxEventRepository
                .existsByAggregateTypeAndAggregateIdAndEventTypeAndProcessedAtIsNull(
                        AGGREGATE_TYPE, String.valueOf(attachmentId), EVENT_TYPE_DELETE);

        if (alreadyScheduled) {
            log.info("Delete already scheduled for attachmentId={}, skipping", attachmentId);
            return;
        }

        // --- 2. подготовка payload (JSON) для dispatcher'а ---
        String payload;
        try {
            var node = objectMapper.createObjectNode();
            node.put("attachmentId", attachmentId);
            node.put("storageKey", storageKey);
            payload = objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox payload for attachmentId=" + attachmentId, e);
        }

        int maxAttempts = Optional.ofNullable(props).map(AttachmentProperties::getOutboxMaxAttempts).orElse(DEFAULT_MAX_ATTEMPTS);

        // --- 3. создаём outbox событие (в той же транзакции) ---
        OutboxEvent ev = new OutboxEvent(
                AGGREGATE_TYPE,
                String.valueOf(attachmentId),
                EVENT_TYPE_DELETE,
                payload,
                maxAttempts,
                Instant.now() // nextAttemptAt = сейчас => обработка может начаться немедленно
        );
        outboxEventRepository.save(ev);
    }

    // ----------------- HELPERS -----------------


    /**
     * Валидирует DTO, который приходит на presign:
     * - fileName обязателен,
     * - contentType обязателен и должен быть в allowed list,
     * - size не должен превышать maxFileSize (если передан).
     * <p>
     * Бросает BadRequestException при нарушениях.
     */
    private void validateRequestForPresign(AttachmentRequestDto req) {
        if (req == null) throw new BadRequestException("Request body is required");

        if (!StringUtils.hasText(req.fileName())) {
            throw new BadRequestException("fileName is required");
        }

        if (!StringUtils.hasText(req.contentType())) {
            throw new BadRequestException("contentType is required");
        }

        if (!isAllowedContentType(req.contentType())) {
            throw new BadRequestException("Unsupported content type: " + req.contentType());
        }

        if (req.size() == null) {
            throw new BadRequestException("size is required");
        }

        if (req.size() <= 0) {
            throw new BadRequestException("size must be greater than 0");
        }

        if (req.size() > props.getMaxFileSize()) {
            throw new BadRequestException("File too large (max " + props.getMaxFileSize() + " bytes)");
        }
    }

    /**
     * Проверяет, соответствует ли тип файла разрешенному в StorageProperties.
     */
    private boolean isAllowedContentType(String contentType) {
        return Arrays.stream(props.getAllowedContentTypes())
                .anyMatch(allowed -> allowed.equalsIgnoreCase(contentType));
    }


    /**
     * Безопасно удаляет объект из storage и запись из БД.
     * Ошибки при удалении логируются и игнорируются — метод best-effort.
     * <p>
     * Используется в ветках rollback'а при неконсистентных/опасных загрузках.
     */
    private void safeDeleteAndRemove(String key, Attachment attachment) {
        try {
            storage.delete(key);
        } catch (Exception ignored) {
        }
        try {
            attachmentRepository.delete(attachment);
        } catch (Exception ignored) {
        }
    }

    /**
     * Генерирует storageKey в формате "attachments/{uuid}{extension}".
     * Можно расширить: добавить дату, userId, listingId и т.д.
     */
    private String buildStorageKey(String extension) {
        // Формат: attachments/{uuid}{ext} — можно добавить подпапки по дате, userId и т.д.
        return "attachments/" + UUID.randomUUID() + extension;
    }


    /**
     * Попытка извлечь расширение из имени файла. Возвращает Optional.empty(), если невалидно.
     */
    private Optional<String> extensionFromFilename(String filename) {
        if (filename == null) return Optional.empty();
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return Optional.empty();
        String ext = filename.substring(idx).toLowerCase();
        // простая валидация ext
        if (ext.matches("\\.[a-z0-9]{1,6}")) return Optional.of(ext);
        return Optional.empty();
    }

    /**
     * Простейшая маппинг-функция contentType -> расширение.
     */
    private String guessExtFromContentType(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
