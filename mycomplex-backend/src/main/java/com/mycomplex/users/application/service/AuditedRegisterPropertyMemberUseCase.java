package com.mycomplex.users.application.service;

import java.util.Map;

import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.api.SecurityAuditCommand;
import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.observability.api.SecurityAuditResult;
import com.mycomplex.observability.api.SecurityEventReporter;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.in.RegisterPropertyMemberUseCase;
import com.mycomplex.users.application.port.out.LoadLocalUserIdPort;
import com.mycomplex.users.domain.model.PropertyMemberDetails;
import com.mycomplex.users.domain.model.PropertyMemberRegistration;

public final class AuditedRegisterPropertyMemberUseCase
        implements RegisterPropertyMemberUseCase {

    private final RegisterPropertyMemberUseCase delegate;
    private final LoadLocalUserIdPort localUserIdPort;
    private final RecordSecurityAuditUseCase auditUseCase;
    private final SecurityEventReporter securityReporter;

    public AuditedRegisterPropertyMemberUseCase(
            RegisterPropertyMemberUseCase delegate,
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
    public PropertyMemberDetails register(
            PropertyMemberRegistration registration,
            String auth0Subject
    ) {
        try {
            PropertyMemberDetails details = delegate.register(
                    registration, auth0Subject
            );
            auditUseCase.record(new SecurityAuditCommand(
                    SecurityAuditEventType.MEMBER_REGISTERED,
                    SecurityAuditResult.SUCCESS,
                    localUserIdPort.findByAuth0Subject(auth0Subject)
                            .orElse(null),
                    registration.propertyId(), details.member().userId(),
                    "users", Map.of(
                            "registrationMode", registration.mode().name()
                    )
            ));
            return details;
        } catch (PropertyMembersAccessDeniedException exception) {
            securityReporter.administrativeAccessDenied(
                    "member_registration"
            );
            throw exception;
        }
    }
}
