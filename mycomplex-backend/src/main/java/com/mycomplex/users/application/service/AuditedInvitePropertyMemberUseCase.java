package com.mycomplex.users.application.service;

import java.util.Map;
import java.util.UUID;

import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.api.SecurityAuditCommand;
import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.observability.api.SecurityAuditResult;
import com.mycomplex.observability.api.SecurityEventReporter;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.exception.UserInvitationException;
import com.mycomplex.users.application.port.in.InvitePropertyMemberUseCase;
import com.mycomplex.users.application.port.out.LoadLocalUserIdPort;
import com.mycomplex.users.domain.model.PropertyMemberInvitation;

public final class AuditedInvitePropertyMemberUseCase
        implements InvitePropertyMemberUseCase {

    private final InvitePropertyMemberUseCase delegate;
    private final LoadLocalUserIdPort localUserIdPort;
    private final RecordSecurityAuditUseCase auditUseCase;
    private final SecurityEventReporter securityReporter;

    public AuditedInvitePropertyMemberUseCase(
            InvitePropertyMemberUseCase delegate,
            LoadLocalUserIdPort localUserIdPort,
            RecordSecurityAuditUseCase auditUseCase,
            SecurityEventReporter securityReporter
    ) {
        this.delegate = delegate;
        this.localUserIdPort = localUserIdPort;
        this.auditUseCase = auditUseCase;
        this.securityReporter = securityReporter;
    }

    @Override
    public PropertyMemberInvitation invite(
            UUID propertyId,
            UUID membershipId,
            String administratorSubject
    ) {
        return execute(propertyId, membershipId, administratorSubject, false);
    }

    @Override
    public PropertyMemberInvitation resend(
            UUID propertyId,
            UUID membershipId,
            String administratorSubject
    ) {
        return execute(propertyId, membershipId, administratorSubject, true);
    }

    private PropertyMemberInvitation execute(
            UUID propertyId,
            UUID membershipId,
            String administratorSubject,
            boolean resend
    ) {
        String operation = resend ? "resend" : "invite";
        try {
            PropertyMemberInvitation result = resend
                    ? delegate.resend(
                            propertyId, membershipId, administratorSubject
                    )
                    : delegate.invite(
                            propertyId, membershipId, administratorSubject
                    );
            auditUseCase.record(new SecurityAuditCommand(
                    resend
                            ? SecurityAuditEventType.INVITATION_RESENT
                            : SecurityAuditEventType.INVITATION_REQUESTED,
                    SecurityAuditResult.SUCCESS,
                    localUserIdPort.findByAuth0Subject(administratorSubject)
                            .orElse(null),
                    propertyId, result.userId(), "users",
                    Map.of("invitationMode", operation)
            ));
            return result;
        } catch (PropertyMembersAccessDeniedException exception) {
            securityReporter.administrativeAccessDenied(
                    "member_invitation"
            );
            throw exception;
        } catch (UserInvitationException exception) {
            securityReporter.invitationFailed(operation, exception.code());
            throw exception;
        }
    }
}
