package com.mycomplex.users.infrastructure.adapter.out.persistence.dbo;

import java.util.List;
import java.util.UUID;

public record PropertyMemberDbo(
        UUID membershipId,
        UUID userId,
        String firstNames,
        String lastNames,
        String email,
        String role,
        String userStatus,
        String membershipStatus,
        long version,
        List<PropertyMemberUnitDbo> units
) {
}
