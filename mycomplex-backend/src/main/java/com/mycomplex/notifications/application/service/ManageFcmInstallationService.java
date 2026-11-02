package com.mycomplex.notifications.application.service;

import java.util.UUID;

import com.mycomplex.notifications.application.exception.FcmInstallationException;
import com.mycomplex.notifications.application.port.in.ManageFcmInstallationUseCase;
import com.mycomplex.notifications.application.port.out.FcmInstallationPort;
import com.mycomplex.notifications.domain.FcmInstallation;
import com.mycomplex.users.api.TaskAccessDirectory;

public final class ManageFcmInstallationService
        implements ManageFcmInstallationUseCase {

    private final TaskAccessDirectory users;
    private final FcmInstallationPort installations;

    public ManageFcmInstallationService(
            TaskAccessDirectory users,
            FcmInstallationPort installations
    ) {
        this.users = users;
        this.installations = installations;
    }

    @Override
    public FcmInstallation register(
            String auth0Subject,
            String installationId
    ) {
        UUID userId = activeUser(auth0Subject);
        String normalized = installationId == null
                ? "" : installationId.strip();
        if (normalized.length() < 10 || normalized.length() > 512) {
            throw new FcmInstallationException(
                    "INVALID_FCM_INSTALLATION",
                    "El identificador de la instalación no es válido."
            );
        }
        return installations.register(userId, normalized);
    }

    @Override
    public void revoke(String auth0Subject, UUID installationId) {
        UUID userId = activeUser(auth0Subject);
        if (installationId == null
                || !installations.revoke(userId, installationId)) {
            throw new FcmInstallationException(
                    "FCM_INSTALLATION_NOT_FOUND",
                    "La instalación no está disponible."
            );
        }
    }

    private UUID activeUser(String auth0Subject) {
        return users.findActiveUserId(auth0Subject).orElseThrow(() ->
                new FcmInstallationException(
                        "FCM_USER_NOT_AVAILABLE",
                        "El usuario autenticado no está disponible."
                )
        );
    }
}
