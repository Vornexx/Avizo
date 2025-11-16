package org.vornex.events.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_processed_next_try", columnList = "processed_at, next_attempt_at")
})
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Тип агрегата (например "attachment")
    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    // id агрегата (attachment id or storageKey) — для отладки и поиска
    @Column(name = "aggregate_id", nullable = false, length = 128)
    private String aggregateId;

    // Тип события, например "ATTACHMENT_DELETE_REQUESTED"
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    // payload — json с данными (storageKey, attachmentId, userId и т.д.)
    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    // Retry / delivery
    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 10;

    // Когда пробовать в следующий раз
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    // Когда событие обработано успешно
    /**
     * // вместо boolean processed (обработан) true/false у нас время когда был обработан.
     * В запросе будет WHERE processed_at IS NULL
     */
    @Column(name = "processed_at")
    private Instant processedAt;


    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // getters/setters, конструкторы
    public OutboxEvent() {
    }

    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payload, int maxAttempts, Instant nextAttemptAt) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.maxAttempts = maxAttempts;
        this.nextAttemptAt = nextAttemptAt != null ? nextAttemptAt : Instant.now();
    }
}
