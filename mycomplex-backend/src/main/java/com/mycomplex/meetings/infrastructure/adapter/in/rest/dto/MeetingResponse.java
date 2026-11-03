package com.mycomplex.meetings.infrastructure.adapter.in.rest.dto;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record MeetingResponse(
        UUID meetingId,
        UUID propertyId,
        String title,
        String description,
        URI meetingUrl,
        Instant scheduledAt,
        Instant updatedAt,
        long version
) {
}
