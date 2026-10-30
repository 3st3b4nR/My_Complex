package com.mycomplex.properties.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.properties.application.port.out.LoadAvailableResidentialUnitsPort;
import com.mycomplex.properties.domain.model.AvailableResidentialUnit;
import com.mycomplex.properties.domain.model.ResidentialUnitStatus;
import com.mycomplex.properties.domain.model.ResidentialUnitSummary;

class ResolveResidentialUnitsServiceTest {

    @Test
    void delegatesAllIdsInOneBatch() {
        UUID propertyId = UUID.randomUUID();
        Set<UUID> unitIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        ResidentialUnitSummary expected = new ResidentialUnitSummary(
                unitIds.iterator().next(), propertyId, null, null, null,
                "101", null, ResidentialUnitStatus.ACTIVE
        );
        LoadAvailableResidentialUnitsPort port = new StubPort(
                propertyId, unitIds, List.of(expected)
        );

        assertEquals(
                List.of(expected),
                new ResolveResidentialUnitsService(port)
                        .resolveAvailable(propertyId, unitIds)
        );
    }

    private record StubPort(
            UUID propertyId,
            Set<UUID> unitIds,
            List<ResidentialUnitSummary> result
    ) implements LoadAvailableResidentialUnitsPort {
        @Override
        public List<AvailableResidentialUnit> loadByPropertyId(UUID ignored) {
            return List.of();
        }

        @Override
        public List<ResidentialUnitSummary> loadByPropertyIdAndUnitIds(
                UUID actualPropertyId,
                Set<UUID> actualUnitIds
        ) {
            assertEquals(propertyId, actualPropertyId);
            assertEquals(unitIds, actualUnitIds);
            return result;
        }
    }
}
