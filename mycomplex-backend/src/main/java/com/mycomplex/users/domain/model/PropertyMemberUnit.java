package com.mycomplex.users.domain.model;

import java.util.UUID;

import com.mycomplex.properties.domain.model.ResidentialUnitReference;

public record PropertyMemberUnit(
        UUID assignmentId,
        ResidentialUnitReference unit,
        ResidentialRelationshipType relationshipType,
        boolean primary,
        ResidentialRelationshipStatus status
) {
}
