package com.mycomplex.users.domain.model;

import java.util.List;
import java.util.UUID;

public record PropertyMemberUpdate(
        UUID propertyId,
        UUID membershipId,
        PropertyRole role,
        MembershipStatus membershipStatus,
        long version,
        List<ResidentialAssignmentUpdate> units
) {
}
