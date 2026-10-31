package com.mycomplex.observability.infrastructure.adapter.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.mycomplex.observability.application.port.out.AppendSecurityAuditEventPort;
import com.mycomplex.observability.domain.SecurityAuditEvent;

@Component
public class SecurityAuditPersistenceAdapter
        implements AppendSecurityAuditEventPort {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SecurityAuditPersistenceAdapter(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(SecurityAuditEvent event) {
        jdbcTemplate.update(
                """
                INSERT INTO eventos_auditoria_seguridad (
                    id, fecha_evento, tipo_evento, resultado,
                    actor_usuario_id, copropiedad_id, usuario_objetivo_id,
                    correlation_id, origen, metadata
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
                """,
                event.id(), event.occurredAt(), event.type().name(),
                event.result().name(), event.actorUserId(), event.propertyId(),
                event.targetUserId(), event.correlationId(), event.origin(),
                serializeMetadata(event)
        );
    }

    private String serializeMetadata(SecurityAuditEvent event) {
        try {
            return objectMapper.writeValueAsString(event.metadata());
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Security audit metadata cannot be serialized",
                    exception
            );
        }
    }
}
