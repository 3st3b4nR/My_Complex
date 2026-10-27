package com.mycomplex.users.domain.model;

import java.util.List;
import java.util.UUID;

public record PropertyMemberRegistration(
        UUID propertyId,
        PropertyMemberRegistrationMode mode,
        UUID userId,
        UUID membershipId,
        String firstNames,
        String lastNames,
        String email,
        String documentType,
        String documentNumber,
        PropertyRole role,
        MembershipStatus membershipStatus,
        List<ResidentialAssignmentUpdate> units
) {
}
