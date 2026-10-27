package com.mycomplex.users.infrastructure.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberDbo;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth0.test/",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth0.test/.well-known/jwks.json"
})
@Transactional
class UserPropertyJpaRepositoryTest {

    @Autowired
    private UserPropertyJpaRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void authorizesOnlyActivePropertyAdministrator() {
        UUID propertyId = insertProperty("Access property");
        String administratorSubject = "auth0|" + UUID.randomUUID();
        UUID administratorId = insertUser(
                administratorSubject,
                "ACTIVO",
                false
        );
        insertMembership(
                administratorId,
                propertyId,
                "ADMIN_COPROPIEDAD",
                "ACTIVA",
                false
        );

        String residentSubject = "auth0|" + UUID.randomUUID();
        UUID residentId = insertUser(
                residentSubject,
                "ACTIVO",
                false
        );
        insertMembership(
                residentId,
                propertyId,
                "RESIDENTE",
                "ACTIVA",
                false
        );

        assertTrue(repository.hasActiveAdministratorAccess(
                propertyId,
                administratorSubject
        ));
        assertFalse(repository.hasActiveAdministratorAccess(
                propertyId,
                residentSubject
        ));
        assertFalse(repository.hasActiveAdministratorAccess(
                propertyId,
                "auth0|not-associated"
        ));
    }

    @Test
    void returnsNonDeletedMembersOnlyFromRequestedProperty() {
        UUID requestedPropertyId = insertProperty(
                "Requested property"
        );
        UUID otherPropertyId = insertProperty("Other property");

        UUID activeMemberId = insertUser(
                "auth0|" + UUID.randomUUID(),
                "ACTIVO",
                false
        );
        insertMembership(
                activeMemberId,
                requestedPropertyId,
                "PROPIETARIO",
                "ACTIVA",
                false
        );

        UUID blockedMemberId = insertUser(
                "auth0|" + UUID.randomUUID(),
                "BLOQUEADO",
                false
        );
        insertMembership(
                blockedMemberId,
                requestedPropertyId,
                "RESIDENTE",
                "SUSPENDIDA",
                false
        );

        UUID deletedMemberId = insertUser(
                "auth0|" + UUID.randomUUID(),
                "ACTIVO",
                true
        );
        insertMembership(
                deletedMemberId,
                requestedPropertyId,
                "TRABAJADOR",
                "ACTIVA",
                false
        );

        UUID otherPropertyMemberId = insertUser(
                "auth0|" + UUID.randomUUID(),
                "ACTIVO",
                false
        );
        insertMembership(
                otherPropertyMemberId,
                otherPropertyId,
                "VIGILANTE",
                "ACTIVA",
                false
        );

        List<PropertyMemberDbo> members =
                repository.findMembersByPropertyId(
                        requestedPropertyId
                );

        assertEquals(2, members.size());
        assertTrue(members.stream().anyMatch(member ->
                member.userId().equals(activeMemberId)
                        && member.userStatus().equals("ACTIVO")
                        && member.membershipStatus().equals("ACTIVA")
        ));
        assertTrue(members.stream().anyMatch(member ->
                member.userId().equals(blockedMemberId)
                        && member.userStatus().equals("BLOQUEADO")
                        && member.membershipStatus().equals("SUSPENDIDA")
        ));
        assertFalse(members.stream().anyMatch(member ->
                member.userId().equals(deletedMemberId)
                        || member.userId().equals(otherPropertyMemberId)
        ));
    }

    private UUID insertProperty(String name) {
        UUID propertyId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO copropiedades (
                    id, nombre, nit, direccion, ciudad, estado
                ) VALUES (?, ?, ?, ?, ?, 'ACTIVA')
                """,
                propertyId,
                name,
                "NIT-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 20),
                "Test address",
                "Rionegro"
        );
        return propertyId;
    }

    private UUID insertUser(
            String auth0Subject,
            String status,
            boolean deleted
    ) {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO usuarios (
                    id, auth0_subject, correo, nombres, apellidos,
                    estado, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?,
                    CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END
                )
                """,
                userId,
                auth0Subject,
                userId + "@example.test",
                "Test",
                userId.toString(),
                status,
                deleted
        );
        return userId;
    }

    private void insertMembership(
            UUID userId,
            UUID propertyId,
            String role,
            String status,
            boolean deleted
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO usuarios_copropiedades (
                    id, usuario_id, copropiedad_id, rol,
                    estado, deleted_at
                ) VALUES (?, ?, ?, ?, ?,
                    CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END
                )
                """,
                UUID.randomUUID(),
                userId,
                propertyId,
                role,
                status,
                deleted
        );
    }
}
