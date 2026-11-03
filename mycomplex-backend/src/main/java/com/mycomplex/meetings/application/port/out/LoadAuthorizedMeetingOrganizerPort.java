package com.mycomplex.meetings.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface LoadAuthorizedMeetingOrganizerPort {

    Optional<UUID> findActiveAdministratorId(
            UUID propertyId,
            String auth0Subject
    );
}
