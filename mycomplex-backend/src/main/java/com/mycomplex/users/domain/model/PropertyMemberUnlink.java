package com.mycomplex.users.domain.model;

import java.util.UUID;

public record PropertyMemberUnlink(
        UUID propertyId,
        UUID membershipId,
        long version
) {
}
