package com.mycomplex.users.application.service;

import java.util.Map;

import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.api.SecurityAuditCommand;
import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.observability.api.SecurityAuditResult;
import com.mycomplex.observability.api.SecurityEventReporter;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.in.UnlinkPropertyMemberUseCase;
import com.mycomplex.users.application.port.out.LoadLocalUserIdPort;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberUnlink;

public final class AuditedUnlinkPropertyMemberUseCase
        implements UnlinkPropertyMemberUseCase {

    private final UnlinkPropertyMemberUseCase delegate;
    private final LoadPropertyMembersPort membersPort;
    private final LoadLocalUserIdPort localUserIdPort;
    private final RecordSecurityAuditUseCase auditUseCase;
    private final SecurityEventReporter securityReporter;

    public AuditedUnlinkPropertyMemberUseCase(
            UnlinkPropertyMemberUseCase delegate,
            LoadPropertyMembersPort membersPort,
            LoadLocalUserIdPort localUserIdPort,
            RecordSecurityAuditUseCase auditUseCase,
            SecurityEventReporter securityReporter
    ) {
        this.delegate = delegate;
        this.membersPort = membersPort;
        this.localUserIdPort = localUserIdPort;
        this.auditUseCase = auditUseCase;
        this.securityReporter = securityReporter;
    }

    @Override
    public void unlink(PropertyMemberUnlink unlink, String auth0Subject) {
        if (!membersPort.hasActiveAdministratorAccess(
                unlink.propertyId(), auth0Subject
        )) {
            securityReporter.administrativeAccessDenied("member_unlink");
            throw new PropertyMembersAccessDeniedException();
        }
        PropertyMember target = membersPort.loadByMembershipId(
                unlink.propertyId(), unlink.membershipId()
        ).orElse(null);
        try {
            delegate.unlink(unlink, auth0Subject);
            auditUseCase.record(new SecurityAuditCommand(
                    SecurityAuditEventType.MEMBER_UNLINKED,
                    SecurityAuditResult.SUCCESS,
                    localUserIdPort.findByAuth0Subject(auth0Subject)
                            .orElse(null),
                    unlink.propertyId(),
                    target == null ? null : target.userId(),
                    "users", Map.of()
            ));
        } catch (PropertyMembersAccessDeniedException exception) {
            securityReporter.administrativeAccessDenied("member_unlink");
            throw exception;
        }
    }
}
