package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PropertyMemberResponse(
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
        OffsetDateTime invitationSentAt,
        List<PropertyMemberUnitResponse> units
) {
}
