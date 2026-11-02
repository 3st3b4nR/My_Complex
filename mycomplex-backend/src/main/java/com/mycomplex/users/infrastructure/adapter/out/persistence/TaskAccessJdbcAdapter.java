package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.api.TaskAccessDirectory;

@Component
public class TaskAccessJdbcAdapter implements TaskAccessDirectory {

    private final JdbcTemplate jdbcTemplate;

    public TaskAccessJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Administrator> findActiveAdministrator(
            UUID propertyId,
            String auth0Subject
    ) {
        if (propertyId == null || auth0Subject == null
                || auth0Subject.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                """
                SELECT usuario.id AS usuario_id, membresia.id AS membresia_id
                FROM usuarios_copropiedades membresia
                JOIN usuarios usuario ON usuario.id = membresia.usuario_id
                JOIN copropiedades copropiedad
                  ON copropiedad.id = membresia.copropiedad_id
                WHERE membresia.copropiedad_id = ?
                  AND usuario.auth0_subject = ?
                  AND usuario.estado = 'ACTIVO'
                  AND usuario.deleted_at IS NULL
                  AND membresia.rol = 'ADMIN_COPROPIEDAD'
                  AND membresia.estado = 'ACTIVA'
                  AND membresia.deleted_at IS NULL
                  AND copropiedad.estado = 'ACTIVA'
                  AND copropiedad.deleted_at IS NULL
                """,
                (resultSet, rowNumber) -> new Administrator(
                        resultSet.getObject("usuario_id", UUID.class),
                        resultSet.getObject("membresia_id", UUID.class)
                ),
                propertyId,
                auth0Subject
        ).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Worker> findEligibleWorker(
            UUID propertyId,
            UUID membershipId
    ) {
        if (propertyId == null || membershipId == null) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                eligibleWorkerSql() + " AND membresia.id = ?",
                this::mapWorker,
                propertyId,
                membershipId
        ).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Worker> findEligibleWorkers(UUID propertyId) {
        if (propertyId == null) {
            return List.of();
        }
        return jdbcTemplate.query(
                eligibleWorkerSql()
                        + " ORDER BY usuario.apellidos, usuario.nombres",
                this::mapWorker,
                propertyId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findActiveUserId(String auth0Subject) {
        if (auth0Subject == null || auth0Subject.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                """
                SELECT id
                FROM usuarios
                WHERE auth0_subject = ?
                  AND estado = 'ACTIVO'
                  AND deleted_at IS NULL
                """,
                (resultSet, rowNumber) ->
                        resultSet.getObject("id", UUID.class),
                auth0Subject
        ).stream().findFirst();
    }

    private String eligibleWorkerSql() {
        return """
                SELECT usuario.id AS usuario_id,
                       membresia.id AS membresia_id,
                       CONCAT_WS(' ', usuario.nombres, usuario.apellidos)
                           AS nombre
                FROM usuarios_copropiedades membresia
                JOIN usuarios usuario ON usuario.id = membresia.usuario_id
                JOIN copropiedades copropiedad
                  ON copropiedad.id = membresia.copropiedad_id
                WHERE membresia.copropiedad_id = ?
                  AND usuario.estado = 'ACTIVO'
                  AND usuario.deleted_at IS NULL
                  AND membresia.rol = 'TRABAJADOR'
                  AND membresia.estado = 'ACTIVA'
                  AND membresia.deleted_at IS NULL
                  AND copropiedad.estado = 'ACTIVA'
                  AND copropiedad.deleted_at IS NULL
                """;
    }

    private Worker mapWorker(
            java.sql.ResultSet resultSet,
            int rowNumber
    ) throws java.sql.SQLException {
        return new Worker(
                resultSet.getObject("usuario_id", UUID.class),
                resultSet.getObject("membresia_id", UUID.class),
                resultSet.getString("nombre")
        );
    }
}
