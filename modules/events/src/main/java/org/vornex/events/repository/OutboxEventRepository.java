package org.vornex.events.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vornex.events.entity.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    // Берём не-обработанные события, ready для обработки (nextAttemptAt <= now), с ограничением по batch
    @Query("select e from OutboxEvent e where e.processedAt is null and e.nextAttemptAt <= :now order by e.createdAt")
    List<OutboxEvent> findReadyEvents(Instant now, Pageable pageable);

    @Modifying
    @Query("update OutboxEvent e set e.attempts = :attempts, e.nextAttemptAt = :nextAttemptAt where e.id = :id")
    int updateAttemptsAndNextAttemptAt(
            @Param("id") UUID id,
            @Param("attempts") int attempts,
            @Param("nextAttemptAt") Instant nextAttemptAt
    );
    @Modifying
    @Query("update OutboxEvent e set e.processedAt = :processedAt where e.id = :id")
    int markProcessed(
            @Param("id") UUID id,
            @Param("processedAt") Instant processedAt
    );

    /**
     * Проверяет, есть ли непроцессированное (processedAt IS NULL) событие
     * с тем же aggregateType, aggregateId и eventType.
     * <p>
     * Используется чтобы не создавать дублирующие задачи (простая защита).
     * Возвращает true, если такое событие уже существует и ещё не обработано.
     */
    boolean existsByAggregateTypeAndAggregateIdAndEventTypeAndProcessedAtIsNull(
            String aggregateType, String aggregateId, String eventType);
}
