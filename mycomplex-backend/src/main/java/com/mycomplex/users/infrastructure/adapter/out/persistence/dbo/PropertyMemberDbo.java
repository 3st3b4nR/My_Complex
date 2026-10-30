package com.mycomplex.users.infrastructure.adapter.out.persistence.dbo;

import java.time.OffsetDateTime;
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
        String registrationStatus,
        String membershipStatus,
        long version,
        OffsetDateTime invitationSentAt,
        List<PropertyMemberUnitDbo> units
) {
    public PropertyMemberDbo(
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
            List<PropertyMemberUnitDbo> units
    ) {
        this(membershipId, userId, firstNames, lastNames, email, role,
                userStatus, registrationStatus, membershipStatus, version,
                null, units);
    }
}
