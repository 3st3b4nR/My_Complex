package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.mycomplex.users.domain.model.PropertyMemberCandidate;
import com.mycomplex.users.domain.model.PropertyMemberCandidateState;
import com.mycomplex.users.domain.model.UserRegistrationStatus;

/** Reads candidate state without mutating membership lifecycle. */
@Component
class PropertyMemberCandidatePersistence {

    private final JdbcTemplate jdbcTemplate;

    PropertyMemberCandidatePersistence(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    PropertyMemberCandidate find(UUID propertyId, String email) {
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

    private record CandidateRow(
            UUID userId, String firstNames, String lastNames, String email,
            String registrationStatus, boolean userDeleted,
            UUID membershipId, boolean membershipDeleted
    ) {
    }
}
