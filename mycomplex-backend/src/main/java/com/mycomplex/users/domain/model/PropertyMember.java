package com.mycomplex.users.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PropertyMember(
        UUID membershipId,
        UUID userId,
        String firstNames,
        String lastNames,
        String email,
        PropertyRole role,
        UserStatus userStatus,
        UserRegistrationStatus registrationStatus,
        MembershipStatus membershipStatus,
        long version,
        OffsetDateTime invitationSentAt,
        List<PropertyMemberUnit> units
) {
    public PropertyMember(
            UUID membershipId,
            UUID userId,
            String firstNames,
            String lastNames,
            String email,
            PropertyRole role,
            UserStatus userStatus,
            UserRegistrationStatus registrationStatus,
            MembershipStatus membershipStatus,
            long version,
            List<PropertyMemberUnit> units
    ) {
        this(membershipId, userId, firstNames, lastNames, email, role,
                userStatus, registrationStatus, membershipStatus, version,
                null, units);
    }
}
