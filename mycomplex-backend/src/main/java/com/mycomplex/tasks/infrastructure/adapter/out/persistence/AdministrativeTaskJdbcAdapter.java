package com.mycomplex.tasks.infrastructure.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.mycomplex.tasks.application.port.out.AdministrativeTaskPort;
import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.CreateAdministrativeTask;
import com.mycomplex.tasks.domain.TaskPriority;
import com.mycomplex.tasks.domain.TaskSearchCriteria;
import com.mycomplex.tasks.domain.TaskStatus;
import com.mycomplex.tasks.domain.UpdateAdministrativeTask;
import com.mycomplex.users.api.TaskAccessDirectory.Administrator;
import com.mycomplex.users.api.TaskAccessDirectory.Worker;

@Component
public class AdministrativeTaskJdbcAdapter
        implements AdministrativeTaskPort {

    private static final String SELECT_TASK = """
            SELECT tarea.id, tarea.copropiedad_id, tarea.titulo,
                   tarea.descripcion, tarea.prioridad, tarea.estado,
                   tarea.ubicacion, tarea.fecha_limite,
                   tarea.observacion_administrativa,
                   tarea.creada_por_usuario_copropiedad_id,
                   tarea.asignada_a_usuario_copropiedad_id,
                   usuario.id AS trabajador_usuario_id,
                   CONCAT_WS(' ', usuario.nombres, usuario.apellidos)
                       AS trabajador_nombre,
                   tarea.iniciada_at, tarea.completada_at,
                   tarea.cancelada_at, tarea.created_at,
                   tarea.updated_at, tarea.version
            FROM tareas tarea
            JOIN usuarios_copropiedades membresia
              ON membresia.id = tarea.asignada_a_usuario_copropiedad_id
            JOIN usuarios usuario ON usuario.id = membresia.usuario_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public AdministrativeTaskJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AdministrativeTask> find(TaskSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder(SELECT_TASK).append("""
                WHERE tarea.copropiedad_id = ?
                  AND tarea.deleted_at IS NULL
                """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(criteria.propertyId());
        if (criteria.status() != null) {
            sql.append(" AND tarea.estado = ?");
            parameters.add(toDatabase(criteria.status()));
        }
        if (criteria.priority() != null) {
            sql.append(" AND tarea.prioridad = ?");
            parameters.add(toDatabase(criteria.priority()));
        }
        if (criteria.assignedMembershipId() != null) {
            sql.append(" AND tarea.asignada_a_usuario_copropiedad_id = ?");
            parameters.add(criteria.assignedMembershipId());
        }
        if (criteria.overdueOnly()) {
            sql.append("""
                     AND tarea.fecha_limite < CURRENT_TIMESTAMP
                     AND tarea.estado NOT IN ('COMPLETADA', 'CANCELADA')
                    """);
        }
        if (criteria.query() != null && !criteria.query().isBlank()) {
            sql.append("""
                     AND (LOWER(tarea.titulo) LIKE ?
                       OR LOWER(tarea.descripcion) LIKE ?
                       OR LOWER(CONCAT_WS(' ', usuario.nombres,
                                                    usuario.apellidos)) LIKE ?)
                    """);
            String query = "%" + criteria.query().strip().toLowerCase() + "%";
            parameters.add(query);
            parameters.add(query);
            parameters.add(query);
        }
        sql.append(" ORDER BY tarea.created_at DESC");
        return jdbcTemplate.query(sql.toString(), this::mapTask,
                parameters.toArray());
    }

    @Override
    public Optional<AdministrativeTask> findOne(
            UUID propertyId,
            UUID taskId
    ) {
        return jdbcTemplate.query(
                SELECT_TASK + """
                 WHERE tarea.copropiedad_id = ? AND tarea.id = ?
                   AND tarea.deleted_at IS NULL
                """,
                this::mapTask, propertyId, taskId
        ).stream().findFirst();
    }

    @Override
    public AdministrativeTask create(
            CreateAdministrativeTask command,
            Administrator administrator,
            Worker worker
    ) {
        UUID taskId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tareas (
                    id, copropiedad_id,
                    creada_por_usuario_copropiedad_id,
                    asignada_a_usuario_copropiedad_id,
                    titulo, descripcion, prioridad, estado, ubicacion,
                    fecha_limite, observacion_administrativa
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDIENTE', ?, ?, ?)
                """,
                taskId, command.propertyId(), administrator.membershipId(),
                worker.membershipId(), command.title().strip(),
                command.description().strip(), toDatabase(command.priority()),
                normalize(command.location()), command.dueAt(),
                normalize(command.administrativeNote())
        );
        return findOne(command.propertyId(), taskId).orElseThrow();
    }

    @Override
    public Optional<AdministrativeTask> update(
            UpdateAdministrativeTask command,
            Worker worker
    ) {
        int changed = jdbcTemplate.update("""
                UPDATE tareas
                SET titulo = ?, descripcion = ?, prioridad = ?,
                    ubicacion = ?, fecha_limite = ?,
                    observacion_administrativa = ?,
                    asignada_a_usuario_copropiedad_id = ?,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = ? AND copropiedad_id = ? AND version = ?
                  AND deleted_at IS NULL
                  AND estado NOT IN ('COMPLETADA', 'CANCELADA')
                """,
                command.title().strip(), command.description().strip(),
                toDatabase(command.priority()), normalize(command.location()),
                command.dueAt(), normalize(command.administrativeNote()),
                worker.membershipId(), command.taskId(), command.propertyId(),
                command.version()
        );
        return changed == 0 ? Optional.empty()
                : findOne(command.propertyId(), command.taskId());
    }

    @Override
    public Optional<AdministrativeTask> changeStatus(
            UUID propertyId,
            UUID taskId,
            TaskStatus status,
            long version
    ) {
        int changed = jdbcTemplate.update("""
                UPDATE tareas
                SET estado = ?,
                    iniciada_at = CASE WHEN ? = 'EN_PROGRESO'
                        THEN CURRENT_TIMESTAMP ELSE iniciada_at END,
                    completada_at = CASE WHEN ? = 'COMPLETADA'
                        THEN CURRENT_TIMESTAMP ELSE NULL END,
                    cancelada_at = CASE WHEN ? = 'CANCELADA'
                        THEN CURRENT_TIMESTAMP ELSE NULL END,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = ? AND copropiedad_id = ? AND version = ?
                  AND deleted_at IS NULL
                """,
                toDatabase(status), toDatabase(status), toDatabase(status),
                toDatabase(status), taskId, propertyId, version
        );
        return changed == 0 ? Optional.empty() : findOne(propertyId, taskId);
    }

    private AdministrativeTask mapTask(ResultSet rs, int row)
            throws SQLException {
        return new AdministrativeTask(
                rs.getObject("id", UUID.class),
                rs.getObject("copropiedad_id", UUID.class),
                rs.getString("titulo"), rs.getString("descripcion"),
                fromPriority(rs.getString("prioridad")),
                fromStatus(rs.getString("estado")),
                rs.getString("ubicacion"),
                rs.getObject("fecha_limite", OffsetDateTime.class),
                rs.getString("observacion_administrativa"),
                rs.getObject("creada_por_usuario_copropiedad_id", UUID.class),
                rs.getObject("asignada_a_usuario_copropiedad_id", UUID.class),
                rs.getObject("trabajador_usuario_id", UUID.class),
                rs.getString("trabajador_nombre"),
                rs.getObject("iniciada_at", OffsetDateTime.class),
                rs.getObject("completada_at", OffsetDateTime.class),
                rs.getObject("cancelada_at", OffsetDateTime.class),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                rs.getLong("version"), null
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String toDatabase(TaskPriority value) {
        return switch (value) {
            case LOW -> "BAJA";
            case MEDIUM -> "MEDIA";
            case HIGH -> "ALTA";
            case URGENT -> "URGENTE";
        };
    }

    private String toDatabase(TaskStatus value) {
        return switch (value) {
            case PENDING -> "PENDIENTE";
            case IN_PROGRESS -> "EN_PROGRESO";
            case COMPLETED -> "COMPLETADA";
            case CANCELLED -> "CANCELADA";
        };
    }

    private TaskPriority fromPriority(String value) {
        return switch (value) {
            case "BAJA" -> TaskPriority.LOW;
            case "MEDIA" -> TaskPriority.MEDIUM;
            case "ALTA" -> TaskPriority.HIGH;
            case "URGENTE" -> TaskPriority.URGENT;
            default -> throw new IllegalArgumentException("Unsupported priority");
        };
    }

    private TaskStatus fromStatus(String value) {
        return switch (value) {
            case "PENDIENTE" -> TaskStatus.PENDING;
            case "EN_PROGRESO" -> TaskStatus.IN_PROGRESS;
            case "COMPLETADA" -> TaskStatus.COMPLETED;
            case "CANCELADA" -> TaskStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unsupported status");
        };
    }
}
