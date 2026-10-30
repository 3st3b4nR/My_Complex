package com.mycomplex.properties.application.port.out;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mycomplex.properties.domain.model.AvailableResidentialUnit;
import com.mycomplex.properties.domain.model.ResidentialUnitSummary;

public interface LoadAvailableResidentialUnitsPort {

    List<AvailableResidentialUnit> loadByPropertyId(UUID propertyId);

    List<ResidentialUnitSummary> loadByPropertyIdAndUnitIds(
            UUID propertyId,
            Set<UUID> unitIds
    );
}
