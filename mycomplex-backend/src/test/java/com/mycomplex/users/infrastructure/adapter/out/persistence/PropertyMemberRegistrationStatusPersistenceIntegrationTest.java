package com.mycomplex.users.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.UserRegistrationStatus;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth0.test/",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth0.test/.well-known/jwks.json"
})
@Transactional
class PropertyMemberRegistrationStatusPersistenceIntegrationTest {

    @Autowired
    private PropertyMemberPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listsMembersInEveryRegistrationStatusIncludingInvited() {
        UUID propertyId = insertProperty();
        insertMember(propertyId, "PENDIENTE", null, false);
        insertMember(propertyId, "APROVISIONANDO", null, false);
        insertMember(
                propertyId,
                "PROVISIONADO",
                "auth0|" + UUID.randomUUID(),
                false
        );
        insertMember(
                propertyId,
                "INVITADO",
                "auth0|" + UUID.randomUUID(),
                true
        );
        insertMember(
                propertyId,
                "VINCULADO",
                "auth0|" + UUID.randomUUID(),
                false
        );

        List<PropertyMember> members = adapter.loadByPropertyId(propertyId);
        Map<UserRegistrationStatus, PropertyMember> byStatus = members.stream()
                .collect(Collectors.toMap(
                        PropertyMember::registrationStatus,
                        Function.identity()
                ));

        assertEquals(5, members.size());
        assertEquals(5, byStatus.size());
        assertNull(byStatus.get(
                UserRegistrationStatus.PENDING
        ).invitationSentAt());
        assertNull(byStatus.get(
                UserRegistrationStatus.PROVISIONING
        ).invitationSentAt());
        assertNull(byStatus.get(
                UserRegistrationStatus.PROVISIONED
        ).invitationSentAt());
        assertNotNull(byStatus.get(
                UserRegistrationStatus.INVITED
        ).invitationSentAt());
        assertNull(byStatus.get(
                UserRegistrationStatus.LINKED
        ).invitationSentAt());
    }

    private UUID insertProperty() {
        UUID propertyId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO copropiedades (
                    id, nombre, nit, direccion, ciudad, estado
                ) VALUES (?, ?, ?, ?, ?, 'ACTIVA')
                """,
                propertyId,
                "Registration status property",
                "NIT-" + UUID.randomUUID().toString().substring(0, 20),
                "Test address",
                "Rionegro"
        );
        return propertyId;
    }

    private void insertMember(
            UUID propertyId,
            String registrationStatus,
            String auth0Subject,
            boolean invitationSent
    ) {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO usuarios (
                    id, auth0_subject, correo, nombres, apellidos,
                    estado, estado_registro, invitacion_enviada_at
                ) VALUES (?, ?, ?, ?, ?, 'ACTIVO', ?,
                    CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END
                )
                """,
                userId,
                auth0Subject,
                userId + "@example.test",
                "Test",
                registrationStatus,
                registrationStatus,
                invitationSent
        );
        jdbcTemplate.update(
                """
                INSERT INTO usuarios_copropiedades (
                    id, usuario_id, copropiedad_id, rol, estado
                ) VALUES (?, ?, ?, 'RESIDENTE', 'ACTIVA')
                """,
                UUID.randomUUID(),
                userId,
                propertyId
        );
    }
}
