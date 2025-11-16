package org.vornex.listing.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vornex.authapi.SecurityContextUtils;
import org.vornex.exception.BadRequestException;
import org.vornex.exception.ConflictException;
import org.vornex.exception.ForbiddenException;
import org.vornex.exception.NotFoundException;
import org.vornex.listing.dto.CreateListingDto;
import org.vornex.listing.dto.ListingResponseDto;
import org.vornex.listing.dto.UpdateListingDto;
import org.vornex.listing.entity.Attachment;
import org.vornex.listing.entity.Listing;
import org.vornex.listing.enums.ItemCondition;
import org.vornex.listing.enums.ListingStatus;
import org.vornex.listing.enums.ModerationStatus;
import org.vornex.listing.mapper.ListingMapper;
import org.vornex.listing.repository.AttachmentRepository;
import org.vornex.listing.repository.ListingRepository;
import org.vornex.listing.service.AttachmentService;
import org.vornex.listing.service.ListingService;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService; // для удаления attachment'ов (outbox)
    private final SecurityContextUtils securityUtils;         // getCurrentUserId(), isAdmin()
    private final ListingMapper mapper;

    // Константы / конвенции
    private static final int MIN_TITLE_LENGTH = 3;
    private static final int MAX_TITLE_LENGTH = 120;

    // ------------------- createListing -------------------

    /**
     * Создание объявления (черновик).
     * <p>
     * Правила:
     * - ownerId берём из SecurityUtils (игнорируем owner из DTO).
     * - status = DRAFT, moderationStatus = NOT_REQUIRED.
     * - Привязываем уже загруженные attachments (если переданы) к объявлению.
     * - Валидируем простые инварианты (title, price->amount >= 0, category not blank).
     * <p>
     * Возвращает готовый ListingResponseDto.
     */
    @Override
    @Transactional
    public ListingResponseDto createListing(CreateListingDto dto) {
        Objects.requireNonNull(dto, "create dto required");

        // Owner должен быть аутентифицирован — получаем UUID или бросаем UnauthorizedException
        UUID ownerId = securityUtils.getCurrentUserIdRequired();

        // Basic validation (KISS)
        validateTitle(dto.title());
        if (dto.category() == null || dto.category().isBlank()) {
            throw new BadRequestException("category is required");
        }
        if (dto.price() != null && dto.price().getAmount().signum() < 0) {
            throw new BadRequestException("price must be >= 0");
        }

        Listing listing = Listing.builder()
                .title(dto.title().trim())
                .description(trimOrNull(dto.description()))
                .price(dto.price())
                .category(dto.category().trim())
                .itemCondition(Optional.ofNullable(dto.itemCondition()).orElse(ItemCondition.USED))
                .status(ListingStatus.DRAFT)
                .moderationStatus(ModerationStatus.NOT_REQUIRED)
                .ownerId(ownerId)
                .attributes(Optional.ofNullable(dto.attributes()).orElse(Map.of()))
                .build();

        // Persist listing first to have id for attachments FK
        listing = listingRepository.save(listing);

        // Attach attachments if provided
        if (dto.attachmentIds() != null && !dto.attachmentIds().isEmpty()) {
            attachAttachmentsToListing(dto.attachmentIds(), listing);
        }

        return mapper.toDto(listing); //public Url там есть
    }

    // ------------------- getById -------------------

    /**
     * Получение объявления по id.
     * Используем fetch attachments методом findByIdWithAttachments чтобы избежать N+1 при маппинге.
     * Благодаря @Where на сущности Listing возвращаются только не-deleted.
     */
    @Override
    @Transactional(readOnly = true)
    public ListingResponseDto getById(UUID id) {
        Listing listing = listingRepository.findByIdWithAttachments(id)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + id));
        return mapper.toDto(listing);
    }

    // ------------------- updateListing (PATCH semantics) -------------------

    /**
     * Частичное обновление объявления (PATCH semantics).
     * <p>
     * Правила:
     * - Только владелец или admin может редактировать.
     * - Если DTO содержит version — проверяем optimistic version и кидаем ConflictException при несоответствии.
     * - Поля, равные null в DTO — не меняются.
     * - Для attachmentIds: если переданы — заменяем текущие attachments этим списком.
     */
    @Override
    @Transactional
    public ListingResponseDto updateListing(UUID id, UpdateListingDto dto) {
        Objects.requireNonNull(dto, "update dto required");

        Listing listing = listingRepository.findByIdWithAttachments(id)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + id));

        UUID currentUser = securityUtils.getCurrentUserIdRequired();
        if (isForbiddenToEdit(listing, currentUser)) {
            throw new ForbiddenException("Not allowed to edit listing");
        }

        // Optimistic version check (optional)
        if (dto.getVersion() != null && !dto.getVersion().equals(listing.getVersion())) {
            throw new ConflictException("Version conflict: entity was modified");
        }

        // Apply patch: only non-null fields
        if (dto.getTitle() != null) {
            validateTitle(dto.getTitle());
            listing.setTitle(dto.getTitle().trim());
        }
        if (dto.getDescription() != null) {
            listing.setDescription(trimOrNull(dto.getDescription()));
        }
        if (dto.getPrice() != null) {
            if (dto.getPrice().getAmount().signum() < 0)
                throw new BadRequestException("price must be >= 0");
            listing.setPrice(dto.getPrice());
        }
        if (dto.getCategory() != null) {
            if (dto.getCategory().isBlank())
                throw new BadRequestException("category cannot be blank");
            listing.setCategory(dto.getCategory().trim());
        }
        if (dto.getItemCondition() != null) listing.setItemCondition(dto.getItemCondition());
        if (dto.getAttributes() != null) listing.setAttributes(dto.getAttributes());

        // Attachments: replace if provided
        if (dto.getAttachmentIds() != null) {
            replaceAttachments(dto.getAttachmentIds(), listing);
        }

        listing = listingRepository.save(listing);
        return mapper.toDto(listing);
    }

    // ------------------- publish -------------------

    /**
     * Публикация объявления.
     * <p>
     * Правила:
     * - Только владелец или admin.
     * - Обязательные поля: title, price (если бизнес требует), category.
     * - Опционально: require >=1 processed attachment.
     * - Смена статуса на PUBLISHED и установка publishedAt.
     */
    @Override
    @Transactional
    public void publish(UUID id) {
        Listing listing = listingRepository.findByIdWithAttachments(id)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + id));

        UUID currentUser = securityUtils.getCurrentUserIdRequired();
        if (isForbiddenToEdit(listing, currentUser)) {
            throw new ForbiddenException("Not allowed to publish listing");
        }

        // Mandatory checks
        if (listing.getTitle() == null || listing.getTitle().isBlank())
            throw new BadRequestException("title is required");
        if (listing.getCategory() == null || listing.getCategory().isBlank())
            throw new BadRequestException("category is required");
        if (listing.getPrice() == null || listing.getPrice().getAmount().signum() <= 0)
            throw new BadRequestException("price must be provided and > 0");

        // (Optional) require at least one processed attachment to publish
        boolean hasProcessedAttachment = listing.getAttachments().stream().anyMatch(Attachment::isProcessed);
        if (!hasProcessedAttachment) {
            throw new BadRequestException("At least one uploaded image is required to publish");
        }

        // Update status
        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setPublishedAt(Instant.now());
        listingRepository.save(listing);

        // Note: moderation workflow could be triggered here (async).
    }

    // ------------------- deleteListing -------------------

    /**
     * Удаление объявления (soft-delete).
     * <p>
     * Алгоритм:
     * - Проверяем права (owner/admin).
     * - Планируем удаление attachments через AttachmentService (outbox) — делаем это до удаления listing,
     * чтобы background worker удалил файлы.
     * - Удаляем listing (cascade orphanRemoval = true / soft-delete via @SQLDelete).
     * <p>
     * Все операции выполняются в одной транзакции: если транзакция закоммитится, outbox события и пометка deleted будут сохранены.
     */
    @Override
    @Transactional
    public void deleteListing(UUID id) {
        Listing listing = listingRepository.findByIdWithAttachments(id)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + id));

        UUID currentUser = securityUtils.getCurrentUserIdRequired();
        if (isForbiddenToEdit(listing, currentUser)) {
            throw new ForbiddenException("Not allowed to delete listing");
        }

        // Schedule attachments for deletion (non-blocking; AttachmentService creates outbox events)
        for (Attachment att : new ArrayList<>(listing.getAttachments())) {
            try {
                attachmentService.delete(att); // this creates outbox event for each attachment
            } catch (Exception ex) {
                log.warn("Failed to schedule deletion for attachment {}: {}", att.getId(), ex.getMessage());
                // We do not fail the whole operation; log and continue — attachments cleanup can be retried manually.
            }
        }

        listingRepository.delete(listing);
        log.info("Listing {} deleted by user {}", listing.getId(), currentUser);
    }

    // ------------------- incrementViews -------------------

    /**
     * Инкремент просмотров оптимизированно (atomic DB update).
     * Мы не загружаем entity в память; используем UPDATE ... = ... + 1.
     */
    @Override
    @Transactional
    public void incrementViews(UUID id) {
        int updated = listingRepository.incrementViews(id);
        if (updated == 0) {
            // listing not found — throw NotFound to be explicit
            throw new NotFoundException("Listing not found: " + id);
        }
    }


    private void attachAttachmentsToListing(List<Long> attachmentIds, Listing listing) {
        // Fetch attachments
        List<Attachment> attachments = attachmentRepository.findAllById(attachmentIds);

        // Validation: ensure all requested attachments exist
        if (attachments.size() != attachmentIds.size()) {
            throw new BadRequestException("One or more attachments not found");
        }

        // Ensure attachments are processed (uploaded and validated)
        for (Attachment a : attachments) {
            if (!a.isProcessed()) {
                throw new BadRequestException("Attachment " + a.getId() + " is not uploaded/processed yet");
            }
            // Set listing FK — Attachment is the owning side
            a.setListing(listing);
        }

        // Проверка лимита и установка позиции
        assignPositionsAndLinkAttachments(attachments, listing);


        // Save attachments (cascade may persist them)
        attachmentRepository.saveAll(attachments);
        // listing.attachments will be populated on next fetch / flush
    }

    private void assignPositionsAndLinkAttachments(List<Attachment> attachments, Listing listing) {
        int maxPos = attachmentRepository.findMaxPositionByListingId(listing.getId());
        for (Attachment a : attachments) {
            if (maxPos >= 10) {
                throw new BadRequestException("Max 10 attachments per listing");
            }
            maxPos++;
            a.setPosition(maxPos);
            a.setListing(listing);
        }
    }

    private void replaceAttachments(List<Long> attachmentIds, Listing listing) {
        // Remove current attachments not in new list, and attach new ones
        List<Attachment> newAttachments = attachmentRepository.findAllById(attachmentIds);
        if (newAttachments.size() != attachmentIds.size()) {
            throw new BadRequestException("One or more attachments not found");
        }
        // Validate processed
        for (Attachment a : newAttachments) {
            if (!a.isProcessed()) throw new BadRequestException("Attachment " + a.getId() + " is not processed");
        }
        // Orphan removal + cascade on Listing.attachments allows us to replace list safely:
        listing.getAttachments().clear();

        // Установка позиции и linking
        assignPositionsAndLinkAttachments(newAttachments, listing);

        listing.getAttachments().addAll(newAttachments);
    }

    private boolean isForbiddenToEdit(Listing listing, UUID userId) {
        return !securityUtils.isAdmin() && !Objects.equals(listing.getOwnerId(), userId);
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) throw new BadRequestException("title is required");
        int len = title.trim().length();
        if (len < MIN_TITLE_LENGTH || len > MAX_TITLE_LENGTH) {
            throw new BadRequestException("title length must be between " + MIN_TITLE_LENGTH + " and " + MAX_TITLE_LENGTH);
        }
    }

    private String trimOrNull(String s) {
        return s == null ? null : s.trim();
    }


}
