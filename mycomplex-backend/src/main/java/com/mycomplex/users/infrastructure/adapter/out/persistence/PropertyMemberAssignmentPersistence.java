package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.mycomplex.users.domain.model.PropertyMemberUpdate;
import com.mycomplex.users.domain.model.ResidentialAssignmentUpdate;
import com.mycomplex.users.domain.model.ResidentialRelationshipType;

/** Synchronizes users-owned membership-to-unit associations. */
@Component
class PropertyMemberAssignmentPersistence {

    private final JdbcTemplate jdbcTemplate;

    PropertyMemberAssignmentPersistence(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void synchronize(PropertyMemberUpdate update) {
        List<AssignmentSnapshot> existing = jdbcTemplate.query(
                """
                SELECT id, unidad_residencial_id, tipo_vinculacion
                FROM usuarios_copropiedades_unidades
                WHERE copropiedad_id = ?
                  AND usuario_copropiedad_id = ?
                  AND estado = 'ACTIVA'
                  AND deleted_at IS NULL
                FOR UPDATE
                """,
                this::mapAssignment,
                update.propertyId(),
                update.membershipId()
        );

        jdbcTemplate.update(
                """
                UPDATE usuarios_copropiedades_unidades
                SET es_principal = FALSE,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE copropiedad_id = ?
                  AND usuario_copropiedad_id = ?
                  AND estado = 'ACTIVA'
                  AND deleted_at IS NULL
                  AND es_principal = TRUE
                """,
                update.propertyId(),
                update.membershipId()
        );

        Map<UUID, ResidentialAssignmentUpdate> requestedByUnit =
                new HashMap<>();
        update.units().forEach(assignment ->
                requestedByUnit.put(assignment.unitId(), assignment)
        );
        Set<UUID> retainedUnits = new HashSet<>();

        for (AssignmentSnapshot assignment : existing) {
            ResidentialAssignmentUpdate requested =
                    requestedByUnit.get(assignment.unitId());
            if (requested != null
                    && assignment.relationshipType().equals(
                            databaseRelationship(requested.relationshipType())
                    )
                    && retainedUnits.add(assignment.unitId())) {
                if (requested.primary()) markPrimary(assignment.assignmentId());
                continue;
            }
            finalizeAssignment(assignment.assignmentId());
        }

        for (ResidentialAssignmentUpdate requested : update.units()) {
            if (!retainedUnits.contains(requested.unitId())) {
                insertAssignment(update, requested);
            }
        }
    }

    private AssignmentSnapshot mapAssignment(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        return new AssignmentSnapshot(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("unidad_residencial_id", UUID.class),
                resultSet.getString("tipo_vinculacion")
        );
    }

    private void markPrimary(UUID assignmentId) {
        jdbcTemplate.update(
                """
                UPDATE usuarios_copropiedades_unidades
                SET es_principal = TRUE, updated_at = CURRENT_TIMESTAMP,
                    version = version + 1 WHERE id = ?
                """,
                assignmentId
        );
    }

    private void finalizeAssignment(UUID assignmentId) {
        jdbcTemplate.update(
                """
                UPDATE usuarios_copropiedades_unidades
                SET estado = 'FINALIZADA', es_principal = FALSE,
                    fecha_fin = GREATEST(fecha_inicio, CURRENT_DATE),
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = ?
                """,
                assignmentId
        );
    }

    private void insertAssignment(
            PropertyMemberUpdate update,
            ResidentialAssignmentUpdate assignment
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO usuarios_copropiedades_unidades (
                    id, copropiedad_id, usuario_copropiedad_id,
                    unidad_residencial_id, tipo_vinculacion,
                    es_principal, estado, fecha_inicio
                ) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVA', CURRENT_DATE)
                """,
                UUID.randomUUID(), update.propertyId(), update.membershipId(),
                assignment.unitId(),
                databaseRelationship(assignment.relationshipType()),
                assignment.primary()
        );
    }

    private String databaseRelationship(ResidentialRelationshipType type) {
        return switch (type) {
            case OWNER -> "PROPIETARIO";
            case RESIDENT -> "RESIDENTE";
        };
    }

    private record AssignmentSnapshot(
            UUID assignmentId,
            UUID unitId,
            String relationshipType
    ) {
    }
}
