package com.mycomplex.users.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mycomplex.properties.application.port.in.FindAvailableResidentialUnitsUseCase;
import com.mycomplex.properties.domain.model.AvailableResidentialUnit;
import com.mycomplex.users.application.exception.InvalidPropertyMemberRegistrationException;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.in.RegisterPropertyMemberUseCase;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.application.port.out.ManagePropertyMembersPort;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberDetails;
import com.mycomplex.users.domain.model.PropertyMemberRegistration;
import com.mycomplex.users.domain.model.PropertyMemberRegistrationMode;
import com.mycomplex.users.domain.model.ResidentialAssignmentUpdate;

public final class RegisterPropertyMemberService
        implements RegisterPropertyMemberUseCase {

    private final LoadPropertyMembersPort loadMembersPort;
    private final ManagePropertyMembersPort manageMembersPort;
    private final FindAvailableResidentialUnitsUseCase findUnitsUseCase;

    public RegisterPropertyMemberService(
            LoadPropertyMembersPort loadMembersPort,
            ManagePropertyMembersPort manageMembersPort,
            FindAvailableResidentialUnitsUseCase findUnitsUseCase
    ) {
        this.loadMembersPort = loadMembersPort;
        this.manageMembersPort = manageMembersPort;
        this.findUnitsUseCase = findUnitsUseCase;
    }

    @Override
    public PropertyMemberDetails register(
            PropertyMemberRegistration registration,
            String auth0Subject
    ) {
        PropertyMemberRegistration normalized = validateAndNormalize(
                registration,
                auth0Subject
        );
        if (!loadMembersPort.hasActiveAdministratorAccess(
                normalized.propertyId(),
                auth0Subject
        )) {
            throw new PropertyMembersAccessDeniedException();
        }
        List<AvailableResidentialUnit> units =
                findUnitsUseCase.findByPropertyId(normalized.propertyId());
        Set<UUID> availableIds = units.stream()
                .map(AvailableResidentialUnit::unitId)
                .collect(Collectors.toSet());
        if (normalized.units().stream().anyMatch(
                assignment -> !availableIds.contains(assignment.unitId())
        )) {
            throw new InvalidPropertyMemberRegistrationException(
                    "Todas las unidades deben estar activas y pertenecer a la copropiedad."
            );
        }
        PropertyMember member = manageMembersPort.register(
                normalized,
                auth0Subject
        );
        return new PropertyMemberDetails(member, units);
    }

    private PropertyMemberRegistration validateAndNormalize(
            PropertyMemberRegistration value,
            String auth0Subject
    ) {
        if (value == null || value.propertyId() == null
                || value.mode() == null || value.role() == null
                || value.membershipStatus() == null
                || value.units() == null || auth0Subject == null
                || auth0Subject.isBlank()) {
            throw new InvalidPropertyMemberRegistrationException(
                    "El registro está incompleto."
            );
        }
        String email = FindPropertyMemberCandidateService.normalizeEmail(
                value.email()
        );
        String firstNames = trim(value.firstNames());
        String lastNames = trim(value.lastNames());
        String documentType = nullableTrim(value.documentType());
        String documentNumber = nullableTrim(value.documentNumber());
        if (value.mode() == PropertyMemberRegistrationMode.CREATE_PENDING_USER
                && (firstNames == null || lastNames == null)) {
            throw new InvalidPropertyMemberRegistrationException(
                    "Nombres y apellidos son obligatorios."
            );
        }
        if ((firstNames != null && firstNames.length() > 120)
                || (lastNames != null && lastNames.length() > 120)
                || (documentType != null && documentType.length() > 20)
                || (documentNumber != null && documentNumber.length() > 50)) {
            throw new InvalidPropertyMemberRegistrationException(
                    "Uno de los campos supera la longitud permitida."
            );
        }
        if ((documentType == null) != (documentNumber == null)) {
            throw new InvalidPropertyMemberRegistrationException(
                    "El tipo y el número de documento deben informarse juntos."
            );
        }
        if (value.mode() == PropertyMemberRegistrationMode.LINK_EXISTING_USER
                && value.userId() == null) {
            throw new InvalidPropertyMemberRegistrationException(
                    "El usuario existente es obligatorio."
            );
        }
        if (value.mode() == PropertyMemberRegistrationMode.REACTIVATE_MEMBERSHIP
                && value.membershipId() == null) {
            throw new InvalidPropertyMemberRegistrationException(
                    "La membresía a reactivar es obligatoria."
            );
        }
        validateAssignments(value.units());
        return new PropertyMemberRegistration(
                value.propertyId(), value.mode(), value.userId(),
                value.membershipId(), firstNames, lastNames, email,
                documentType, documentNumber, value.role(),
                value.membershipStatus(), List.copyOf(value.units())
        );
    }

    private void validateAssignments(List<ResidentialAssignmentUpdate> units) {
        Set<UUID> ids = new HashSet<>();
        int primary = 0;
        for (ResidentialAssignmentUpdate unit : units) {
            if (unit == null || unit.unitId() == null
                    || unit.relationshipType() == null) {
                throw new InvalidPropertyMemberRegistrationException(
                        "Cada asignación residencial debe estar completa."
                );
            }
            if (!ids.add(unit.unitId())) {
                throw new InvalidPropertyMemberRegistrationException(
                        "No se puede asignar dos veces la misma unidad."
                );
            }
            if (unit.primary()) {
                primary++;
            }
        }
        if (primary > 1) {
            throw new InvalidPropertyMemberRegistrationException(
                    "Solo una unidad puede ser principal."
            );
        }
    }

    private String trim(String value) {
        return value == null || value.trim().isEmpty()
                ? null : value.trim();
    }

    private String nullableTrim(String value) {
        return trim(value);
    }
}
