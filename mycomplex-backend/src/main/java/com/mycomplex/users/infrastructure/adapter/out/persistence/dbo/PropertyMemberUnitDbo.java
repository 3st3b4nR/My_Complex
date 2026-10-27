package com.mycomplex.users.infrastructure.adapter.out.persistence.dbo;

import java.util.UUID;

public record PropertyMemberUnitDbo(
        UUID assignmentId,
        UUID unitId,
        String structureName,
        String structureType,
        String unitNumber,
        String unitType,
        String relationshipType,
        boolean primary,
        String status
) {
}
