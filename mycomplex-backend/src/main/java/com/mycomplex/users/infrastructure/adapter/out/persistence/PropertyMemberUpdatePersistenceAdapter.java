package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

@Component
public class PropertyMemberUpdatePersistenceAdapter
        implements UpdatePropertyMemberPort {

    private final JdbcTemplate jdbcTemplate;
    private final LoadPropertyMembersPort loadMembersPort;
    private final PropertyMemberAssignmentPersistence assignmentPersistence;

    public PropertyMemberUpdatePersistenceAdapter(
            JdbcTemplate jdbcTemplate,
            LoadPropertyMembersPort loadMembersPort,
            PropertyMemberAssignmentPersistence assignmentPersistence
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.loadMembersPort = loadMembersPort;
        this.assignmentPersistence = assignmentPersistence;
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
        assignmentPersistence.synchronize(update);
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

    private record MembershipSnapshot(
            String role,
            String status,
            long version
    ) {
    }

}
