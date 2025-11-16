package org.vornex.listing.event;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.vornex.listing.entity.Attachment;
import org.vornex.listing.repository.AttachmentRepository;

@Service
@RequiredArgsConstructor
public class AttachmentDeletionService {
    private static final Logger log = LoggerFactory.getLogger(AttachmentDeletionService.class);

    private final AttachmentRepository attachmentRepository;

    /**
     * Транзакционно удаляет attachment по id, если он существует.
     * - Если записи нет — считаем это нормальным (идемпотентность).
     * - Если удаление упало — бросается исключение, которое вызовет retry у OutboxProcessor.
     */
    @Transactional
    public void deleteIfExists(Long attachmentId) {
        log.info("Попытка удалить attachment {}", attachmentId);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalStateException("Attachment not found: " + attachmentId));
        attachmentRepository.delete(attachment);
        log.info("Attachment {} deleted from DB", attachmentId);
    }
}

