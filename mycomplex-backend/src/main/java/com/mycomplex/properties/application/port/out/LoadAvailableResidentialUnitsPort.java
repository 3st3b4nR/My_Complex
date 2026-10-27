package com.mycomplex.properties.application.port.out;

import java.util.List;
import java.util.UUID;

import com.mycomplex.properties.domain.model.AvailableResidentialUnit;

public interface LoadAvailableResidentialUnitsPort {

    List<AvailableResidentialUnit> loadByPropertyId(UUID propertyId);
}
