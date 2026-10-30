package com.mycomplex.properties.domain.model;

import java.util.UUID;

/** Public residential read model exposed by the properties module. */
public record ResidentialUnitSummary(
        UUID unitId,
        UUID propertyId,
        UUID structureId,
        String structureName,
        PropertyStructureType structureType,
        String unitNumber,
        ResidentialUnitType unitType,
        ResidentialUnitStatus unitStatus
) {
    public ResidentialUnitReference toReference() {
        PropertyStructureReference structure = structureName == null
                ? null
                : new PropertyStructureReference(
                        structureName,
                        structureType
                );
        return new ResidentialUnitReference(
                unitId,
                structure,
                unitNumber,
                unitType
        );
    }
}
