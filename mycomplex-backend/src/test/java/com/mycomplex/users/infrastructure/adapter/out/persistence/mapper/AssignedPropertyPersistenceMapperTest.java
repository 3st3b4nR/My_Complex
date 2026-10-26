package com.mycomplex.users.infrastructure.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.mycomplex.users.domain.model.AssignedProperty;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.AssignedPropertyDbo;

class AssignedPropertyPersistenceMapperTest {

    private final AssignedPropertyPersistenceMapper mapper =
            Mappers.getMapper(
                    AssignedPropertyPersistenceMapper.class
            );

    @Test
    void mapsSpanishDatabaseRoleToEnglishDomainRole() {
        UUID propertyId = UUID.fromString(
                "10000000-0000-0000-0000-000000000001"
        );

        AssignedPropertyDbo dbo = new AssignedPropertyDbo(
                propertyId,
                "Los Cerezos",
                "Rionegro",
                "ADMIN_COPROPIEDAD"
        );

        AssignedProperty result = mapper.toDomain(dbo);

        AssignedProperty expected = new AssignedProperty(
                propertyId,
                "Los Cerezos",
                "Rionegro",
                PropertyRole.PROPERTY_ADMIN
        );

        assertEquals(expected, result);
    }
}