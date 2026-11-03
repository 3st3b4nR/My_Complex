package com.mycomplex.meetings.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface LoadAuthorizedMeetingViewerPort {

    Optional<UUID> findActiveMemberId(
            UUID propertyId,
            String auth0Subject
    );
}
