package com.mycomplex.notifications.application.port.out;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.mycomplex.notifications.application.model.EmailNotification;
import com.mycomplex.notifications.domain.EmailOutboxEvent;

public interface EmailOutboxPort {

    void enqueue(EmailNotification notification);

    Optional<EmailOutboxEvent> claimNext();

    void markSent(UUID eventId, String externalMessageId);

    void markFailed(UUID eventId, String safeReason);

    void scheduleRetry(
            UUID eventId,
            int attempts,
            Duration delay,
            String safeReason
    );
}
