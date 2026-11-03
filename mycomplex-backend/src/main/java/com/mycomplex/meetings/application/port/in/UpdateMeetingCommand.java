package com.mycomplex.meetings.application.port.in;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record UpdateMeetingCommand(
        UUID propertyId,
        UUID meetingId,
        String auth0Subject,
        String title,
        String description,
        URI meetingUrl,
        Instant scheduledAt,
        long version
) {
}
