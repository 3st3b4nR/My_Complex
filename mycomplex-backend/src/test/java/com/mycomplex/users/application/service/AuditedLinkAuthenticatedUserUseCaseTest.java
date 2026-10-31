package com.mycomplex.users.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.api.SecurityAuditCommand;
import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.users.application.port.in.LinkAuthenticatedUserUseCase;
import com.mycomplex.users.application.port.out.LoadLocalUserIdPort;

class AuditedLinkAuthenticatedUserUseCaseTest {

    @Test
    void recordsAuditOnlyWhenRegistrationStateActuallyChanges() {
        LinkAuthenticatedUserUseCase delegate =
                Mockito.mock(LinkAuthenticatedUserUseCase.class);
        LoadLocalUserIdPort users = Mockito.mock(LoadLocalUserIdPort.class);
        RecordSecurityAuditUseCase audit =
                Mockito.mock(RecordSecurityAuditUseCase.class);
        UUID userId = UUID.randomUUID();
        when(delegate.link("auth0|member")).thenReturn(true, false);
        when(users.findByAuth0Subject("auth0|member"))
                .thenReturn(Optional.of(userId));
        AuditedLinkAuthenticatedUserUseCase useCase =
                new AuditedLinkAuthenticatedUserUseCase(
                        delegate, users, audit
                );

        assertTrue(useCase.link("auth0|member"));
        ArgumentCaptor<SecurityAuditCommand> command =
                ArgumentCaptor.forClass(SecurityAuditCommand.class);
        verify(audit).record(command.capture());
        assertTrue(command.getValue().type()
                == SecurityAuditEventType.AUTHENTICATED_IDENTITY_LINKED);

        assertFalse(useCase.link("auth0|member"));
        verify(audit).record(command.getValue());
    }

    @Test
    void doesNotResolveOrAuditAnAlreadyLinkedIdentity() {
        LinkAuthenticatedUserUseCase delegate =
                Mockito.mock(LinkAuthenticatedUserUseCase.class);
        LoadLocalUserIdPort users = Mockito.mock(LoadLocalUserIdPort.class);
        RecordSecurityAuditUseCase audit =
                Mockito.mock(RecordSecurityAuditUseCase.class);
        when(delegate.link("auth0|member")).thenReturn(false);
        AuditedLinkAuthenticatedUserUseCase useCase =
                new AuditedLinkAuthenticatedUserUseCase(
                        delegate, users, audit
                );

        assertFalse(useCase.link("auth0|member"));

        verify(users, never()).findByAuth0Subject("auth0|member");
        verify(audit, never()).record(Mockito.any());
    }
}
