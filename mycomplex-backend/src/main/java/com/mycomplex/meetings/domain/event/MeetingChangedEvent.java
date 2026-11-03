package com.mycomplex.meetings.domain.event;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record MeetingChangedEvent(
        UUID meetingId,
        UUID propertyId,
        String title,
        String description,
        URI meetingUrl,
        Instant scheduledAt,
        ChangeType changeType,
        Instant occurredAt
) {

    public enum ChangeType {
        CREATED,
        UPDATED,
        REMINDER
    }
}
