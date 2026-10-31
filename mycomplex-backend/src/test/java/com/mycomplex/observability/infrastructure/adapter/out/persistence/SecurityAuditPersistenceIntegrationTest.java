package com.mycomplex.observability.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.observability.api.CorrelationIds;
import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.api.SecurityAuditCommand;
import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.observability.api.SecurityAuditResult;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth0.test/",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth0.test/.well-known/jwks.json"
})
@Transactional
class SecurityAuditPersistenceIntegrationTest {

    @Autowired
    private RecordSecurityAuditUseCase auditUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsAppendOnlyEventWithLimitedMetadata() {
        UUID propertyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        insertProperty(propertyId);
        insertUser(actorId, "actor");
        insertUser(targetId, "target");
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CorrelationIds.MDC_KEY, correlationId);
        try {
            auditUseCase.record(new SecurityAuditCommand(
                    SecurityAuditEventType.MEMBER_REGISTERED,
                    SecurityAuditResult.SUCCESS,
                    actorId, propertyId, targetId, "users",
                    Map.of("registrationMode", "CREATE_PENDING_USER")
            ));
        } finally {
            MDC.remove(CorrelationIds.MDC_KEY);
        }

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                SELECT tipo_evento, resultado, actor_usuario_id,
                       copropiedad_id, usuario_objetivo_id,
                       correlation_id, metadata::text AS metadata
                FROM eventos_auditoria_seguridad
                WHERE correlation_id = ?
                """,
                correlationId
        );
        assertThat(row.get("tipo_evento")).isEqualTo("MEMBER_REGISTERED");
        assertThat(row.get("resultado")).isEqualTo("SUCCESS");
        assertThat(row.get("actor_usuario_id")).isEqualTo(actorId);
        assertThat(row.get("copropiedad_id")).isEqualTo(propertyId);
        assertThat(row.get("usuario_objetivo_id")).isEqualTo(targetId);
        assertThat(row.get("metadata").toString())
                .contains("CREATE_PENDING_USER")
                .doesNotContain("email", "document", "token");
    }

    private void insertProperty(UUID id) {
        jdbcTemplate.update(
                """
                INSERT INTO copropiedades (
                    id, nombre, nit, direccion, ciudad, estado
                ) VALUES (?, ?, ?, ?, ?, 'ACTIVA')
                """,
                id, "Audit property",
                "NIT-" + id.toString().substring(0, 8),
                "Audit address", "Bogota"
        );
    }

    private void insertUser(UUID id, String label) {
        jdbcTemplate.update(
                """
                INSERT INTO usuarios (
                    id, auth0_subject, correo, nombres, apellidos, estado
                ) VALUES (?, ?, ?, ?, ?, 'ACTIVO')
                """,
                id, "auth0|" + id, label + id + "@example.test",
                "Audit", label
        );
    }
}
