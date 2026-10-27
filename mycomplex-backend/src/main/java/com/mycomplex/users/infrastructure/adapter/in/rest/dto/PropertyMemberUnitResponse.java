package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

import java.util.UUID;

public record PropertyMemberUnitResponse(
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
