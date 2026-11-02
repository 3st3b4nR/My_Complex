package com.mycomplex.notifications.application.port.in;

public record RegisterPushInstallationCommand(
        String auth0Subject,
        String installationId
) {
}
