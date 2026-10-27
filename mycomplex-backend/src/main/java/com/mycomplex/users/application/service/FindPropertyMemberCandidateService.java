package com.mycomplex.users.application.service;

import java.util.Locale;
import java.util.UUID;

import com.mycomplex.users.application.exception.InvalidPropertyMemberRegistrationException;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.in.FindPropertyMemberCandidateUseCase;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.application.port.out.ManagePropertyMembersPort;
import com.mycomplex.users.domain.model.PropertyMemberCandidate;
import com.mycomplex.properties.application.port.in.FindAvailableResidentialUnitsUseCase;

public final class FindPropertyMemberCandidateService
        implements FindPropertyMemberCandidateUseCase {

    private final LoadPropertyMembersPort loadMembersPort;
    private final ManagePropertyMembersPort manageMembersPort;
    private final FindAvailableResidentialUnitsUseCase findUnitsUseCase;

    public FindPropertyMemberCandidateService(
            LoadPropertyMembersPort loadMembersPort,
            ManagePropertyMembersPort manageMembersPort,
            FindAvailableResidentialUnitsUseCase findUnitsUseCase
    ) {
        this.loadMembersPort = loadMembersPort;
        this.manageMembersPort = manageMembersPort;
        this.findUnitsUseCase = findUnitsUseCase;
    }

    @Override
    public PropertyMemberCandidate find(
            UUID propertyId,
            String email,
            String auth0Subject
    ) {
        if (propertyId == null || auth0Subject == null
                || auth0Subject.isBlank()) {
            throw new IllegalArgumentException("Invalid candidate lookup");
        }
        String normalizedEmail = normalizeEmail(email);
        if (!loadMembersPort.hasActiveAdministratorAccess(
                propertyId,
                auth0Subject
        )) {
            throw new PropertyMembersAccessDeniedException();
        }
        PropertyMemberCandidate candidate = manageMembersPort.findCandidate(
                propertyId, normalizedEmail
        );
        return new PropertyMemberCandidate(
                candidate.state(), candidate.userId(),
                candidate.membershipId(), candidate.firstNames(),
                candidate.lastNames(), candidate.email(),
                candidate.registrationStatus(),
                findUnitsUseCase.findByPropertyId(propertyId)
        );
    }

    static String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()
                || email.trim().length() > 254
                || !email.trim().contains("@")) {
            throw new InvalidPropertyMemberRegistrationException(
                    "El correo no es válido."
            );
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
