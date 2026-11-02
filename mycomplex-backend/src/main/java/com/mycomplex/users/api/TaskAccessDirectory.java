package com.mycomplex.users.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Public users-module projection used by administrative task workflows. */
public interface TaskAccessDirectory {

    Optional<Administrator> findActiveAdministrator(
            UUID propertyId,
            String auth0Subject
    );

    Optional<Worker> findEligibleWorker(
            UUID propertyId,
            UUID membershipId
    );

    List<Worker> findEligibleWorkers(UUID propertyId);

    Optional<UUID> findActiveUserId(String auth0Subject);

    record Administrator(UUID userId, UUID membershipId) { }

    record Worker(UUID userId, UUID membershipId, String displayName) { }
}
