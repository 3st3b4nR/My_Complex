package com.mycomplex.notifications.application.service;

import static org.assertj.core.api.Assertions.assertThat;
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
}
