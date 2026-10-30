package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.exception.InvalidPropertyMemberRegistrationException;
import com.mycomplex.users.application.exception.LastPropertyAdministratorException;
import com.mycomplex.users.application.exception.PropertyMemberBusinessConflictException;
import com.mycomplex.users.application.exception.PropertyMemberConflictException;
import com.mycomplex.users.application.exception.PropertyMemberNotFoundException;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.application.port.out.ManagePropertyMembersPort;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberCandidate;
import com.mycomplex.users.domain.model.PropertyMemberCandidateState;
import com.mycomplex.users.domain.model.PropertyMemberRegistration;
import com.mycomplex.users.domain.model.PropertyMemberRegistrationMode;
import com.mycomplex.users.domain.model.PropertyMemberUnlink;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.domain.model.ResidentialAssignmentUpdate;
import com.mycomplex.users.domain.model.ResidentialRelationshipType;
import com.mycomplex.users.domain.model.UserRegistrationStatus;

@Component
public class PropertyMemberManagementPersistenceAdapter
        implements ManagePropertyMembersPort {

    private final JdbcTemplate jdbcTemplate;
    private final LoadPropertyMembersPort loadMembersPort;

    public PropertyMemberManagementPersistenceAdapter(
            JdbcTemplate jdbcTemplate,
            LoadPropertyMembersPort loadMembersPort
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.loadMembersPort = loadMembersPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyMemberCandidate findCandidate(
            UUID propertyId,
            String email
    ) {
        List<CandidateRow> rows = jdbcTemplate.query(
                """
                SELECT usuario.id AS usuario_id,
                       usuario.nombres,
                       usuario.apellidos,
                       usuario.correo,
                       usuario.estado_registro,
                       usuario.deleted_at AS usuario_deleted_at,
                       membresia.id AS membresia_id,
                       membresia.deleted_at AS membresia_deleted_at
                FROM usuarios usuario
                LEFT JOIN usuarios_copropiedades membresia
                  ON membresia.usuario_id = usuario.id
                 AND membresia.copropiedad_id = ?
                WHERE LOWER(usuario.correo) = ?
                ORDER BY usuario.deleted_at NULLS FIRST,
                         usuario.created_at
                LIMIT 1
                """,
                (resultSet, rowNumber) -> new CandidateRow(
                        resultSet.getObject("usuario_id", UUID.class),
                        resultSet.getString("nombres"),
                        resultSet.getString("apellidos"),
                        resultSet.getString("correo"),
                        resultSet.getString("estado_registro"),
                        resultSet.getObject("usuario_deleted_at") != null,
                        resultSet.getObject("membresia_id", UUID.class),
                        resultSet.getObject("membresia_deleted_at") != null
                ),
                propertyId,
                email
        );
        if (rows.isEmpty()) {
            return new PropertyMemberCandidate(
                    PropertyMemberCandidateState.NEW_USER,
                    null, null, null, null, email, null, List.of()
            );
        }
        CandidateRow row = rows.getFirst();
        PropertyMemberCandidateState state;
        if (row.userDeleted()) {
            state = PropertyMemberCandidateState.DELETED_USER;
        } else if (row.membershipId() == null) {
            state = PropertyMemberCandidateState.AVAILABLE_EXISTING_USER;
        } else if (row.membershipDeleted()) {
            state = PropertyMemberCandidateState.DELETED_MEMBERSHIP;
        } else {
            state = PropertyMemberCandidateState.EXISTING_MEMBERSHIP;
        }
        return new PropertyMemberCandidate(
                state, row.userId(), row.membershipId(),
                row.firstNames(), row.lastNames(), row.email(),
                mapRegistrationStatus(row.registrationStatus()), List.of()
        );
    }

    @Override
    @Transactional
    public PropertyMember register(
            PropertyMemberRegistration registration,
            String auth0Subject
    ) {
        lockActiveProperty(registration.propertyId());
        verifyAdministrator(registration.propertyId(), auth0Subject);
        validateUnits(registration.propertyId(), registration.units());
        try {
            UUID membershipId = switch (registration.mode()) {
                case CREATE_PENDING_USER -> createPendingUser(registration);
                case LINK_EXISTING_USER -> linkExistingUser(registration);
                case REACTIVATE_MEMBERSHIP -> reactivateMembership(registration);
            };
            insertAssignments(registration.propertyId(), membershipId,
                    registration.units());
            return loadMembersPort.loadByMembershipId(
                    registration.propertyId(), membershipId
            ).orElseThrow(PropertyMemberNotFoundException::new);
        } catch (DataIntegrityViolationException exception) {
            throw new PropertyMemberBusinessConflictException(
                    "DUPLICATE_USER_DATA",
                    "El correo, documento o la vinculación ya se encuentra registrado."
            );
        }
    }

    @Override
    @Transactional
    public void unlink(PropertyMemberUnlink unlink, String auth0Subject) {
        lockActiveProperty(unlink.propertyId());
        verifyAdministrator(unlink.propertyId(), auth0Subject);
        MembershipRow membership = lockActiveMembership(
                unlink.propertyId(), unlink.membershipId()
        );
        if (membership.version() != unlink.version()) {
            throw new PropertyMemberConflictException();
        }
        if (membership.role().equals("ADMIN_COPROPIEDAD")
                && membership.status().equals("ACTIVA")) {
            Integer administrators = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM usuarios_copropiedades membresia
                    JOIN usuarios usuario ON usuario.id = membresia.usuario_id
                    WHERE membresia.copropiedad_id = ?
                      AND membresia.rol = 'ADMIN_COPROPIEDAD'
                      AND membresia.estado = 'ACTIVA'
                      AND membresia.deleted_at IS NULL
                      AND usuario.estado = 'ACTIVO'
                      AND usuario.deleted_at IS NULL
                    """,
                    Integer.class,
                    unlink.propertyId()
            );
            if (administrators == null || administrators <= 1) {
                throw new LastPropertyAdministratorException();
            }
        }
        jdbcTemplate.update(
                """
                UPDATE usuarios_copropiedades_unidades
                SET estado = 'FINALIZADA', es_principal = FALSE,
                    fecha_fin = GREATEST(fecha_inicio, CURRENT_DATE),
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE copropiedad_id = ?
                  AND usuario_copropiedad_id = ?
                  AND estado = 'ACTIVA' AND deleted_at IS NULL
                """,
                unlink.propertyId(), unlink.membershipId()
        );
        int affected = jdbcTemplate.update(
                """
                UPDATE usuarios_copropiedades
                SET estado = 'INACTIVA', deleted_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = ? AND copropiedad_id = ?
                  AND version = ? AND deleted_at IS NULL
                """,
                unlink.membershipId(), unlink.propertyId(), unlink.version()
        );
        if (affected != 1) {
            throw new PropertyMemberConflictException();
        }
    }

    private UUID createPendingUser(PropertyMemberRegistration registration) {
        PropertyMemberCandidate candidate = findCandidate(
                registration.propertyId(), registration.email()
        );
        if (candidate.state() != PropertyMemberCandidateState.NEW_USER) {
            throw conflictFor(candidate.state());
        }
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO usuarios (
                    id, auth0_subject, correo, nombres, apellidos,
                    tipo_documento, numero_documento,
                    es_admin_sistema, estado, estado_registro
                ) VALUES (?, NULL, ?, ?, ?, ?, ?, FALSE, 'ACTIVO', 'PENDIENTE')
                """,
                userId, registration.email(), registration.firstNames(),
                registration.lastNames(), registration.documentType(),
                registration.documentNumber()
        );
        return insertMembership(userId, registration);
    }

    private UUID linkExistingUser(PropertyMemberRegistration registration) {
        CandidateUser user = lockUser(registration.userId());
        if (user.deleted()) {
            throw conflictFor(PropertyMemberCandidateState.DELETED_USER);
        }
        if (!user.email().equalsIgnoreCase(registration.email())) {
            throw new InvalidPropertyMemberRegistrationException(
                    "El usuario seleccionado no corresponde al correo verificado."
            );
        }
        PropertyMemberCandidate candidate = findCandidate(
                registration.propertyId(), registration.email()
        );
        if (candidate.state()
                != PropertyMemberCandidateState.AVAILABLE_EXISTING_USER
                || !registration.userId().equals(candidate.userId())) {
            throw conflictFor(candidate.state());
        }
        return insertMembership(registration.userId(), registration);
    }

    private UUID reactivateMembership(PropertyMemberRegistration registration) {
        try {
            UUID userId = jdbcTemplate.queryForObject(
                    """
                    SELECT usuario_id
                    FROM usuarios_copropiedades
                    WHERE id = ? AND copropiedad_id = ?
                      AND deleted_at IS NOT NULL
                    FOR UPDATE
                    """,
                    UUID.class,
                    registration.membershipId(), registration.propertyId()
            );
            CandidateUser user = lockUser(userId);
            if (user.deleted() || !user.email().equalsIgnoreCase(
                    registration.email()
            )) {
                throw conflictFor(PropertyMemberCandidateState.DELETED_USER);
            }
            jdbcTemplate.update(
                    """
                    UPDATE usuarios_copropiedades
                    SET rol = ?, estado = ?, deleted_at = NULL,
                        updated_at = CURRENT_TIMESTAMP, version = version + 1
                    WHERE id = ? AND copropiedad_id = ?
                    """,
                    databaseRole(registration.role()),
                    databaseMembershipStatus(registration.membershipStatus()),
                    registration.membershipId(), registration.propertyId()
            );
            return registration.membershipId();
        } catch (EmptyResultDataAccessException exception) {
            throw new PropertyMemberBusinessConflictException(
                    "MEMBERSHIP_NOT_REACTIVATABLE",
                    "La vinculación ya no puede reactivarse."
            );
        }
    }

    private UUID insertMembership(
            UUID userId,
            PropertyMemberRegistration registration
    ) {
        UUID membershipId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO usuarios_copropiedades (
                    id, usuario_id, copropiedad_id, rol, estado
                ) VALUES (?, ?, ?, ?, ?)
                """,
                membershipId, userId, registration.propertyId(),
                databaseRole(registration.role()),
                databaseMembershipStatus(registration.membershipStatus())
        );
        return membershipId;
    }

    private void insertAssignments(
            UUID propertyId,
            UUID membershipId,
            List<ResidentialAssignmentUpdate> assignments
    ) {
        for (ResidentialAssignmentUpdate assignment : assignments) {
            jdbcTemplate.update(
                    """
                    INSERT INTO usuarios_copropiedades_unidades (
                        id, copropiedad_id, usuario_copropiedad_id,
                        unidad_residencial_id, tipo_vinculacion,
                        es_principal, estado, fecha_inicio
                    ) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVA', CURRENT_DATE)
                    """,
                    UUID.randomUUID(), propertyId, membershipId,
                    assignment.unitId(),
                    databaseRelationship(assignment.relationshipType()),
                    assignment.primary()
            );
        }
    }

    private void validateUnits(
            UUID propertyId,
            List<ResidentialAssignmentUpdate> units
    ) {
        if (units.isEmpty()) {
            return;
        }
        List<Object> parameters = new ArrayList<>();
        units.forEach(unit -> parameters.add(unit.unitId()));
        parameters.add(propertyId);
        String placeholders = String.join(", ",
                java.util.Collections.nCopies(units.size(), "?"));
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM unidades_residenciales unidad
                LEFT JOIN estructuras_copropiedad estructura
                  ON estructura.id = unidad.estructura_id
                 AND estructura.copropiedad_id = unidad.copropiedad_id
                 AND estructura.estado = 'ACTIVA'
                 AND estructura.deleted_at IS NULL
                WHERE unidad.id IN (%s) AND unidad.copropiedad_id = ?
                  AND unidad.estado = 'ACTIVA' AND unidad.deleted_at IS NULL
                  AND (unidad.estructura_id IS NULL OR estructura.id IS NOT NULL)
                """.formatted(placeholders),
                Integer.class,
                parameters.toArray()
        );
        if (count == null || count != units.size()) {
            throw new InvalidPropertyMemberRegistrationException(
                    "Todas las unidades deben estar activas y pertenecer a la copropiedad."
            );
        }
    }

    private void lockActiveProperty(UUID propertyId) {
        try {
            jdbcTemplate.queryForObject(
                    "SELECT id FROM copropiedades WHERE id = ? AND estado = 'ACTIVA' AND deleted_at IS NULL FOR UPDATE",
                    UUID.class, propertyId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new PropertyMembersAccessDeniedException();
        }
    }

    private void verifyAdministrator(UUID propertyId, String auth0Subject) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM usuarios_copropiedades membresia
                JOIN usuarios usuario ON usuario.id = membresia.usuario_id
                WHERE membresia.copropiedad_id = ?
                  AND usuario.auth0_subject = ?
                  AND usuario.estado = 'ACTIVO' AND usuario.deleted_at IS NULL
                  AND membresia.rol = 'ADMIN_COPROPIEDAD'
                  AND membresia.estado = 'ACTIVA'
                  AND membresia.deleted_at IS NULL
                """,
                Integer.class, propertyId, auth0Subject
        );
        if (count == null || count == 0) {
            throw new PropertyMembersAccessDeniedException();
        }
    }

    private MembershipRow lockActiveMembership(UUID propertyId, UUID id) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT rol, estado, version FROM usuarios_copropiedades
                    WHERE id = ? AND copropiedad_id = ? AND deleted_at IS NULL
                    FOR UPDATE
                    """,
                    (rs, row) -> new MembershipRow(
                            rs.getString("rol"), rs.getString("estado"),
                            rs.getLong("version")
                    ), id, propertyId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new PropertyMemberNotFoundException();
        }
    }

    private CandidateUser lockUser(UUID userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT correo, deleted_at FROM usuarios WHERE id = ? FOR UPDATE",
                    (rs, row) -> new CandidateUser(
                            rs.getString("correo"),
                            rs.getObject("deleted_at") != null
                    ), userId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new PropertyMemberBusinessConflictException(
                    "USER_NOT_AVAILABLE",
                    "El usuario seleccionado ya no está disponible."
            );
        }
    }

    private PropertyMemberBusinessConflictException conflictFor(
            PropertyMemberCandidateState state
    ) {
        return switch (state) {
            case EXISTING_MEMBERSHIP -> new PropertyMemberBusinessConflictException(
                    "MEMBERSHIP_ALREADY_EXISTS",
                    "El usuario ya tiene una vinculación con esta copropiedad."
            );
            case DELETED_MEMBERSHIP -> new PropertyMemberBusinessConflictException(
                    "MEMBERSHIP_REQUIRES_REACTIVATION",
                    "La vinculación anterior debe reactivarse explícitamente."
            );
            case DELETED_USER -> new PropertyMemberBusinessConflictException(
                    "DELETED_USER_REQUIRES_INTERVENTION",
                    "El usuario fue eliminado y requiere intervención de un administrador del sistema."
            );
            default -> new PropertyMemberBusinessConflictException(
                    "REGISTRATION_MODE_CONFLICT",
                    "La operación seleccionada no coincide con el estado actual del usuario."
            );
        };
    }

    private UserRegistrationStatus mapRegistrationStatus(String value) {
        return switch (value) {
            case "PENDIENTE" -> UserRegistrationStatus.PENDING;
            case "APROVISIONANDO" -> UserRegistrationStatus.PROVISIONING;
            case "PROVISIONADO" -> UserRegistrationStatus.PROVISIONED;
            case "INVITADO" -> UserRegistrationStatus.INVITED;
            case "VINCULADO" -> UserRegistrationStatus.LINKED;
            default -> throw new IllegalArgumentException(
                    "Unsupported registration status: " + value
            );
        };
    }

    private String databaseRole(PropertyRole role) {
        return switch (role) {
            case PROPERTY_ADMIN -> "ADMIN_COPROPIEDAD";
            case OWNER -> "PROPIETARIO";
            case RESIDENT -> "RESIDENTE";
            case SECURITY_GUARD -> "VIGILANTE";
            case WORKER -> "TRABAJADOR";
        };
    }

    private String databaseMembershipStatus(
            com.mycomplex.users.domain.model.MembershipStatus status
    ) {
        return switch (status) {
            case ACTIVE -> "ACTIVA";
            case INACTIVE -> "INACTIVA";
            case SUSPENDED -> "SUSPENDIDA";
        };
    }

    private String databaseRelationship(ResidentialRelationshipType type) {
        return switch (type) {
            case OWNER -> "PROPIETARIO";
            case RESIDENT -> "RESIDENTE";
        };
    }

    private record CandidateRow(
            UUID userId, String firstNames, String lastNames, String email,
            String registrationStatus, boolean userDeleted,
            UUID membershipId, boolean membershipDeleted
    ) {
    }

    private record CandidateUser(String email, boolean deleted) {
    }

    private record MembershipRow(String role, String status, long version) {
    }
}
