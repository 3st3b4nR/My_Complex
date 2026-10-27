package com.mycomplex.users.infrastructure.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;

import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.domain.model.UserStatus;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberDbo;

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

    private PropertyMemberDbo dbo(
            String role,
            String userStatus,
            String membershipStatus
    ) {
        return new PropertyMemberDbo(
                UUID.fromString(
                        "20000000-0000-0000-0000-000000000001"
                ),
                "John Esteban",
                "Roldan Marin",
                "admin@mycomplex.com",
                role,
                userStatus,
                membershipStatus
        );
    }
}
