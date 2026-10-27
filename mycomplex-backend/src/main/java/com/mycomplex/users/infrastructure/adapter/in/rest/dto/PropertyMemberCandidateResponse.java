package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

import java.util.UUID;
import java.util.List;

public record PropertyMemberCandidateResponse(
        String state,
        UUID userId,
        UUID membershipId,
        String firstNames,
        String lastNames,
        String email,
        String registrationStatus,
        List<AvailableResidentialUnitResponse> availableUnits
) {
}
