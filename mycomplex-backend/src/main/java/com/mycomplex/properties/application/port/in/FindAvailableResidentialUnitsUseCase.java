package com.mycomplex.properties.application.port.in;

import java.util.List;
import java.util.UUID;

import com.mycomplex.properties.domain.model.AvailableResidentialUnit;

public interface FindAvailableResidentialUnitsUseCase {

    List<AvailableResidentialUnit> findByPropertyId(UUID propertyId);
}
