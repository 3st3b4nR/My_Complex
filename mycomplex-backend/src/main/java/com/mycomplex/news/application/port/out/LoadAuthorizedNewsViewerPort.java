package com.mycomplex.news.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface LoadAuthorizedNewsViewerPort {

    Optional<UUID> findActiveMemberId(
            UUID propertyId,
            String auth0Subject
    );
}
