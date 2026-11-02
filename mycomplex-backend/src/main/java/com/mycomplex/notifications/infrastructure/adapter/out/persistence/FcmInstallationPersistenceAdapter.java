package com.mycomplex.notifications.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.notifications.application.exception.FcmInstallationException;
import com.mycomplex.notifications.application.port.out.FcmInstallationPort;
import com.mycomplex.notifications.domain.FcmInstallation;

@Component
public class FcmInstallationPersistenceAdapter
        implements FcmInstallationPort {

    private final JdbcTemplate jdbcTemplate;

    public FcmInstallationPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public FcmInstallation register(
            UUID userId,
            String installationIdentifier
    ) {
        List<ExistingInstallation> existing = jdbcTemplate.query(
                """
                SELECT id, usuario_id
                FROM instalaciones_fcm
                WHERE identificador_instalacion = ?
                  AND estado = 'ACTIVA'
                FOR UPDATE
                """,
                (resultSet, rowNumber) -> new ExistingInstallation(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("usuario_id", UUID.class)
                ),
                installationIdentifier
        );
        if (!existing.isEmpty()) {
            ExistingInstallation current = existing.getFirst();
            if (!current.userId().equals(userId)) {
                throw conflict();
            }
            jdbcTemplate.update(
                    """
                    UPDATE instalaciones_fcm
                    SET ultimo_uso_at = CURRENT_TIMESTAMP,
                        updated_at = CURRENT_TIMESTAMP,
                        version = version + 1
                    WHERE id = ?
                    """,
                    current.id()
            );
            return new FcmInstallation(current.id(), userId);
        }

        UUID id = UUID.randomUUID();
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO instalaciones_fcm (
                        id, usuario_id, identificador_instalacion,
                        plataforma, estado, ultimo_uso_at,
                        created_at, updated_at, version
                    ) VALUES (?, ?, ?, 'WEB', 'ACTIVA', ?, ?, ?, 0)
                    """,
                    id, userId, installationIdentifier,
                    OffsetDateTime.now(ZoneOffset.UTC),
                    OffsetDateTime.now(ZoneOffset.UTC),
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
        } catch (DataIntegrityViolationException exception) {
            throw conflict();
        }
        return new FcmInstallation(id, userId);
    }

    @Override
    @Transactional
    public boolean revoke(UUID userId, UUID installationId) {
        return jdbcTemplate.update(
                """
                UPDATE instalaciones_fcm
                SET estado = 'REVOCADA', revocada_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = ? AND usuario_id = ? AND estado = 'ACTIVA'
                """,
                installationId,
                userId
        ) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Destination> findMostRecentActive(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id, identificador_instalacion
                FROM instalaciones_fcm
                WHERE usuario_id = ? AND estado = 'ACTIVA'
                ORDER BY ultimo_uso_at DESC
                LIMIT 1
                """,
                (resultSet, rowNumber) -> new Destination(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("identificador_instalacion")
                ),
                userId
        ).stream().findFirst();
    }

    @Override
    @Transactional
    public void revokeByIdentifier(String installationIdentifier) {
        jdbcTemplate.update(
                """
                UPDATE instalaciones_fcm
                SET estado = 'REVOCADA', revocada_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE identificador_instalacion = ? AND estado = 'ACTIVA'
                """,
                installationIdentifier
        );
    }

    private FcmInstallationException conflict() {
        return new FcmInstallationException(
                "FCM_INSTALLATION_CONFLICT",
                "La instalación requiere volver a registrarse."
        );
    }

    private record ExistingInstallation(UUID id, UUID userId) { }
}
