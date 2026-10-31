package com.mycomplex.users.application.service;

import java.util.Map;
import java.util.UUID;

import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.api.SecurityAuditCommand;
import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.observability.api.SecurityAuditResult;
import com.mycomplex.users.application.port.in.LinkAuthenticatedUserUseCase;
import com.mycomplex.users.application.port.out.LoadLocalUserIdPort;

public final class AuditedLinkAuthenticatedUserUseCase
        implements LinkAuthenticatedUserUseCase {

    private final LinkAuthenticatedUserUseCase delegate;
    private final LoadLocalUserIdPort localUserIdPort;
    private final RecordSecurityAuditUseCase auditUseCase;

    public AuditedLinkAuthenticatedUserUseCase(
            LinkAuthenticatedUserUseCase delegate,
            LoadLocalUserIdPort localUserIdPort,
            RecordSecurityAuditUseCase auditUseCase
    ) {
        this.delegate = delegate;
        this.localUserIdPort = localUserIdPort;
        this.auditUseCase = auditUseCase;
    }

    @Override
    public boolean link(String auth0Subject) {
        boolean linked = delegate.link(auth0Subject);
        if (!linked) {
            return false;
        }
        UUID userId = localUserIdPort.findByAuth0Subject(auth0Subject)
                .orElse(null);
        if (userId != null) {
            auditUseCase.record(new SecurityAuditCommand(
                    SecurityAuditEventType.AUTHENTICATED_IDENTITY_LINKED,
                    SecurityAuditResult.SUCCESS,
                    userId, null, userId, "users", Map.of()
            ));
        }
        return true;
    }
}
