package com.mycomplex.users.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.mycomplex.users.application.exception.InvalidPropertyMemberRegistrationException;
import com.mycomplex.users.application.exception.LastPropertyAdministratorException;
import com.mycomplex.users.application.exception.PropertyMemberBusinessConflictException;
import com.mycomplex.users.application.exception.PropertyMemberConflictException;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.in.FindPropertyMemberCandidateUseCase;
import com.mycomplex.users.application.port.in.RegisterPropertyMemberUseCase;
import com.mycomplex.users.application.port.in.UnlinkPropertyMemberUseCase;
import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMemberCandidateState;
import com.mycomplex.users.domain.model.PropertyMemberDetails;
import com.mycomplex.users.domain.model.PropertyMemberRegistration;
import com.mycomplex.users.domain.model.PropertyMemberRegistrationMode;
import com.mycomplex.users.domain.model.PropertyMemberUnlink;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.domain.model.ResidentialAssignmentUpdate;
import com.mycomplex.users.domain.model.ResidentialRelationshipType;
import com.mycomplex.users.domain.model.UserRegistrationStatus;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth0.test/",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth0.test/.well-known/jwks.json"
})
@Transactional
class PropertyMemberRegistrationIntegrationTest {

    private static final UUID PROPERTY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_MEMBERSHIP_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID ANA_MEMBERSHIP_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
    private static final UUID UNIT_102 = UUID.fromString("50000000-0000-0000-0000-000000000102");
    private static final String ADMIN_SUBJECT = "auth0|6ad6a66c5703ba3cd5629f70";

    @Autowired private FindPropertyMemberCandidateUseCase candidateUseCase;
    @Autowired private RegisterPropertyMemberUseCase registerUseCase;
    @Autowired private UnlinkPropertyMemberUseCase unlinkUseCase;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void createsPendingUserMembershipAndAssignmentAtomically() {
        String email = "new-" + UUID.randomUUID() + "@test.com";
        assertEquals(PropertyMemberCandidateState.NEW_USER,
                candidateUseCase.find(PROPERTY_ID, email, ADMIN_SUBJECT).state());

        PropertyMemberDetails result = registerUseCase.register(registration(
                PropertyMemberRegistrationMode.CREATE_PENDING_USER,
                null, null, "Nueva", "Persona", email,
                List.of(new ResidentialAssignmentUpdate(
                        UNIT_102, ResidentialRelationshipType.RESIDENT, true
                ))
        ), ADMIN_SUBJECT);

        assertEquals(UserRegistrationStatus.PENDING,
                result.member().registrationStatus());
        assertEquals(1, result.member().units().size());
        String auth0 = jdbcTemplate.queryForObject(
                "SELECT auth0_subject FROM usuarios WHERE id = ?",
                String.class, result.member().userId()
        );
        assertEquals(null, auth0);
    }

    @Test
    void linksExistingUserWithoutChangingGlobalData() {
        UUID userId = UUID.randomUUID();
        String email = "existing-" + UUID.randomUUID() + "@test.com";
        insertLinkedUser(userId, email);
        PropertyMemberDetails result = registerUseCase.register(registration(
                PropertyMemberRegistrationMode.LINK_EXISTING_USER,
                userId, null, null, null, email, List.of()
        ), ADMIN_SUBJECT);
        assertEquals(userId, result.member().userId());
        assertEquals("Existente", result.member().firstNames());
        assertEquals(UserRegistrationStatus.LINKED,
                result.member().registrationStatus());
    }

    @Test
    void rejectsDuplicateMembershipAndDeletedGlobalUser() {
        assertThrows(PropertyMemberBusinessConflictException.class,
                () -> registerUseCase.register(registration(
                        PropertyMemberRegistrationMode.LINK_EXISTING_USER,
                        UUID.fromString("20000000-0000-0000-0000-000000000002"),
                        null, null, null, "ana.gomez@loscerezos.test", List.of()
                ), ADMIN_SUBJECT));

        UUID deletedId = UUID.randomUUID();
        String email = "deleted-" + UUID.randomUUID() + "@test.com";
        insertLinkedUser(deletedId, email);
        jdbcTemplate.update("UPDATE usuarios SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?", deletedId);
        assertEquals(PropertyMemberCandidateState.DELETED_USER,
                candidateUseCase.find(PROPERTY_ID, email, ADMIN_SUBJECT).state());
    }

    @Test
    void explicitlyReactivatesDeletedMembershipWithoutOldAssignments() {
        UUID userId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        String email = "reactivate-" + UUID.randomUUID() + "@test.com";
        insertLinkedUser(userId, email);
        jdbcTemplate.update(
                """
                INSERT INTO usuarios_copropiedades (id, usuario_id, copropiedad_id, rol, estado, deleted_at)
                VALUES (?, ?, ?, 'RESIDENTE', 'INACTIVA', CURRENT_TIMESTAMP)
                """, membershipId, userId, PROPERTY_ID
        );
        assertEquals(PropertyMemberCandidateState.DELETED_MEMBERSHIP,
                candidateUseCase.find(PROPERTY_ID, email, ADMIN_SUBJECT).state());
        PropertyMemberDetails result = registerUseCase.register(registration(
                PropertyMemberRegistrationMode.REACTIVATE_MEMBERSHIP,
                userId, membershipId, null, null, email, List.of()
        ), ADMIN_SUBJECT);
        assertEquals(membershipId, result.member().membershipId());
        assertTrue(result.member().units().isEmpty());
    }

