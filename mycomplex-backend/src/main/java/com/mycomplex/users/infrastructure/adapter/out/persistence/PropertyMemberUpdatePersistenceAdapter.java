package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.exception.InvalidPropertyMemberUpdateException;
import com.mycomplex.users.application.exception.LastPropertyAdministratorException;
import com.mycomplex.users.application.exception.PropertyMemberConflictException;
import com.mycomplex.users.application.exception.PropertyMemberNotFoundException;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.application.port.out.UpdatePropertyMemberPort;
import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberUpdate;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.domain.model.ResidentialAssignmentUpdate;
import com.mycomplex.users.domain.model.ResidentialRelationshipType;

@Component
public class PropertyMemberUpdatePersistenceAdapter
        implements UpdatePropertyMemberPort {

    private final JdbcTemplate jdbcTemplate;
    private final LoadPropertyMembersPort loadMembersPort;

    public PropertyMemberUpdatePersistenceAdapter(
            JdbcTemplate jdbcTemplate,
            LoadPropertyMembersPort loadMembersPort
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.loadMembersPort = loadMembersPort;
    }

    @Override
    @Transactional
    public PropertyMember update(
            PropertyMemberUpdate update,
            String auth0Subject
    ) {
        lockActiveProperty(update.propertyId());
        verifyAdministrator(update.propertyId(), auth0Subject);

        MembershipSnapshot membership = lockMembership(
                update.propertyId(),
                update.membershipId()
        );
        if (membership.version() != update.version()) {
            throw new PropertyMemberConflictException();
        }

        protectLastAdministrator(update, membership);
        validateUnitsBelongToProperty(update);
        synchronizeAssignments(update);
        updateMembership(update);

        return loadMembersPort.loadByMembershipId(
                update.propertyId(),
                update.membershipId()
        ).orElseThrow(PropertyMemberNotFoundException::new);
    }

    private void lockActiveProperty(UUID propertyId) {
        try {
            jdbcTemplate.queryForObject(
                    """
                    SELECT id
                    FROM copropiedades
                    WHERE id = ?
                      AND estado = 'ACTIVA'
                      AND deleted_at IS NULL
                    FOR UPDATE
                    """,
                    UUID.class,
                    propertyId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new PropertyMembersAccessDeniedException();
        }
    }

    private void verifyAdministrator(
            UUID propertyId,
            String auth0Subject
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM usuarios_copropiedades membresia
                JOIN usuarios usuario
                  ON usuario.id = membresia.usuario_id
                WHERE membresia.copropiedad_id = ?
                  AND usuario.auth0_subject = ?
                  AND usuario.estado = 'ACTIVO'
                  AND usuario.deleted_at IS NULL
                  AND membresia.rol = 'ADMIN_COPROPIEDAD'
                  AND membresia.estado = 'ACTIVA'
                  AND membresia.deleted_at IS NULL
                """,
                Integer.class,
                propertyId,
                auth0Subject
        );
        if (count == null || count == 0) {
            throw new PropertyMembersAccessDeniedException();
        }
    }

    private MembershipSnapshot lockMembership(
            UUID propertyId,
            UUID membershipId
    ) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT membresia.rol,
                           membresia.estado,
                           membresia.version
                    FROM usuarios_copropiedades membresia
                    JOIN usuarios usuario
                      ON usuario.id = membresia.usuario_id
                    WHERE membresia.id = ?
                      AND membresia.copropiedad_id = ?
                      AND membresia.deleted_at IS NULL
                      AND usuario.deleted_at IS NULL
                    FOR UPDATE OF membresia
                    """,
                    (resultSet, rowNumber) -> new MembershipSnapshot(
                            resultSet.getString("rol"),
                            resultSet.getString("estado"),
                            resultSet.getLong("version")
                    ),
                    membershipId,
                    propertyId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new PropertyMemberNotFoundException();
        }
    }

    private void protectLastAdministrator(
            PropertyMemberUpdate update,
            MembershipSnapshot membership
    ) {
        boolean currentlyActiveAdministrator =
                membership.role().equals("ADMIN_COPROPIEDAD")
                        && membership.status().equals("ACTIVA");
        boolean remainsActiveAdministrator =
                update.role() == PropertyRole.PROPERTY_ADMIN
                        && update.membershipStatus()
                        == MembershipStatus.ACTIVE;

        if (!currentlyActiveAdministrator
                || remainsActiveAdministrator) {
            return;
        }

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM usuarios_copropiedades membresia
                JOIN usuarios usuario
                  ON usuario.id = membresia.usuario_id
                WHERE membresia.copropiedad_id = ?
                  AND membresia.rol = 'ADMIN_COPROPIEDAD'
                  AND membresia.estado = 'ACTIVA'
                  AND membresia.deleted_at IS NULL
                  AND usuario.estado = 'ACTIVO'
                  AND usuario.deleted_at IS NULL
                """,
                Integer.class,
                update.propertyId()
        );
        if (count == null || count <= 1) {
            throw new LastPropertyAdministratorException();
        }
    }

    private void validateUnitsBelongToProperty(
            PropertyMemberUpdate update
    ) {
        if (update.units().isEmpty()) {
            return;
        }

        List<UUID> unitIds = update.units().stream()
                .map(ResidentialAssignmentUpdate::unitId)
                .toList();
        String placeholders = String.join(
                ", ",
                java.util.Collections.nCopies(unitIds.size(), "?")
        );
        List<Object> parameters = new ArrayList<>(unitIds);
        parameters.add(update.propertyId());

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM unidades_residenciales unidad
                LEFT JOIN estructuras_copropiedad estructura
                  ON estructura.id = unidad.estructura_id
                 AND estructura.copropiedad_id = unidad.copropiedad_id
                 AND estructura.estado = 'ACTIVA'
                 AND estructura.deleted_at IS NULL
                WHERE unidad.id IN (%s)
                  AND unidad.copropiedad_id = ?
                  AND unidad.estado = 'ACTIVA'
                  AND unidad.deleted_at IS NULL
                  AND (
                      unidad.estructura_id IS NULL
                      OR estructura.id IS NOT NULL
                  )
                """.formatted(placeholders),
                Integer.class,
                parameters.toArray()
        );
        if (count == null || count != unitIds.size()) {
            throw new InvalidPropertyMemberUpdateException(
                    "Every unit must be active and belong to the property"
            );
        }
    }

    private void synchronizeAssignments(PropertyMemberUpdate update) {
        List<AssignmentSnapshot> existing = jdbcTemplate.query(
                """
                SELECT id,
                       unidad_residencial_id,
                       tipo_vinculacion,
                       es_principal
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
                            toDatabaseRelationshipType(
                                    requested.relationshipType()
                            )
                    )
                    && retainedUnits.add(assignment.unitId())) {
                if (requested.primary()) {
                    markPrimary(assignment.assignmentId());
                }
                continue;
            }
            finalizeAssignment(assignment.assignmentId());
        }

        for (ResidentialAssignmentUpdate requested : update.units()) {
            if (retainedUnits.contains(requested.unitId())) {
                continue;
            }
            insertAssignment(update, requested);
        }
    }

    private AssignmentSnapshot mapAssignment(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        return new AssignmentSnapshot(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject(
                        "unidad_residencial_id",
                        UUID.class
                ),
                resultSet.getString("tipo_vinculacion")
        );
    }

    private void markPrimary(UUID assignmentId) {
        jdbcTemplate.update(
                """
                UPDATE usuarios_copropiedades_unidades
                SET es_principal = TRUE,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = ?
                """,
                assignmentId
        );
    }

    private void finalizeAssignment(UUID assignmentId) {
        jdbcTemplate.update(
                """
                UPDATE usuarios_copropiedades_unidades
                SET estado = 'FINALIZADA',
                    es_principal = FALSE,
                    fecha_fin = GREATEST(fecha_inicio, CURRENT_DATE),
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
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
                    id,
                    copropiedad_id,
                    usuario_copropiedad_id,
                    unidad_residencial_id,
                    tipo_vinculacion,
                    es_principal,
                    estado,
                    fecha_inicio
                ) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVA', CURRENT_DATE)
                """,
                UUID.randomUUID(),
                update.propertyId(),
                update.membershipId(),
                assignment.unitId(),
                toDatabaseRelationshipType(
                        assignment.relationshipType()
                ),
                assignment.primary()
        );
    }

    private void updateMembership(PropertyMemberUpdate update) {
        int affectedRows = jdbcTemplate.update(
                """
                UPDATE usuarios_copropiedades
                SET rol = ?,
                    estado = ?,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = ?
                  AND copropiedad_id = ?
                  AND version = ?
                  AND deleted_at IS NULL
                """,
                toDatabaseRole(update.role()),
                toDatabaseMembershipStatus(
                        update.membershipStatus()
                ),
                update.membershipId(),
                update.propertyId(),
                update.version()
        );
        if (affectedRows != 1) {
            throw new PropertyMemberConflictException();
        }
    }

    private String toDatabaseRole(PropertyRole role) {
        return switch (role) {
            case PROPERTY_ADMIN -> "ADMIN_COPROPIEDAD";
            case OWNER -> "PROPIETARIO";
            case RESIDENT -> "RESIDENTE";
            case SECURITY_GUARD -> "VIGILANTE";
            case WORKER -> "TRABAJADOR";
        };
    }

    private String toDatabaseMembershipStatus(
            MembershipStatus status
    ) {
        return switch (status) {
            case ACTIVE -> "ACTIVA";
            case INACTIVE -> "INACTIVA";
            case SUSPENDED -> "SUSPENDIDA";
        };
    }

    private String toDatabaseRelationshipType(
            ResidentialRelationshipType type
    ) {
        return switch (type) {
            case OWNER -> "PROPIETARIO";
            case RESIDENT -> "RESIDENTE";
        };
    }

    private record MembershipSnapshot(
            String role,
            String status,
            long version
    ) {
    }

    private record AssignmentSnapshot(
            UUID assignmentId,
            UUID unitId,
            String relationshipType
    ) {
    }
}
