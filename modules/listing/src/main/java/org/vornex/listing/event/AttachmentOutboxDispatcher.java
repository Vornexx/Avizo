package org.vornex.listing.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.vornex.events.OutboxDispatcher;
import org.vornex.events.entity.OutboxEvent;
import org.vornex.listing.service.StorageService;

/**
 * Dispatcher для aggregateType = "attachment".
 * Ожидаемый payload (JSON):
 * {
 * "attachmentId": 123,
 * "storageKey": "attachments/uuid.jpg"
 * }
 * <p>
 * Поведение:
 * 1) парсим payload,
 * 2) вызываем storage.delete(storageKey) — сетевой вызов,
 * 3) затем транзакционно удаляем запись из БД через AttachmentDeletionService.
 * <p>
 * Исключения при storage.delete приводят к retry (OutboxProcessor обработает scheduleRetry).
 */
@Component
@RequiredArgsConstructor
public class AttachmentOutboxDispatcher implements OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AttachmentOutboxDispatcher.class);

    private final StorageService storage;
    private final AttachmentDeletionService deletionService;
    private final ObjectMapper objectMapper;

    @Override
    public String aggregateType() {
        return "attachment";
    }

    @Override
    public void dispatch(OutboxEvent event) throws Exception {
        System.out.println("DISPATCH START: " + event.getId());
        // 1) parse payload
        Long attachmentId;
        String storageKey;
        try {
            JsonNode root = objectMapper.readTree(event.getPayload());
            if (root == null) throw new IllegalArgumentException("payload is null");

            JsonNode aid = root.get("attachmentId");
            JsonNode sk = root.get("storageKey");

            if (aid == null || aid.isNull() || sk == null || sk.isNull()) {
                throw new IllegalArgumentException("payload must contain 'attachmentId' and 'storageKey'");
            }

            attachmentId = aid.asLong();
            storageKey = sk.asText();
        } catch (Exception e) {
            // Неправильный payload — логируем и пробрасываем исключение,
            // OutboxProcessor либо пометит processed либо переместит в dead-letter.
            log.error("Invalid payload for outbox {}: {}", event.getId(), e.getMessage(), e);
            throw e;
        }

        // 2) external call: delete object from storage
        //    Эта операция может упасть (сеть/провайдер). В этом случае мы хотим, чтобы
        //    OutboxProcessor провёл retry, поэтому пробрасываем исключение дальше.
        storage.delete(storageKey);
        log.info("Deleted storage object {}", storageKey);

        // 3) DB side: транзакционно удалить attachment (если есть).
        //    Мы вызываем отдельный сервис (AttachmentDeletionService) с @Transactional,
        //    чтобы транзакция открылась через Spring proxy (нет self-invocation).
        deletionService.deleteIfExists(attachmentId);
        log.info("Deleted attachment {} in DB", attachmentId);

        log.info("Attachment outbox event {} handled: attachmentId={}, storageKey={}", event.getId(), attachmentId, storageKey);
    }
}
