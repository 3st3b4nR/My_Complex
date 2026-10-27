package com.mycomplex.users.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mycomplex.properties.application.port.in.FindAvailableResidentialUnitsUseCase;
import com.mycomplex.properties.domain.model.AvailableResidentialUnit;
import com.mycomplex.users.application.exception.InvalidPropertyMemberUpdateException;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.in.UpdatePropertyMemberUseCase;
import com.mycomplex.users.application.port.out.UpdatePropertyMemberPort;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberDetails;
import com.mycomplex.users.domain.model.PropertyMemberUpdate;
import com.mycomplex.users.domain.model.ResidentialAssignmentUpdate;

public final class UpdatePropertyMemberService
        implements UpdatePropertyMemberUseCase {

    private final UpdatePropertyMemberPort updateMemberPort;
    private final LoadPropertyMembersPort loadMembersPort;
    private final FindAvailableResidentialUnitsUseCase findUnitsUseCase;

    public UpdatePropertyMemberService(
            UpdatePropertyMemberPort updateMemberPort,
            LoadPropertyMembersPort loadMembersPort,
            FindAvailableResidentialUnitsUseCase findUnitsUseCase
    ) {
        this.updateMemberPort = updateMemberPort;
        this.loadMembersPort = loadMembersPort;
        this.findUnitsUseCase = findUnitsUseCase;
    }

    @Override
    public PropertyMemberDetails update(
            PropertyMemberUpdate update,
            String auth0Subject
    ) {
        validate(update, auth0Subject);

        if (!loadMembersPort.hasActiveAdministratorAccess(
                update.propertyId(),
                auth0Subject
        )) {
            throw new PropertyMembersAccessDeniedException();
        }

        List<AvailableResidentialUnit> availableUnits =
                findUnitsUseCase.findByPropertyId(update.propertyId());
        Set<UUID> availableUnitIds = availableUnits.stream()
                .map(AvailableResidentialUnit::unitId)
                .collect(java.util.stream.Collectors.toSet());

        if (update.units().stream().anyMatch(
                assignment -> !availableUnitIds.contains(
                        assignment.unitId()
                )
        )) {
            throw new InvalidPropertyMemberUpdateException(
                    "Every unit must be active and belong to the property"
            );
        }

        PropertyMember member = updateMemberPort.update(
                update,
                auth0Subject
        );

        return new PropertyMemberDetails(member, availableUnits);
    }

    private void validate(
            PropertyMemberUpdate update,
            String auth0Subject
    ) {
        if (update == null
                || update.propertyId() == null
                || update.membershipId() == null
                || update.role() == null
                || update.membershipStatus() == null
                || update.units() == null) {
            throw new InvalidPropertyMemberUpdateException(
                    "The property member update is incomplete"
            );
        }
        if (auth0Subject == null || auth0Subject.isBlank()) {
            throw new IllegalArgumentException(
                    "auth0Subject must not be blank"
            );
        }
        if (update.version() < 0) {
            throw new InvalidPropertyMemberUpdateException(
                    "The version must not be negative"
            );
        }

        Set<UUID> unitIds = new HashSet<>();
        int primaryCount = 0;
        for (ResidentialAssignmentUpdate assignment : update.units()) {
            if (assignment == null
                    || assignment.unitId() == null
                    || assignment.relationshipType() == null) {
                throw new InvalidPropertyMemberUpdateException(
                        "Every residential assignment is required"
                );
            }
            if (!unitIds.add(assignment.unitId())) {
                throw new InvalidPropertyMemberUpdateException(
                        "A residential unit cannot be assigned twice"
                );
            }
            if (assignment.primary()) {
                primaryCount++;
            }
        }
        if (primaryCount > 1) {
            throw new InvalidPropertyMemberUpdateException(
                    "Only one residential unit can be primary"
            );
        }
    }
}
