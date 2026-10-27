package com.mycomplex.users.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.exception.InvalidPropertyMemberUpdateException;
import com.mycomplex.users.application.exception.LastPropertyAdministratorException;
import com.mycomplex.users.application.exception.PropertyMemberConflictException;
import com.mycomplex.users.application.exception.PropertyMemberNotFoundException;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.in.FindPropertyMemberDetailsUseCase;
import com.mycomplex.users.application.port.in.UpdatePropertyMemberUseCase;
import com.mycomplex.users.application.port.out.UpdatePropertyMemberPort;
import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMemberDetails;
import com.mycomplex.users.domain.model.PropertyMemberUpdate;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.domain.model.ResidentialAssignmentUpdate;
import com.mycomplex.users.domain.model.ResidentialRelationshipType;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth0.test/",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth0.test/.well-known/jwks.json"
})
@Transactional
class PropertyMemberManagementIntegrationTest {

    private static final UUID PROPERTY_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001"
    );
    private static final UUID ADMIN_MEMBERSHIP_ID = UUID.fromString(
            "30000000-0000-0000-0000-000000000001"
    );
    private static final UUID ANA_MEMBERSHIP_ID = UUID.fromString(
            "70000000-0000-0000-0000-000000000001"
    );
    private static final UUID UNIT_101 = UUID.fromString(
            "50000000-0000-0000-0000-000000000101"
    );
    private static final UUID UNIT_102 = UUID.fromString(
            "50000000-0000-0000-0000-000000000102"
    );
    private static final String ADMIN_SUBJECT =
            "auth0|6ad6a66c5703ba3cd5629f70";

    @Autowired
    private FindPropertyMemberDetailsUseCase findDetailsUseCase;

    @Autowired
    private UpdatePropertyMemberUseCase updateUseCase;

    @Autowired
    private UpdatePropertyMemberPort updatePort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsAuthorizedDetailsWithAvailableUnits() {
        PropertyMemberDetails details = findDetailsUseCase.find(
                PROPERTY_ID,
                ANA_MEMBERSHIP_ID,
                ADMIN_SUBJECT
        );

        assertEquals(ANA_MEMBERSHIP_ID,
                details.member().membershipId());
        assertEquals("Ana", details.member().firstNames());
        assertEquals(0, details.member().version());
        assertEquals(1, details.member().units().size());
        assertEquals(4, details.availableUnits().size());
    }

    @Test
    void updatesRoleStatusAndMultipleResidentialAssignments() {
        PropertyMemberDetails details = updateUseCase.update(
                update(
                        PropertyRole.RESIDENT,
                        MembershipStatus.SUSPENDED,
                        0,
                        List.of(
                                assignment(
                                        UNIT_101,
                                        ResidentialRelationshipType.RESIDENT,
                                        false
                                ),
                                assignment(
                                        UNIT_102,
                                        ResidentialRelationshipType.OWNER,
                                        true
                                )
                        )
                ),
                ADMIN_SUBJECT
        );

        assertEquals(PropertyRole.RESIDENT, details.member().role());
        assertEquals(
                MembershipStatus.SUSPENDED,
                details.member().membershipStatus()
        );
        assertEquals(1, details.member().version());
        assertEquals(2, details.member().units().size());
        assertTrue(details.member().units().stream().anyMatch(unit ->
                unit.unit().id().equals(UNIT_101)
                        && unit.relationshipType()
                        == ResidentialRelationshipType.RESIDENT
        ));
        assertTrue(details.member().units().stream().anyMatch(unit ->
                unit.unit().id().equals(UNIT_102)
                        && unit.primary()
        ));

        Integer finalizedOwner = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM usuarios_copropiedades_unidades
                WHERE usuario_copropiedad_id = ?
                  AND unidad_residencial_id = ?
                  AND tipo_vinculacion = 'PROPIETARIO'
                  AND estado = 'FINALIZADA'
                  AND fecha_fin IS NOT NULL
                """,
                Integer.class,
                ANA_MEMBERSHIP_ID,
                UNIT_101
        );
        assertEquals(1, finalizedOwner);
    }

    @Test
    void logicallyEndsRemovedAssignment() {
        PropertyMemberDetails details = updateUseCase.update(
                update(
                        PropertyRole.OWNER,
                        MembershipStatus.ACTIVE,
                        0,
                        List.of()
                ),
                ADMIN_SUBJECT
        );

        assertTrue(details.member().units().isEmpty());
        String status = jdbcTemplate.queryForObject(
                """
                SELECT estado
                FROM usuarios_copropiedades_unidades
                WHERE id = '80000000-0000-0000-0000-000000000001'
                """,
                String.class
        );
        assertEquals("FINALIZADA", status);
    }

    @Test
    void rejectsDuplicateUnitsAndMultiplePrimaryUnits() {
        ResidentialAssignmentUpdate first = assignment(
                UNIT_101,
                ResidentialRelationshipType.OWNER,
                true
        );

        assertThrows(
                InvalidPropertyMemberUpdateException.class,
                () -> updateUseCase.update(
                        update(
                                PropertyRole.OWNER,
                                MembershipStatus.ACTIVE,
                                0,
                                List.of(first, first)
                        ),
                        ADMIN_SUBJECT
                )
        );
        assertThrows(
                InvalidPropertyMemberUpdateException.class,
                () -> updateUseCase.update(
                        update(
                                PropertyRole.OWNER,
                                MembershipStatus.ACTIVE,
                                0,
                                List.of(
                                        first,
                                        assignment(
                                                UNIT_102,
                                                ResidentialRelationshipType.OWNER,
                                                true
                                        )
                                )
                        ),
                        ADMIN_SUBJECT
                )
        );
    }

    @Test
    void rejectsUnknownOrForeignUnitAndMembership() {
        assertThrows(
                InvalidPropertyMemberUpdateException.class,
                () -> updateUseCase.update(
                        update(
                                PropertyRole.OWNER,
                                MembershipStatus.ACTIVE,
                                0,
                                List.of(assignment(
                                        UUID.randomUUID(),
                                        ResidentialRelationshipType.OWNER,
                                        true
                                ))
                        ),
                        ADMIN_SUBJECT
                )
        );
        assertThrows(
                PropertyMemberNotFoundException.class,
                () -> findDetailsUseCase.find(
                        PROPERTY_ID,
                        UUID.randomUUID(),
                        ADMIN_SUBJECT
                )
        );
    }

    @Test
    void rejectsUserWithoutPermissionsOrInactiveGlobalUser() {
        assertThrows(
                PropertyMembersAccessDeniedException.class,
                () -> findDetailsUseCase.find(
                        PROPERTY_ID,
                        ANA_MEMBERSHIP_ID,
                        "demo|ana.gomez"
                )
        );

        jdbcTemplate.update(
                "UPDATE usuarios SET estado = 'INACTIVO' "
                        + "WHERE auth0_subject = ?",
                ADMIN_SUBJECT
        );
        assertThrows(
                PropertyMembersAccessDeniedException.class,
                () -> findDetailsUseCase.find(
                        PROPERTY_ID,
                        ANA_MEMBERSHIP_ID,
                        ADMIN_SUBJECT
                )
        );
    }

    @Test
    void rejectsInactiveOrDeletedProperty() {
        jdbcTemplate.update(
                "UPDATE copropiedades SET estado = 'INACTIVA' "
                        + "WHERE id = ?",
                PROPERTY_ID
        );
        assertThrows(
                PropertyMembersAccessDeniedException.class,
                () -> findDetailsUseCase.find(
                        PROPERTY_ID,
                        ANA_MEMBERSHIP_ID,
                        ADMIN_SUBJECT
                )
        );
    }

    @Test
    void protectsLastActiveAdministrator() {
        PropertyMemberUpdate update = new PropertyMemberUpdate(
                PROPERTY_ID,
                ADMIN_MEMBERSHIP_ID,
                PropertyRole.OWNER,
                MembershipStatus.ACTIVE,
                0,
                List.of()
        );

        assertThrows(
                LastPropertyAdministratorException.class,
                () -> updateUseCase.update(update, ADMIN_SUBJECT)
        );
    }

    @Test
    void rejectsStaleVersion() {
        assertThrows(
                PropertyMemberConflictException.class,
                () -> updateUseCase.update(
                        update(
                                PropertyRole.OWNER,
                                MembershipStatus.ACTIVE,
                                99,
                                List.of(assignment(
                                        UNIT_101,
                                        ResidentialRelationshipType.OWNER,
                                        true
                                ))
                        ),
                        ADMIN_SUBJECT
                )
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackEveryChangeWhenDatabaseConstraintFails() {
        PropertyMemberUpdate invalidUpdate = update(
                PropertyRole.OWNER,
                MembershipStatus.ACTIVE,
                0,
                List.of(
                        assignment(
                                UNIT_101,
                                ResidentialRelationshipType.OWNER,
                                true
                        ),
                        assignment(
                                UNIT_102,
                                ResidentialRelationshipType.OWNER,
                                true
                        )
                )
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> updatePort.update(invalidUpdate, ADMIN_SUBJECT)
        );

        Boolean originalPrimary = jdbcTemplate.queryForObject(
                """
                SELECT es_principal
                FROM usuarios_copropiedades_unidades
                WHERE id = '80000000-0000-0000-0000-000000000001'
                """,
                Boolean.class
        );
        Integer addedAssignments = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM usuarios_copropiedades_unidades
                WHERE usuario_copropiedad_id = ?
                  AND unidad_residencial_id = ?
                  AND estado = 'ACTIVA'
                """,
                Integer.class,
                ANA_MEMBERSHIP_ID,
                UNIT_102
        );
        Long version = jdbcTemplate.queryForObject(
                """
                SELECT version
                FROM usuarios_copropiedades
                WHERE id = ?
                """,
                Long.class,
                ANA_MEMBERSHIP_ID
        );

        assertEquals(Boolean.TRUE, originalPrimary);
        assertEquals(0, addedAssignments);
        assertEquals(0L, version);
    }

    private PropertyMemberUpdate update(
            PropertyRole role,
            MembershipStatus status,
            long version,
            List<ResidentialAssignmentUpdate> units
    ) {
        return new PropertyMemberUpdate(
                PROPERTY_ID,
                ANA_MEMBERSHIP_ID,
                role,
                status,
                version,
                units
        );
    }

    private ResidentialAssignmentUpdate assignment(
            UUID unitId,
            ResidentialRelationshipType type,
            boolean primary
    ) {
        return new ResidentialAssignmentUpdate(
                unitId,
                type,
                primary
        );
    }
}
