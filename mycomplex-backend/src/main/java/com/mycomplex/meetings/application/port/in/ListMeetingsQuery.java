package com.mycomplex.meetings.application.port.in;

import java.util.UUID;

public record ListMeetingsQuery(
        UUID propertyId,
        String auth0Subject
) {
}
