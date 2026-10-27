package com.mycomplex.users.domain.model;

import java.util.UUID;
import java.util.List;

import com.mycomplex.properties.domain.model.AvailableResidentialUnit;

public record PropertyMemberCandidate(
        PropertyMemberCandidateState state,
        UUID userId,
        UUID membershipId,
        String firstNames,
        String lastNames,
        String email,
        UserRegistrationStatus registrationStatus,
        List<AvailableResidentialUnit> availableUnits
) {
}
