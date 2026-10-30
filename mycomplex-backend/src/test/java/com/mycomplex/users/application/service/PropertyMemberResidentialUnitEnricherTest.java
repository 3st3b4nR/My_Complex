package com.mycomplex.users.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.mycomplex.properties.application.port.in.ResolveResidentialUnitsUseCase;
import com.mycomplex.properties.domain.model.ResidentialUnitReference;
import com.mycomplex.properties.domain.model.ResidentialUnitStatus;
import com.mycomplex.properties.domain.model.ResidentialUnitSummary;
import com.mycomplex.properties.domain.model.ResidentialUnitType;
import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberUnit;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.domain.model.ResidentialRelationshipStatus;
import com.mycomplex.users.domain.model.ResidentialRelationshipType;
import com.mycomplex.users.domain.model.UserRegistrationStatus;
import com.mycomplex.users.domain.model.UserStatus;

class PropertyMemberResidentialUnitEnricherTest {

    @Test
    void resolvesAllMembersWithOneBatchCall() {
        UUID propertyId = UUID.randomUUID();
        UUID firstUnit = UUID.randomUUID();
        UUID secondUnit = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();
        ResolveResidentialUnitsUseCase resolver = (requestedProperty, ids) -> {
            calls.incrementAndGet();
            assertEquals(propertyId, requestedProperty);
            assertEquals(Set.of(firstUnit, secondUnit), ids);
            return ids.stream().map(id -> new ResidentialUnitSummary(
                    id, propertyId, null, null, null,
                    id.equals(firstUnit) ? "101" : "102",
                    ResidentialUnitType.APARTMENT,
                    ResidentialUnitStatus.ACTIVE
            )).toList();
        };

        List<PropertyMember> result =
                new PropertyMemberResidentialUnitEnricher(resolver)
                        .enrichAll(propertyId, List.of(
                                member(firstUnit), member(secondUnit)
                        ));

        assertEquals(1, calls.get());
        assertEquals("101", result.get(0).units().getFirst().unit().number());
        assertEquals("102", result.get(1).units().getFirst().unit().number());
    }

    private PropertyMember member(UUID unitId) {
        PropertyMemberUnit assignment = new PropertyMemberUnit(
                UUID.randomUUID(),
                new ResidentialUnitReference(unitId, null, null, null),
                ResidentialRelationshipType.RESIDENT,
                true,
                ResidentialRelationshipStatus.ACTIVE
        );
        return new PropertyMember(
                UUID.randomUUID(), UUID.randomUUID(), "Ana", "Gómez",
                "ana@example.com", PropertyRole.RESIDENT, UserStatus.ACTIVE,
                UserRegistrationStatus.LINKED, MembershipStatus.ACTIVE,
                0, List.of(assignment)
        );
    }
}
