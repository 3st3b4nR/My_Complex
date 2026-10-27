package com.mycomplex.properties.domain.model;

import java.util.UUID;

public record ResidentialUnitReference(
        UUID id,
        PropertyStructureReference structure,
        String number,
        ResidentialUnitType type
) {
}
