package com.mycomplex.users.application.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.api.SecurityAuditCommand;
import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.observability.api.SecurityAuditResult;
import com.mycomplex.observability.api.SecurityEventReporter;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.in.UpdatePropertyMemberUseCase;
import com.mycomplex.users.application.port.out.LoadLocalUserIdPort;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberDetails;
import com.mycomplex.users.domain.model.PropertyMemberUpdate;
import com.mycomplex.users.domain.model.PropertyMemberUnit;
import com.mycomplex.users.domain.model.ResidentialAssignmentUpdate;

public final class AuditedUpdatePropertyMemberUseCase
        implements UpdatePropertyMemberUseCase {

    private final UpdatePropertyMemberUseCase delegate;
    private final LoadPropertyMembersPort membersPort;
    private final LoadLocalUserIdPort localUserIdPort;
    private final RecordSecurityAuditUseCase auditUseCase;
    private final SecurityEventReporter securityReporter;

    public AuditedUpdatePropertyMemberUseCase(
            UpdatePropertyMemberUseCase delegate,
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
    public PropertyMemberDetails update(
            PropertyMemberUpdate update,
            String auth0Subject
    ) {
        if (!membersPort.hasActiveAdministratorAccess(
                update.propertyId(), auth0Subject
        )) {
            securityReporter.administrativeAccessDenied("membership_update");
            throw new PropertyMembersAccessDeniedException();
        }
        PropertyMember before = membersPort.loadByMembershipId(
                update.propertyId(), update.membershipId()
        ).orElse(null);
        try {
            PropertyMemberDetails result = delegate.update(
                    update, auth0Subject
            );
            java.util.UUID actorId = localUserIdPort
                    .findByAuth0Subject(auth0Subject).orElse(null);
            if (before != null && before.role() != result.member().role()) {
                record(SecurityAuditEventType.MEMBERSHIP_ROLE_CHANGED,
                        update, result, actorId, "role");
            }
            if (before != null && before.membershipStatus()
                    != result.member().membershipStatus()) {
                record(SecurityAuditEventType.MEMBERSHIP_STATUS_CHANGED,
                        update, result, actorId, "membershipStatus");
            }
            if (before != null && !unitKeys(before.units()).equals(
                    updateKeys(update.units()))) {
                record(SecurityAuditEventType.RESIDENTIAL_ASSIGNMENTS_CHANGED,
                        update, result, actorId, "residentialAssignments");
            }
            return result;
        } catch (PropertyMembersAccessDeniedException exception) {
            securityReporter.administrativeAccessDenied("membership_update");
            throw exception;
        }
    }

    private void record(
            SecurityAuditEventType type,
            PropertyMemberUpdate update,
            PropertyMemberDetails result,
            java.util.UUID actorId,
            String changedField
    ) {
        auditUseCase.record(new SecurityAuditCommand(
                type, SecurityAuditResult.SUCCESS, actorId,
                update.propertyId(), result.member().userId(), "users",
                Map.of("changedField", changedField)
        ));
    }

    private Set<String> unitKeys(java.util.List<PropertyMemberUnit> units) {
        return units.stream().map(unit ->
                unit.unit().id() + ":" + unit.relationshipType()
                        + ":" + unit.primary()
        ).collect(Collectors.toSet());
    }

    private Set<String> updateKeys(
            java.util.List<ResidentialAssignmentUpdate> units
    ) {
        return units.stream().map(unit ->
                unit.unitId() + ":" + unit.relationshipType()
                        + ":" + unit.primary()
        ).collect(Collectors.toSet());
    }
}
