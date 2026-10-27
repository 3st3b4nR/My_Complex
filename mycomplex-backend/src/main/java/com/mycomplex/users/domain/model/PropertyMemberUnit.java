package com.mycomplex.users.domain.model;

import com.mycomplex.properties.domain.model.ResidentialUnitReference;

public record PropertyMemberUnit(
        ResidentialUnitReference unit,
        ResidentialRelationshipType relationshipType,
        boolean primary,
        ResidentialRelationshipStatus status
) {
}
