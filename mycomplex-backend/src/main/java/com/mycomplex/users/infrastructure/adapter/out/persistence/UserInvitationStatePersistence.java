package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.mycomplex.users.application.exception.UserInvitationException;
import com.mycomplex.users.domain.model.PropertyMemberInvitation;
import com.mycomplex.users.domain.model.UserRegistrationStatus;

/** Persists invitation lifecycle outcomes after external Auth0 calls. */
@Component
class UserInvitationStatePersistence {

    private final JdbcTemplate jdbcTemplate;

    UserInvitationStatePersistence(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void markProvisioned(UUID userId, String auth0Subject) {
        int affected = jdbcTemplate.update(
                """
                UPDATE usuarios
                SET auth0_subject = ?, estado_registro = 'PROVISIONADO',
                    auth0_provisioned_at = CURRENT_TIMESTAMP,
                    ultimo_error_registro = NULL,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = ? AND estado_registro = 'APROVISIONANDO'
                """,
                auth0Subject, userId
        );
        if (affected != 1) throw stateConflict(
                "El estado del usuario cambió durante el aprovisionamiento."
        );
    }

    PropertyMemberInvitation markInvited(UUID userId, UUID membershipId) {
        int affected = jdbcTemplate.update(
                """
                UPDATE usuarios
                SET estado_registro = 'INVITADO',
                    invitacion_enviada_at = CURRENT_TIMESTAMP,
                    ultimo_error_registro = NULL,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = ?
                  AND estado_registro IN ('PROVISIONADO', 'INVITADO')
                """,
                userId
        );
        if (affected != 1) throw stateConflict(
                "El estado del usuario cambió durante el envío."
        );
        OffsetDateTime sentAt = jdbcTemplate.queryForObject(
                "SELECT invitacion_enviada_at FROM usuarios WHERE id = ?",
                OffsetDateTime.class,
                userId
        );
        return new PropertyMemberInvitation(
                membershipId, userId, UserRegistrationStatus.INVITED,
                sentAt, "Auth0 aceptó la solicitud de invitación."
        );
    }

    void recordProvisioningFailure(UUID userId, String safeError) {
        jdbcTemplate.update(
                """
                UPDATE usuarios
                SET ultimo_error_registro = ?, updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = ? AND estado_registro = 'APROVISIONANDO'
                """,
                limit(safeError), userId
        );
    }

    void recordInvitationFailure(UUID userId, String safeError) {
        jdbcTemplate.update(
                """
                UPDATE usuarios
                SET estado_registro = 'PROVISIONADO',
                    ultimo_error_registro = ?,
                    ultimo_intento_registro_at = NULL,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = ?
                  AND estado_registro IN ('PROVISIONADO', 'INVITADO')
                """,
                limit(safeError), userId
        );
    }

    private UserInvitationException stateConflict(String message) {
        return new UserInvitationException("INVITATION_STATE_CONFLICT", message);
    }

    private String limit(String value) {
        if (value == null) return null;
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
