package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

import java.util.UUID;

public record AvailableResidentialUnitResponse(
        UUID unitId,
        UUID structureId,
        String structureName,
        String structureType,
        String unitNumber,
        String unitType
) {
}
