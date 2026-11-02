package com.mycomplex.notifications.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FcmOutboxEvent(
        UUID id,
        UUID taskId,
        UUID targetUserId,
        UUID assignmentMembershipId,
        NotificationEventType eventType,
        int attempts,
        OffsetDateTime nextAttemptAt
) { }
