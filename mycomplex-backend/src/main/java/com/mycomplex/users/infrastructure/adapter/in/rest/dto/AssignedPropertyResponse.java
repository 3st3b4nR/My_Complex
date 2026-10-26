package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

import java.util.UUID;

public record AssignedPropertyResponse(
        UUID propertyId,
        String name,
        String city,
        String role
) {
}