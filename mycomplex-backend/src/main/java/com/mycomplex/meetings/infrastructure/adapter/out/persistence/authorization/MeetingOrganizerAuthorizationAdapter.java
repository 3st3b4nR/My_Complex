package com.mycomplex.meetings.infrastructure.adapter.out.persistence.authorization;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.meetings.application.port.out.LoadAuthorizedMeetingOrganizerPort;

@Component
public class MeetingOrganizerAuthorizationAdapter
        implements LoadAuthorizedMeetingOrganizerPort {

    private final JdbcClient jdbcClient;

    public MeetingOrganizerAuthorizationAdapter(
            JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findActiveAdministratorId(
            UUID propertyId,
            String auth0Subject
    ) {
        return jdbcClient.sql("""
                SELECT usuario.id
                FROM usuarios usuario
                INNER JOIN usuarios_copropiedades relacion
                    ON relacion.usuario_id = usuario.id
                INNER JOIN copropiedades copropiedad
                    ON copropiedad.id = relacion.copropiedad_id
                WHERE copropiedad.id = :propertyId
                  AND usuario.auth0_subject = :auth0Subject
                  AND usuario.estado = 'ACTIVO'
                  AND usuario.deleted_at IS NULL
                  AND relacion.rol = 'ADMIN_COPROPIEDAD'
                  AND relacion.estado = 'ACTIVA'
                  AND relacion.deleted_at IS NULL
                  AND copropiedad.estado = 'ACTIVA'
                  AND copropiedad.deleted_at IS NULL
                """)
                .param("propertyId", propertyId)
                .param("auth0Subject", auth0Subject)
                .query(UUID.class)
                .optional();
    }
}
