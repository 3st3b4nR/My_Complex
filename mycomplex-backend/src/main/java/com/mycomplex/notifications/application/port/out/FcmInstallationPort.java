package com.mycomplex.notifications.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.mycomplex.notifications.domain.FcmInstallation;

public interface FcmInstallationPort {
    FcmInstallation register(UUID userId, String installationIdentifier);
    boolean revoke(UUID userId, UUID installationId);
    Optional<Destination> findMostRecentActive(UUID userId);
    void revokeByIdentifier(String installationIdentifier);

    record Destination(UUID installationId, String identifier) { }
}
