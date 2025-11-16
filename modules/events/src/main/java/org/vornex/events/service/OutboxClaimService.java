package org.vornex.events.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.vornex.events.dto.ClaimedEvent;
import org.vornex.events.entity.OutboxEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    private final EntityManager em;

    private static final long IN_FLIGHT_LOCK_SECONDS = 60L; // резервирование события при claim

    @Transactional
    public List<ClaimedEvent> claimReadyEvents(int limit) {
        Instant now = Instant.now();

        TypedQuery<OutboxEvent> q = em.createQuery(
                "select e from OutboxEvent e " +
                        "where e.processedAt is null and e.nextAttemptAt <= :now " +
                        "order by e.createdAt",
                OutboxEvent.class
        );
        q.setParameter("now", now);
        q.setMaxResults(limit);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);

        List<OutboxEvent> rows = q.getResultList();
        List<ClaimedEvent> claimed = new ArrayList<>(rows.size());

        for (OutboxEvent ev : rows) {
            int newAttempts = ev.getAttempts() + 1;
            ev.setAttempts(newAttempts);
            ev.setNextAttemptAt(now.plusSeconds(IN_FLIGHT_LOCK_SECONDS));
            em.merge(ev);

            claimed.add(new ClaimedEvent(
                    ev.getId(),
                    ev.getAggregateType(),
                    ev.getAggregateId(),
                    ev.getEventType(),
                    ev.getPayload(),
                    newAttempts,
                    ev.getMaxAttempts()
            ));
        }

        return claimed;
    }
}
