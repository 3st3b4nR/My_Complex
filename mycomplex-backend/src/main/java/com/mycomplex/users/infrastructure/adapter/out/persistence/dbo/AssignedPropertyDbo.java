package com.mycomplex.users.infrastructure.adapter.out.persistence.dbo;

import java.util.UUID;

public record AssignedPropertyDbo(
        UUID propertyId,
        String name,
        String city,
        String role
) {
}