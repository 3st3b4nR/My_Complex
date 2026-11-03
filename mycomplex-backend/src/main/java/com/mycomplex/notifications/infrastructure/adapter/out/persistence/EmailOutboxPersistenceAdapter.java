package com.mycomplex.notifications.infrastructure.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.mycomplex.notifications.application.model.EmailNotification;
import com.mycomplex.notifications.application.port.out.EmailOutboxPort;
import com.mycomplex.notifications.domain.EmailOutboxEvent;

@Component
public class EmailOutboxPersistenceAdapter implements EmailOutboxPort {

    private final JdbcTemplate jdbcTemplate;

    public EmailOutboxPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void enqueue(EmailNotification notification) {
        for (String recipient : notification.recipients()) {
            jdbcTemplate.update(
                    """
                    INSERT INTO eventos_correo (
                        id, destinatario, asunto, cuerpo, clave_entrega,
                        estado, intentos, proximo_intento_at,
                        created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, 'PENDIENTE', 0,
                              CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                              CURRENT_TIMESTAMP)
                    ON CONFLICT (clave_entrega, destinatario) DO NOTHING
                    """,
                    UUID.randomUUID(),
                    recipient,
                    notification.subject(),
                    notification.body(),
                    notification.deliveryKey()
            );
        }
    }

    @Override
    public Optional<EmailOutboxEvent> claimNext() {
        return jdbcTemplate.query(
                """
                SELECT id, destinatario, asunto, cuerpo, clave_entrega,
                       intentos, proximo_intento_at
                FROM eventos_correo
                WHERE estado = 'PENDIENTE'
                  AND proximo_intento_at <= CURRENT_TIMESTAMP
                ORDER BY proximo_intento_at, created_at
                LIMIT 1
                FOR UPDATE SKIP LOCKED
                """,
                (resultSet, rowNumber) -> new EmailOutboxEvent(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("destinatario"),
                        resultSet.getString("asunto"),
                        resultSet.getString("cuerpo"),
                        resultSet.getString("clave_entrega"),
                        resultSet.getInt("intentos"),
                        resultSet.getObject(
                                "proximo_intento_at",
                                OffsetDateTime.class
                        )
                )
        ).stream().findFirst();
    }

    @Override
    public void markSent(UUID eventId, String externalMessageId) {
        jdbcTemplate.update(
                """
                UPDATE eventos_correo
                SET estado = 'ENVIADO', intentos = intentos + 1,
                    enviado_at = CURRENT_TIMESTAMP,
                    mensaje_externo_id = ?, ultimo_error = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND estado = 'PENDIENTE'
                """,
                limit(externalMessageId, 255),
                eventId
        );
    }

    @Override
    public void markFailed(UUID eventId, String safeReason) {
        jdbcTemplate.update(
                """
                UPDATE eventos_correo
                SET estado = 'FALLIDO', intentos = intentos + 1,
                    ultimo_error = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND estado = 'PENDIENTE'
                """,
                limit(safeReason, 120),
                eventId
        );
    }

    @Override
    public void scheduleRetry(
            UUID eventId,
            int attempts,
            Duration delay,
            String safeReason
    ) {
        OffsetDateTime nextAttempt = OffsetDateTime.now(ZoneOffset.UTC)
                .plus(delay);
        jdbcTemplate.update(
                """
                UPDATE eventos_correo
                SET intentos = ?, proximo_intento_at = ?,
                    ultimo_error = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND estado = 'PENDIENTE'
                """,
                attempts,
                Timestamp.from(nextAttempt.toInstant()),
                limit(safeReason, 120),
                eventId
        );
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String safe = value.strip();
        return safe.length() <= maxLength
                ? safe
                : safe.substring(0, maxLength);
    }
}
