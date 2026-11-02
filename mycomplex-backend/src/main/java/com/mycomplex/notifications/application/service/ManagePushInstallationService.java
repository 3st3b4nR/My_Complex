package com.mycomplex.notifications.application.service;

import java.util.Objects;

import com.mycomplex.notifications.application.port.in.ManagePushInstallationUseCase;
import com.mycomplex.notifications.application.port.in.RegisterPushInstallationCommand;
import com.mycomplex.notifications.application.port.in.RevokePushInstallationCommand;
import com.mycomplex.notifications.application.port.out.StorePushInstallationPort;

public final class ManagePushInstallationService
        implements ManagePushInstallationUseCase {

    private final StorePushInstallationPort storePort;

    public ManagePushInstallationService(
            StorePushInstallationPort storePort
    ) {
        this.storePort = Objects.requireNonNull(storePort);
    }

    @Override
    public void register(RegisterPushInstallationCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        if (!storePort.register(
                required(command.auth0Subject(), "auth0Subject"),
                installationId(command.installationId())
        )) {
            throw new IllegalStateException(
                    "The authenticated user is not active in MyComplex"
            );
        }
    }

    @Override
    public void revoke(RevokePushInstallationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        storePort.revoke(
                required(command.auth0Subject(), "auth0Subject"),
                installationId(command.installationId())
        );
    }

    private String installationId(String value) {
        String installationId = required(value, "installationId");
        if (installationId.length() > 512) {
            throw new IllegalArgumentException(
                    "installationId must contain at most 512 characters"
            );
        }
        return installationId;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
