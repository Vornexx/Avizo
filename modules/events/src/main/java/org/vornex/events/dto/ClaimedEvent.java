package org.vornex.events.dto;

import org.vornex.events.entity.OutboxEvent;

import java.util.UUID;

/**
 * Класс-обёртка для snapshot'а события, используемого вне транзакции.
 * Мы берем snapshot (копию) данных события при claim, чтобы не держать entity в persistence context
 * и не держать DB lock во время сетевых вызовов.
 */
public record ClaimedEvent(UUID id, String aggregateType, String aggregateId, String eventType, String payload,
                           int attempts, int maxAttempts) {
    // Восстановить минимальный OutboxEvent (некоторые dispatcher'ы требуют OutboxEvent)
    public OutboxEvent toOutboxEvent() {
        OutboxEvent ev = new OutboxEvent();
        ev.setId(id);
        ev.setAggregateType(aggregateType);
        ev.setAggregateId(aggregateId);
        ev.setEventType(eventType);
        ev.setPayload(payload);
        ev.setAttempts(attempts);
        ev.setMaxAttempts(maxAttempts);
        return ev;
    }
}