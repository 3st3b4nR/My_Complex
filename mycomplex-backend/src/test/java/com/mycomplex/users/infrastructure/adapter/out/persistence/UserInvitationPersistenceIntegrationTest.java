package com.mycomplex.users.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.exception.UserInvitationException;
import com.mycomplex.users.application.port.out.UserInvitationPersistencePort;
import com.mycomplex.users.domain.model.InvitationClaim;
import com.mycomplex.users.domain.model.UserRegistrationStatus;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth0.test/",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth0.test/.well-known/jwks.json"
})
@Transactional
class UserInvitationPersistenceIntegrationTest {

    private static final UUID PROPERTY_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001"
    );
    private static final String ADMIN_SUBJECT =
            "auth0|6ad6a66c5703ba3cd5629f70";

    @Autowired
    private UserInvitationPersistencePort persistence;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsTheCompleteDurableStateMachineAndLinksIdempotently() {
        TestMember member = insertPendingMember();

        InvitationClaim claim = persistence.claim(
                PROPERTY_ID, member.membershipId(), ADMIN_SUBJECT, false,
                Duration.ofMinutes(5), Duration.ofMinutes(5)
        );
        assertEquals(InvitationClaim.Action.PROVISION, claim.action());
        assertEquals("APROVISIONANDO", registrationState(member.userId()));
        assertEquals(1, attempts(member.userId()));

        String subject = "auth0|" + UUID.randomUUID();
        persistence.markProvisioned(member.userId(), subject);
        assertEquals("PROVISIONADO", registrationState(member.userId()));

        persistence.recordInvitationFailure(member.userId(), "HTTP timeout");
        assertEquals("PROVISIONADO", registrationState(member.userId()));

        persistence.markInvited(member.userId(), member.membershipId());
        assertEquals("INVITADO", registrationState(member.userId()));

        assertTrue(persistence.linkAuthenticatedUser(subject));
        assertEquals("VINCULADO", registrationState(member.userId()));
        Long version = version(member.userId());
        assertFalse(persistence.linkAuthenticatedUser(subject));
        assertEquals(version, version(member.userId()));
    }

    @Test
    void enforcesTenantAuthorizationAndExplicitResendCooldown() {
        TestMember member = insertPendingMember();
        assertThrows(PropertyMembersAccessDeniedException.class, () ->
                persistence.claim(
                        UUID.randomUUID(), member.membershipId(),
                        ADMIN_SUBJECT, false, Duration.ofMinutes(5),
                        Duration.ofMinutes(5)
                ));

        InvitationClaim claim = persistence.claim(
                PROPERTY_ID, member.membershipId(), ADMIN_SUBJECT, false,
                Duration.ofMinutes(5), Duration.ofMinutes(5)
        );
        String subject = "auth0|" + UUID.randomUUID();
        persistence.markProvisioned(claim.userId(), subject);
        persistence.markInvited(claim.userId(), claim.membershipId());

        assertThrows(UserInvitationException.class, () -> persistence.claim(
                PROPERTY_ID, member.membershipId(), ADMIN_SUBJECT, true,
                Duration.ofMinutes(5), Duration.ofMinutes(5)
        ));
    }

    private TestMember insertPendingMember() {
        UUID userId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO usuarios (
                    id, correo, nombres, apellidos, estado, estado_registro
                ) VALUES (?, ?, 'Invitada', 'Prueba', 'ACTIVO', 'PENDIENTE')
                """,
                userId, "invite-" + UUID.randomUUID() + "@test.com"
        );
        jdbcTemplate.update(
                """
                INSERT INTO usuarios_copropiedades (
                    id, usuario_id, copropiedad_id, rol, estado
                ) VALUES (?, ?, ?, 'RESIDENTE', 'ACTIVA')
                """,
                membershipId, userId, PROPERTY_ID
        );
        return new TestMember(userId, membershipId);
    }

    private String registrationState(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT estado_registro FROM usuarios WHERE id = ?",
                String.class, userId
        );
    }

    private Integer attempts(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT intentos_registro FROM usuarios WHERE id = ?",
                Integer.class, userId
        );
    }

    private Long version(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM usuarios WHERE id = ?",
                Long.class, userId
        );
    }

    private record TestMember(UUID userId, UUID membershipId) { }
}
