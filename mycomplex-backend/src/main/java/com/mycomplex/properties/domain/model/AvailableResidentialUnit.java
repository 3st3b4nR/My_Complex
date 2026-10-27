package com.mycomplex.properties.domain.model;

import java.util.UUID;

public record AvailableResidentialUnit(
        UUID unitId,
        UUID structureId,
        String structureName,
        PropertyStructureType structureType,
        String unitNumber,
        ResidentialUnitType unitType
) {
}
