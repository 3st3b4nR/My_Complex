package com.mycomplex.properties.application.service;

import java.util.List;
import java.util.UUID;

import com.mycomplex.properties.application.port.in.FindAvailableResidentialUnitsUseCase;
import com.mycomplex.properties.application.port.out.LoadAvailableResidentialUnitsPort;
import com.mycomplex.properties.domain.model.AvailableResidentialUnit;

public final class FindAvailableResidentialUnitsService
        implements FindAvailableResidentialUnitsUseCase {

    private final LoadAvailableResidentialUnitsPort loadUnitsPort;

    public FindAvailableResidentialUnitsService(
            LoadAvailableResidentialUnitsPort loadUnitsPort
    ) {
        this.loadUnitsPort = loadUnitsPort;
    }

    @Override
    public List<AvailableResidentialUnit> findByPropertyId(
            UUID propertyId
    ) {
        if (propertyId == null) {
            throw new IllegalArgumentException(
                    "propertyId must not be null"
            );
        }

        return loadUnitsPort.loadByPropertyId(propertyId);
    }
}
