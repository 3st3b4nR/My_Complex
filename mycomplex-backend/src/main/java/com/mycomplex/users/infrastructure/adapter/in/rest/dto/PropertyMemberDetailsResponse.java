package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

import java.util.List;
import java.util.UUID;

public record PropertyMemberDetailsResponse(
        UUID membershipId,
        UUID userId,
        String firstNames,
        String lastNames,
        String email,
        String role,
        String userStatus,
        String registrationStatus,
        String membershipStatus,
        long version,
        List<PropertyMemberUnitResponse> units,
        List<AvailableResidentialUnitResponse> availableUnits
) {
}
