package com.mycomplex.users.domain.model;

import java.util.UUID;

public record ResidentialAssignmentUpdate(
        UUID unitId,
        ResidentialRelationshipType relationshipType,
        boolean primary
) {
}
