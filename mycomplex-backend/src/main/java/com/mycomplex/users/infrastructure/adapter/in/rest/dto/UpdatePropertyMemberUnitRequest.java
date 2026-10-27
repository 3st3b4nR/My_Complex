package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

import java.util.UUID;

import com.mycomplex.users.domain.model.ResidentialRelationshipType;

import jakarta.validation.constraints.NotNull;

public record UpdatePropertyMemberUnitRequest(
        @NotNull UUID unitId,
        @NotNull ResidentialRelationshipType relationshipType,
        boolean primary
) {
}
