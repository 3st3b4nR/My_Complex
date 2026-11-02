package com.mycomplex.notifications.infrastructure.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.mycomplex.notifications.api.NotificationDeliveryStatus;
import com.mycomplex.notifications.application.port.out.FcmOutboxPort;
import com.mycomplex.notifications.domain.FcmOutboxEvent;
import com.mycomplex.notifications.domain.NotificationEventType;

@Component
public class FcmOutboxPersistenceAdapter implements FcmOutboxPort {

    private final JdbcTemplate jdbcTemplate;

    public FcmOutboxPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void enqueue(
            UUID taskId,
            UUID targetUserId,
            UUID membershipId,
            NotificationEventType eventType
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO eventos_notificacion_fcm (
                    id, tarea_id, usuario_objetivo_id,
                    vinculacion_asignada_id, tipo_evento, estado,
                    intentos, proximo_intento_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, 'PENDIENTE', 0,
                          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                          CURRENT_TIMESTAMP)
                ON CONFLICT (tarea_id, tipo_evento, vinculacion_asignada_id)
                DO NOTHING
                """,
                UUID.randomUUID(),
                taskId,
                targetUserId,
                membershipId,
                toDatabaseEventType(eventType)
        );
    }

    @Override
    public Map<UUID, NotificationDeliveryStatus> findLatestStatuses(
            Collection<UUID> taskIds
    ) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = taskIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));
        List<Object> parameters = taskIds.stream()
                .map(id -> (Object) id)
                .toList();
        Map<UUID, NotificationDeliveryStatus> result = new HashMap<>();
        jdbcTemplate.query(
                """
                SELECT DISTINCT ON (tarea_id) tarea_id, estado
                FROM eventos_notificacion_fcm
                WHERE tarea_id IN (%s)
                ORDER BY tarea_id, created_at DESC
                """.formatted(placeholders),
                (org.springframework.jdbc.core.RowCallbackHandler)
                resultSet -> result.put(
                        resultSet.getObject("tarea_id", UUID.class),
                        fromDatabaseStatus(resultSet.getString("estado"))
                ),
                parameters.toArray()
        );
        return Map.copyOf(result);
    }

    @Override
    public Optional<FcmOutboxEvent> claimNext() {
        return jdbcTemplate.query(
                """
                SELECT id, tarea_id, usuario_objetivo_id,
                       vinculacion_asignada_id, tipo_evento, intentos,
                       proximo_intento_at
                FROM eventos_notificacion_fcm
                WHERE estado = 'PENDIENTE'
                  AND proximo_intento_at <= CURRENT_TIMESTAMP
                ORDER BY proximo_intento_at, created_at
                LIMIT 1
                FOR UPDATE SKIP LOCKED
                """,
                (resultSet, rowNumber) -> new FcmOutboxEvent(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("tarea_id", UUID.class),
                        resultSet.getObject("usuario_objetivo_id", UUID.class),
                        resultSet.getObject(
                                "vinculacion_asignada_id", UUID.class),
                        fromDatabaseEventType(
                                resultSet.getString("tipo_evento")),
                        resultSet.getInt("intentos"),
                        resultSet.getObject(
                                "proximo_intento_at", OffsetDateTime.class)
                )
        ).stream().findFirst();
    }

    @Override
    public void markSent(UUID eventId, String externalMessageId) {
        jdbcTemplate.update(
                """
                UPDATE eventos_notificacion_fcm
                SET estado = 'ENVIADA', intentos = intentos + 1,
                    enviado_at = CURRENT_TIMESTAMP,
                    mensaje_externo_id = ?, ultimo_error = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND estado = 'PENDIENTE'
                """,
                limit(externalMessageId, 255), eventId
        );
    }

    @Override
    public void markNoDestination(UUID eventId, String safeReason) {
        terminal(eventId, "SIN_DESTINO", safeReason);
    }

    @Override
    public void markFailed(UUID eventId, String safeReason) {
        terminal(eventId, "FALLIDA", safeReason);
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
                UPDATE eventos_notificacion_fcm
                SET intentos = ?, proximo_intento_at = ?, ultimo_error = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND estado = 'PENDIENTE'
                """,
                attempts,
                Timestamp.from(nextAttempt.toInstant()),
                limit(safeReason, 120),
                eventId
        );
    }

    private void terminal(UUID eventId, String status, String reason) {
        jdbcTemplate.update(
                """
                UPDATE eventos_notificacion_fcm
                SET estado = ?, intentos = intentos + 1,
                    ultimo_error = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND estado = 'PENDIENTE'
                """,
                status,
                limit(reason, 120),
                eventId
        );
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String safe = value.strip();
        return safe.length() <= maxLength
                ? safe : safe.substring(0, maxLength);
    }

    private String toDatabaseEventType(NotificationEventType value) {
        return switch (value) {
            case TASK_ASSIGNED -> "TAREA_ASIGNADA";
            case TASK_REASSIGNED -> "TAREA_REASIGNADA";
        };
    }

    private NotificationEventType fromDatabaseEventType(String value) {
        return switch (value) {
            case "TAREA_ASIGNADA" -> NotificationEventType.TASK_ASSIGNED;
            case "TAREA_REASIGNADA" -> NotificationEventType.TASK_REASSIGNED;
            default -> throw new IllegalArgumentException(
                    "Unsupported notification event type"
            );
        };
    }

    private NotificationDeliveryStatus fromDatabaseStatus(String value) {
        return switch (value) {
            case "PENDIENTE" -> NotificationDeliveryStatus.PENDING;
            case "ENVIADA" -> NotificationDeliveryStatus.SENT;
            case "FALLIDA" -> NotificationDeliveryStatus.FAILED;
            case "SIN_DESTINO" -> NotificationDeliveryStatus.NO_DESTINATION;
            default -> throw new IllegalArgumentException(
                    "Unsupported notification delivery status"
            );
        };
    }
}