    @Test
    void rejectsIncompleteDocumentsDuplicateUnitsAndForeignUnits() {
        String email = "invalid-" + UUID.randomUUID() + "@test.com";
        PropertyMemberRegistration incomplete = new PropertyMemberRegistration(
                PROPERTY_ID, PropertyMemberRegistrationMode.CREATE_PENDING_USER,
                null, null, "Ana", "Prueba", email, "CC", null,
                PropertyRole.RESIDENT, MembershipStatus.ACTIVE, List.of()
        );
        assertThrows(InvalidPropertyMemberRegistrationException.class,
                () -> registerUseCase.register(incomplete, ADMIN_SUBJECT));
        ResidentialAssignmentUpdate assignment = new ResidentialAssignmentUpdate(
                UNIT_102, ResidentialRelationshipType.RESIDENT, true
        );
        assertThrows(InvalidPropertyMemberRegistrationException.class,
                () -> registerUseCase.register(new PropertyMemberRegistration(
                        PROPERTY_ID, PropertyMemberRegistrationMode.CREATE_PENDING_USER,
                        null, null, "Ana", "Prueba", email, null, null,
                        PropertyRole.RESIDENT, MembershipStatus.ACTIVE,
                        List.of(assignment, assignment)
                ), ADMIN_SUBJECT));
        assertThrows(InvalidPropertyMemberRegistrationException.class,
                () -> registerUseCase.register(registration(
                        PropertyMemberRegistrationMode.CREATE_PENDING_USER,
                        null, null, "Ana", "Prueba", email,
                        List.of(new ResidentialAssignmentUpdate(
                                UUID.randomUUID(), ResidentialRelationshipType.RESIDENT, true
                        ))
                ), ADMIN_SUBJECT));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectsDuplicateDocumentWithoutLeavingAnIncompleteUser() {
        String document = UUID.randomUUID().toString();
        jdbcTemplate.update(
                """
                INSERT INTO usuarios (
                    id, auth0_subject, correo, nombres, apellidos,
                    tipo_documento, numero_documento, estado, estado_registro
                ) VALUES (?, ?, ?, 'Documento', 'Existente', 'CC', ?, 'ACTIVO', 'VINCULADO')
                """,
                UUID.randomUUID(), "auth0|" + UUID.randomUUID(),
                "doc-existing-" + UUID.randomUUID() + "@test.com", document
        );
        String email = "doc-new-" + UUID.randomUUID() + "@test.com";
        PropertyMemberRegistration registration = new PropertyMemberRegistration(
                PROPERTY_ID, PropertyMemberRegistrationMode.CREATE_PENDING_USER,
                null, null, "Documento", "Duplicado", email, "CC", document,
                PropertyRole.RESIDENT, MembershipStatus.ACTIVE, List.of()
        );
        assertThrows(PropertyMemberBusinessConflictException.class,
                () -> registerUseCase.register(registration, ADMIN_SUBJECT));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE correo = ?",
                Integer.class, email
        ));
    }

    @Test
    void logicallyUnlinksMembershipAndFinalizesAssignmentsButKeepsUser() {
        UUID userId = jdbcTemplate.queryForObject(
                "SELECT usuario_id FROM usuarios_copropiedades WHERE id = ?",
                UUID.class, ANA_MEMBERSHIP_ID
        );
        unlinkUseCase.unlink(new PropertyMemberUnlink(
                PROPERTY_ID, ANA_MEMBERSHIP_ID, 0
        ), ADMIN_SUBJECT);
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM usuarios_copropiedades WHERE id = ?",
                java.time.OffsetDateTime.class, ANA_MEMBERSHIP_ID
        ));
        assertEquals("FINALIZADA", jdbcTemplate.queryForObject(
                "SELECT estado FROM usuarios_copropiedades_unidades WHERE usuario_copropiedad_id = ? ORDER BY created_at LIMIT 1",
                String.class, ANA_MEMBERSHIP_ID
        ));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE id = ? AND deleted_at IS NULL",
                Integer.class, userId
        ));
    }

    @Test
    void protectsLastAdministratorVersionAndAuthorization() {
        assertThrows(LastPropertyAdministratorException.class,
                () -> unlinkUseCase.unlink(new PropertyMemberUnlink(
                        PROPERTY_ID, ADMIN_MEMBERSHIP_ID, 0
                ), ADMIN_SUBJECT));
        assertThrows(PropertyMemberConflictException.class,
                () -> unlinkUseCase.unlink(new PropertyMemberUnlink(
                        PROPERTY_ID, ANA_MEMBERSHIP_ID, 99
                ), ADMIN_SUBJECT));
        assertThrows(PropertyMembersAccessDeniedException.class,
                () -> candidateUseCase.find(PROPERTY_ID, "x@test.com", "demo|ana.gomez"));
    }

    private PropertyMemberRegistration registration(
            PropertyMemberRegistrationMode mode, UUID userId,
            UUID membershipId, String firstNames, String lastNames,
            String email, List<ResidentialAssignmentUpdate> units
    ) {
        return new PropertyMemberRegistration(
                PROPERTY_ID, mode, userId, membershipId, firstNames,
                lastNames, email, null, null, PropertyRole.RESIDENT,
                MembershipStatus.ACTIVE, units
        );
    }

    private void insertLinkedUser(UUID id, String email) {
        jdbcTemplate.update(
                """
                INSERT INTO usuarios (id, auth0_subject, correo, nombres, apellidos, estado, estado_registro)
                VALUES (?, ?, ?, 'Existente', 'Usuario', 'ACTIVO', 'VINCULADO')
                """, id, "auth0|" + UUID.randomUUID(), email
        );
    }
}
