package com.mycomplex.properties.application.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mycomplex.properties.application.port.in.ResolveResidentialUnitsUseCase;
import com.mycomplex.properties.application.port.out.LoadAvailableResidentialUnitsPort;
import com.mycomplex.properties.domain.model.ResidentialUnitSummary;

public final class ResolveResidentialUnitsService
        implements ResolveResidentialUnitsUseCase {

    private final LoadAvailableResidentialUnitsPort loadUnitsPort;

    public ResolveResidentialUnitsService(
            LoadAvailableResidentialUnitsPort loadUnitsPort
    ) {
        this.loadUnitsPort = loadUnitsPort;
    }

    @Override
    public List<ResidentialUnitSummary> resolveAvailable(
            UUID propertyId,
            Set<UUID> unitIds
    ) {
        if (propertyId == null || unitIds == null) {
            throw new IllegalArgumentException(
                    "propertyId and unitIds must not be null"
            );
        }
        if (unitIds.isEmpty()) {
            return List.of();
        }
        if (unitIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException(
                    "unitIds must not contain null values"
            );
        }
        return loadUnitsPort.loadByPropertyIdAndUnitIds(
                propertyId,
                Set.copyOf(unitIds)
        );
    }
}
