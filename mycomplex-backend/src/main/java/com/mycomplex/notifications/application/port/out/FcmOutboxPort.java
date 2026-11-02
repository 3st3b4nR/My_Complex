package com.mycomplex.notifications.application.port.out;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mycomplex.notifications.api.NotificationDeliveryStatus;
import com.mycomplex.notifications.domain.FcmOutboxEvent;
import com.mycomplex.notifications.domain.NotificationEventType;

public interface FcmOutboxPort {
    void enqueue(UUID taskId, UUID targetUserId, UUID membershipId,
            NotificationEventType eventType);
    Map<UUID, NotificationDeliveryStatus> findLatestStatuses(
            Collection<UUID> taskIds);
    Optional<FcmOutboxEvent> claimNext();
    void markSent(UUID eventId, String externalMessageId);
    void markNoDestination(UUID eventId, String safeReason);
    void markFailed(UUID eventId, String safeReason);
    void scheduleRetry(UUID eventId, int attempts, Duration delay,
            String safeReason);
}
