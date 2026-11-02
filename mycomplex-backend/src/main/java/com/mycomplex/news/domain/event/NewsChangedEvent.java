package com.mycomplex.news.domain.event;

import java.time.Instant;
import java.util.UUID;

public record NewsChangedEvent(
        UUID newsId,
        UUID propertyId,
        UUID actorId,
        String title,
        ChangeType changeType,
        Instant occurredAt
) {

    public enum ChangeType {
        CREATED,
        UPDATED
    }
}