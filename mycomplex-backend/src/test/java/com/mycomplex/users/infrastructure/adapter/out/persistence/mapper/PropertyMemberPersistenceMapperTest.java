package com.mycomplex.users.infrastructure.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;

import com.mycomplex.properties.domain.model.PropertyStructureType;
import com.mycomplex.properties.domain.model.ResidentialUnitType;
import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberUnit;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.domain.model.ResidentialRelationshipStatus;
import com.mycomplex.users.domain.model.ResidentialRelationshipType;
import com.mycomplex.users.domain.model.UserStatus;
import com.mycomplex.users.domain.model.UserRegistrationStatus;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberDbo;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberUnitDbo;

class PropertyMemberPersistenceMapperTest {

    private final PropertyMemberPersistenceMapper mapper =
            Mappers.getMapper(
                    PropertyMemberPersistenceMapper.class
            );

    @ParameterizedTest
    @CsvSource({
        "ADMIN_COPROPIEDAD, PROPERTY_ADMIN",
        "PROPIETARIO, OWNER",
        "RESIDENTE, RESIDENT",
        "VIGILANTE, SECURITY_GUARD",
        "TRABAJADOR, WORKER"
    })
    void mapsDatabaseRolesToDomain(
            String databaseRole,
            PropertyRole expectedRole
    ) {
        PropertyMember result = mapper.toDomain(
                dbo(databaseRole, "ACTIVO", "ACTIVA")
        );

        assertEquals(expectedRole, result.role());
    }

    @ParameterizedTest
    @CsvSource({
        "ACTIVO, ACTIVE",
        "INACTIVO, INACTIVE",
        "BLOQUEADO, BLOCKED"
    })
    void mapsDatabaseUserStatusesToDomain(
            String databaseStatus,
            UserStatus expectedStatus
    ) {
        PropertyMember result = mapper.toDomain(
                dbo("RESIDENTE", databaseStatus, "ACTIVA")
        );

        assertEquals(expectedStatus, result.userStatus());
    }

    @ParameterizedTest
    @CsvSource({
        "ACTIVA, ACTIVE",
        "INACTIVA, INACTIVE",
        "SUSPENDIDA, SUSPENDED"
    })
    void mapsDatabaseMembershipStatusesToDomain(
            String databaseStatus,
            MembershipStatus expectedStatus
    ) {
        PropertyMember result = mapper.toDomain(
                dbo("RESIDENTE", "ACTIVO", databaseStatus)
        );

        assertEquals(expectedStatus, result.membershipStatus());
    }

    @ParameterizedTest
    @CsvSource({
        "PENDIENTE, PENDING",
        "VINCULADO, LINKED"
    })
    void mapsDatabaseRegistrationStatusesToDomain(
            String databaseStatus,
            UserRegistrationStatus expectedStatus
    ) {
        PropertyMember result = mapper.toDomain(new PropertyMemberDbo(
                UUID.randomUUID(), UUID.randomUUID(), "Ana", "Gómez",
                "ana@example.com", "RESIDENTE", "ACTIVO",
                databaseStatus, "ACTIVA", 0, java.util.List.of()
        ));
        assertEquals(expectedStatus, result.registrationStatus());
    }

    @ParameterizedTest
    @CsvSource({
        "TORRE, TOWER",
        "BLOQUE, BLOCK",
        "SECTOR, SECTOR",
        "OTRO, OTHER"
    })
    void mapsDatabaseStructureTypesToDomain(
            String databaseType,
            PropertyStructureType expectedType
    ) {
        PropertyMemberUnit result = mapper.toDomain(
                unitDbo(databaseType, "APARTAMENTO", "PROPIETARIO")
        );

        assertEquals(expectedType, result.unit().structure().type());
    }

    @ParameterizedTest
    @CsvSource({
        "APARTAMENTO, APARTMENT",
        "CASA, HOUSE",
        "LOCAL, COMMERCIAL_SPACE",
        "OFICINA, OFFICE",
        "OTRO, OTHER"
    })
    void mapsDatabaseUnitTypesToDomain(
            String databaseType,
            ResidentialUnitType expectedType
    ) {
        PropertyMemberUnit result = mapper.toDomain(
                unitDbo("TORRE", databaseType, "PROPIETARIO")
        );

        assertEquals(expectedType, result.unit().type());
    }

    @ParameterizedTest
    @CsvSource({
        "PROPIETARIO, OWNER",
        "RESIDENTE, RESIDENT"
    })
    void mapsDatabaseRelationshipTypesToDomain(
            String databaseType,
            ResidentialRelationshipType expectedType
    ) {
        PropertyMemberUnit result = mapper.toDomain(
                unitDbo("TORRE", "APARTAMENTO", databaseType)
        );

        assertEquals(expectedType, result.relationshipType());
        assertEquals(
                ResidentialRelationshipStatus.ACTIVE,
                result.status()
        );
    }

    private PropertyMemberDbo dbo(
            String role,
            String userStatus,
            String membershipStatus
    ) {
        return new PropertyMemberDbo(
                UUID.fromString(
                        "30000000-0000-0000-0000-000000000001"
                ),
                UUID.fromString(
                        "20000000-0000-0000-0000-000000000001"
                ),
                "John Esteban",
                "Roldan Marin",
                "admin@mycomplex.com",
                role,
                userStatus,
                "VINCULADO",
                membershipStatus,
                0,
                java.util.List.of()
        );
    }

    private PropertyMemberUnitDbo unitDbo(
            String structureType,
            String unitType,
            String relationshipType
    ) {
        return new PropertyMemberUnitDbo(
                UUID.fromString(
                        "80000000-0000-0000-0000-000000000001"
                ),
                UUID.fromString(
                        "50000000-0000-0000-0000-000000000101"
                ),
                "Torre 1",
                structureType,
                "101",
                unitType,
                relationshipType,
                true,
                "ACTIVA"
        );
    }
}
