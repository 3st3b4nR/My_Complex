package com.mycomplex.notifications.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.mycomplex.notifications.application.port.in.RegisterPushInstallationCommand;
import com.mycomplex.notifications.application.port.in.RevokePushInstallationCommand;
import com.mycomplex.notifications.application.port.out.StorePushInstallationPort;

class ManagePushInstallationServiceTest {
    @Test
    void registersAndRevokesTrimmedInstallations() {
        StorePushInstallationPort store = mock(StorePushInstallationPort.class);
        when(store.register("auth0|user", "installation-1")).thenReturn(true);
        ManagePushInstallationService service =
                new ManagePushInstallationService(store);

        service.register(new RegisterPushInstallationCommand(
                " auth0|user ", " installation-1 "));
        service.revoke(new RevokePushInstallationCommand(
                " auth0|user ", " installation-1 "));

        verify(store).register("auth0|user", "installation-1");
        verify(store).revoke("auth0|user", "installation-1");
    }

    @Test
    void rejectsInactiveUsersAndInvalidIdentifiers() {
        StorePushInstallationPort store = mock(StorePushInstallationPort.class);
        when(store.register("auth0|inactive", "installation-1"))
                .thenReturn(false);
        ManagePushInstallationService service =
                new ManagePushInstallationService(store);

        assertThatThrownBy(() -> service.register(
                new RegisterPushInstallationCommand(
                        "auth0|inactive", "installation-1")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.register(
                new RegisterPushInstallationCommand(" ", "installation-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.revoke(
                new RevokePushInstallationCommand("auth0|user", "x".repeat(513))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
