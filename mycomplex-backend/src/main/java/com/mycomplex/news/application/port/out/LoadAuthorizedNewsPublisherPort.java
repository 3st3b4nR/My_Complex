package com.mycomplex.news.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface LoadAuthorizedNewsPublisherPort {

    Optional<UUID> findActiveAdministratorId(
            UUID propertyId,
            String auth0Subject
    );
}