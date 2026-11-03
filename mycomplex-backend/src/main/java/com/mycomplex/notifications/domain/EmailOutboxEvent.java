package com.mycomplex.notifications.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmailOutboxEvent(
        UUID id,
        String recipient,
        String subject,
        String body,
        String deliveryKey,
        int attempts,
        OffsetDateTime nextAttemptAt
) { }
