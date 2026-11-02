package com.mycomplex.notifications.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.notifications.application.model.NotificationRecipient;
import com.mycomplex.notifications.application.port.out.LoadNewsNotificationRecipientsPort;

@Component
public class NewsNotificationRecipientsAdapter
        implements LoadNewsNotificationRecipientsPort {

    private final JdbcClient jdbcClient;

    public NewsNotificationRecipientsAdapter(
            JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationRecipient> loadActiveByPropertyId(
            UUID propertyId
    ) {
        return jdbcClient.sql("""
                SELECT DISTINCT usuario.id, usuario.correo
                FROM usuarios usuario
                INNER JOIN usuarios_copropiedades relacion
                    ON relacion.usuario_id = usuario.id
                INNER JOIN copropiedades copropiedad
                    ON copropiedad.id = relacion.copropiedad_id
                WHERE copropiedad.id = :propertyId
                  AND usuario.estado = 'ACTIVO'
                  AND usuario.deleted_at IS NULL
                  AND relacion.estado = 'ACTIVA'
                  AND relacion.deleted_at IS NULL
                  AND copropiedad.estado = 'ACTIVA'
                  AND copropiedad.deleted_at IS NULL
                ORDER BY usuario.id
                """)
                .param("propertyId", propertyId)
                .query((resultSet, rowNumber) ->
                        new NotificationRecipient(
                                resultSet.getObject(
                                        "id",
                                        UUID.class
                                ),
                                resultSet.getString("correo")
                        )
                )
                .list();
    }
}
