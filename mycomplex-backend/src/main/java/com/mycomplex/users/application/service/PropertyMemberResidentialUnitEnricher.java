package com.mycomplex.users.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mycomplex.properties.application.port.in.ResolveResidentialUnitsUseCase;
import com.mycomplex.properties.domain.model.AvailableResidentialUnit;
import com.mycomplex.properties.domain.model.PropertyStructureReference;
import com.mycomplex.properties.domain.model.ResidentialUnitReference;
import com.mycomplex.properties.domain.model.ResidentialUnitSummary;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberUnit;

/** Composes users-owned assignments with public residential data. */
public final class PropertyMemberResidentialUnitEnricher {

    private final ResolveResidentialUnitsUseCase resolveUnitsUseCase;

    public PropertyMemberResidentialUnitEnricher(
            ResolveResidentialUnitsUseCase resolveUnitsUseCase
    ) {
        this.resolveUnitsUseCase = resolveUnitsUseCase;
    }

    public List<PropertyMember> enrichAll(
            UUID propertyId,
            List<PropertyMember> members
    ) {
        Set<UUID> unitIds = members.stream()
                .flatMap(member -> member.units().stream())
                .map(unit -> unit.unit().id())
                .collect(Collectors.toSet());
        Map<UUID, ResidentialUnitReference> unitsById =
                resolveUnitsUseCase.resolveAvailable(propertyId, unitIds)
                        .stream()
                        .collect(Collectors.toMap(
                                ResidentialUnitSummary::unitId,
                                ResidentialUnitSummary::toReference
                        ));
        return members.stream()
                .map(member -> enrich(member, unitsById))
                .toList();
    }

    public PropertyMember enrich(
            PropertyMember member,
            List<AvailableResidentialUnit> availableUnits
    ) {
        Map<UUID, ResidentialUnitReference> unitsById = availableUnits
                .stream()
                .collect(Collectors.toMap(
                        AvailableResidentialUnit::unitId,
                        this::toReference
                ));
        return enrich(member, unitsById);
    }

    private PropertyMember enrich(
            PropertyMember member,
            Map<UUID, ResidentialUnitReference> unitsById
    ) {
        List<PropertyMemberUnit> units = member.units().stream()
                .filter(assignment -> unitsById.containsKey(
                        assignment.unit().id()
                ))
                .map(assignment -> new PropertyMemberUnit(
                        assignment.assignmentId(),
                        unitsById.get(assignment.unit().id()),
                        assignment.relationshipType(),
                        assignment.primary(),
                        assignment.status()
                ))
                .toList();
        return new PropertyMember(
                member.membershipId(),
                member.userId(),
                member.firstNames(),
                member.lastNames(),
                member.email(),
                member.role(),
                member.userStatus(),
                member.registrationStatus(),
                member.membershipStatus(),
                member.version(),
                member.invitationSentAt(),
                units
        );
    }

    private ResidentialUnitReference toReference(
            AvailableResidentialUnit unit
    ) {
        PropertyStructureReference structure = unit.structureName() == null
                ? null
                : new PropertyStructureReference(
                        unit.structureName(),
                        unit.structureType()
                );
        return new ResidentialUnitReference(
                unit.unitId(),
                structure,
                unit.unitNumber(),
                unit.unitType()
        );
    }
}
