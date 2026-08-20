package com.sergio.planix.notification;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    List<NotificationOutbox> findBySentAtIsNullOrderByIdAsc(Limit limit);

    @Modifying
    @Query(value = """
            insert into notification_outbox (event_id, type, payload)
            values (:eventId, :type, cast(:payload as jsonb))
            on conflict (event_id) do nothing
            """, nativeQuery = true)
    int inserirIgnorandoDuplicado(@Param("eventId") UUID eventId,
                                  @Param("type") String type,
                                  @Param("payload") String payload);

    @Modifying
    int deleteBySentAtBefore(OffsetDateTime limite);
}
