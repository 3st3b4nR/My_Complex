package com.mycomplex.meetings.application.port.in;

import java.util.UUID;

public record DeleteMeetingCommand(
        UUID propertyId,
        UUID meetingId,
        String auth0Subject,
        long version
) {
}
