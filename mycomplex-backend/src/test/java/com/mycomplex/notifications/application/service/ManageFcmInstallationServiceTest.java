package com.mycomplex.notifications.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.notifications.application.port.out.FcmInstallationPort;
import com.mycomplex.notifications.domain.FcmInstallation;
import com.mycomplex.users.api.TaskAccessDirectory;

class ManageFcmInstallationServiceTest {

    @Test
    void associatesInstallationWithAuthenticatedLocalUser() {
        TaskAccessDirectory users = mock(TaskAccessDirectory.class);
        FcmInstallationPort installations = mock(FcmInstallationPort.class);
        UUID userId = UUID.randomUUID();
        UUID installationId = UUID.randomUUID();
        String identifier = "firebase-installation-identifier";
        when(users.findActiveUserId("auth0|worker"))
                .thenReturn(Optional.of(userId));
        when(installations.register(userId, identifier))
                .thenReturn(new FcmInstallation(installationId, userId));

        var service = new ManageFcmInstallationService(users, installations);
        FcmInstallation result = service.register("auth0|worker", identifier);

        assertThat(result.id()).isEqualTo(installationId);
        verify(installations).register(userId, identifier);
    }

    @Test
    void normalizesIdentifierAndRejectsInvalidValues() {
        TaskAccessDirectory users = mock(TaskAccessDirectory.class);
        FcmInstallationPort installations = mock(FcmInstallationPort.class);
        UUID userId = UUID.randomUUID();
        when(users.findActiveUserId("auth0|worker"))
                .thenReturn(Optional.of(userId));
        when(installations.register(userId, "valid-identifier"))
                .thenReturn(new FcmInstallation(UUID.randomUUID(), userId));
        var service = new ManageFcmInstallationService(users, installations);

        service.register("auth0|worker", "  valid-identifier  ");
        verify(installations).register(userId, "valid-identifier");
        assertThatThrownBy(() -> service.register("auth0|worker", null))
                .isInstanceOf(com.mycomplex.notifications.application.exception.FcmInstallationException.class)
                .hasMessageContaining("identificador");
        assertThatThrownBy(() -> service.register("auth0|worker", "short"))
                .isInstanceOf(com.mycomplex.notifications.application.exception.FcmInstallationException.class);
    }

    @Test
    void revokesOwnedInstallationAndRejectsMissingOrInactiveUser() {
        TaskAccessDirectory users = mock(TaskAccessDirectory.class);
        FcmInstallationPort installations = mock(FcmInstallationPort.class);
        UUID userId = UUID.randomUUID();
        UUID installationId = UUID.randomUUID();
        when(users.findActiveUserId("auth0|worker"))
                .thenReturn(Optional.of(userId));
        when(installations.revoke(userId, installationId)).thenReturn(true);
        var service = new ManageFcmInstallationService(users, installations);

        service.revoke("auth0|worker", installationId);
        verify(installations).revoke(userId, installationId);

        when(installations.revoke(userId, installationId)).thenReturn(false);
        assertThatThrownBy(() -> service.revoke(
                "auth0|worker", installationId
        )).isInstanceOf(com.mycomplex.notifications.application.exception.FcmInstallationException.class)
                .hasMessageContaining("disponible");

        when(users.findActiveUserId("auth0|inactive"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.register(
                "auth0|inactive", "valid-identifier"
        )).isInstanceOf(com.mycomplex.notifications.application.exception.FcmInstallationException.class)
                .hasMessageContaining("autenticado");
    }
}
