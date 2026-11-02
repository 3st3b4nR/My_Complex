package com.mycomplex.notifications.application.port.in;

public record RevokePushInstallationCommand(
        String auth0Subject,
        String installationId
) {
}
