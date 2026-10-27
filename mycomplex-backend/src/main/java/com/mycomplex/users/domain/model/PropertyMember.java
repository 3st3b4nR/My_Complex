package com.mycomplex.users.domain.model;

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
        MembershipStatus membershipStatus,
        long version,
        List<PropertyMemberUnit> units
) {
}
