package org.vornex.events;

import org.vornex.events.entity.OutboxEvent;

public interface OutboxDispatcher {
    /**
     * Уникальный агрегат-тип, который этот диспетчер обрабатывает.
     * Например: "attachment", "listing"
     */
    String aggregateType();

    /**
     * Выполнить обработку события (payload уже внутри OutboxEvent).
     * Реализация должна:
     * - выполнить внешние/side-effect действия (например delete в S3),
     * - выполнить необходимые атомарные изменения в своей микросхеме/модуле (например удалить запись Attachment в БД) —
     * обычно в отдельной транзакции внутри dispatcher.
     * <p>
     * Бросать исключение при фейле — тогда OutboxProcessor будет планировать retry.
     */
    void dispatch(OutboxEvent event) throws Exception;
}
