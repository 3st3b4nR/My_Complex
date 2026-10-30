package com.mycomplex.properties.application.port.in;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mycomplex.properties.domain.model.ResidentialUnitSummary;

/** Batch API used by other modules; it never exposes persistence details. */
public interface ResolveResidentialUnitsUseCase {

    List<ResidentialUnitSummary> resolveAvailable(
            UUID propertyId,
            Set<UUID> unitIds
    );
}
