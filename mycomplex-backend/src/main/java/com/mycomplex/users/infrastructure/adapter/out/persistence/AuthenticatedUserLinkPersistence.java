package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.mycomplex.users.application.exception.UserInvitationException;

/** Performs the idempotent first-authentication state transition. */
@Component
class AuthenticatedUserLinkPersistence {

    private final JdbcTemplate jdbcTemplate;

    AuthenticatedUserLinkPersistence(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void link(String auth0Subject) {
        List<String> states = jdbcTemplate.query(
                """
                SELECT estado_registro FROM usuarios
                WHERE auth0_subject = ? AND deleted_at IS NULL
                """,
                (resultSet, rowNumber) -> resultSet.getString(1),
                auth0Subject
        );
        if (states.isEmpty() || states.getFirst().equals("VINCULADO")) {
            return;
        }
        int affected = jdbcTemplate.update(
                """
                UPDATE usuarios
                SET estado_registro = 'VINCULADO',
                    vinculado_at = CURRENT_TIMESTAMP,
                    ultimo_error_registro = NULL,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE auth0_subject = ? AND deleted_at IS NULL
                  AND estado_registro IN ('PROVISIONADO', 'INVITADO')
                """,
                auth0Subject
        );
        if (affected == 0) {
            throw new UserInvitationException(
                    "AUTHENTICATED_IDENTITY_CONFLICT",
                    "La identidad autenticada no coincide con un estado vinculable."
            );
        }
    }
}
