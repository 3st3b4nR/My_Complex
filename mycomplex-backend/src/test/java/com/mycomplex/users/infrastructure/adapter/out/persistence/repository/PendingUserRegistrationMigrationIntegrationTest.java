package com.mycomplex.users.infrastructure.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth0.test/",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth0.test/.well-known/jwks.json"
})
@Transactional
class PendingUserRegistrationMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void marksExistingUsersAsLinked() {
        Integer invalid = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE estado_registro <> 'VINCULADO' OR auth0_subject IS NULL",
                Integer.class
        );
        assertEquals(0, invalid);
    }

    @Test
    void acceptsPendingUserWithoutAuth0Subject() {
        jdbcTemplate.update(
                """
                INSERT INTO usuarios (id, correo, nombres, apellidos, estado, estado_registro)
                VALUES (?, ?, 'Ana', 'Pendiente', 'ACTIVO', 'PENDIENTE')
                """,
                UUID.randomUUID(), "pending-" + UUID.randomUUID() + "@test.com"
        );
    }

    @Test
    void rejectsIncoherentIdentityRegistrationStates() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                INSERT INTO usuarios (id, correo, nombres, apellidos, estado, estado_registro)
                VALUES (?, ?, 'Ana', 'Sin identidad', 'ACTIVO', 'VINCULADO')
                """,
                UUID.randomUUID(), "linked-null-" + UUID.randomUUID() + "@test.com"
        ));
    }

    @Test
    void rejectsPendingUserWithAuth0Subject() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                INSERT INTO usuarios (id, auth0_subject, correo, nombres, apellidos, estado, estado_registro)
                VALUES (?, ?, ?, 'Ana', 'Incoherente', 'ACTIVO', 'PENDIENTE')
                """,
                UUID.randomUUID(), "auth0|" + UUID.randomUUID(),
                "pending-auth-" + UUID.randomUUID() + "@test.com"
        ));
    }

    @Test
    void preservesNormalizedEmailUniqueness() {
        String email = "Unique-" + UUID.randomUUID() + "@test.com";
        insertLinked(email, null, null);
        assertThrows(DataIntegrityViolationException.class,
                () -> insertLinked(email.toUpperCase(), null, null));
    }

    @Test
    void preservesDocumentUniqueness() {
        String number = UUID.randomUUID().toString();
        insertLinked("doc-a-" + UUID.randomUUID() + "@test.com", "CC", number);
        assertThrows(DataIntegrityViolationException.class,
                () -> insertLinked(
                        "doc-b-" + UUID.randomUUID() + "@test.com", "CC", number
                ));
    }

    private void insertLinked(
            String email,
            String documentType,
            String documentNumber
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO usuarios (
                    id, auth0_subject, correo, nombres, apellidos,
                    tipo_documento, numero_documento, estado, estado_registro
                ) VALUES (?, ?, ?, 'Usuario', 'Prueba', ?, ?, 'ACTIVO', 'VINCULADO')
                """,
                UUID.randomUUID(), "auth0|" + UUID.randomUUID(), email,
                documentType, documentNumber
        );
    }
}
