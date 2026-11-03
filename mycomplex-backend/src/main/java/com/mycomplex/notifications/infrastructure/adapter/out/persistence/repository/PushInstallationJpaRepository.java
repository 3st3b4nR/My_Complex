package com.mycomplex.notifications.infrastructure.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mycomplex.notifications.infrastructure.adapter.out.persistence.entity.PushInstallationEntity;

public interface PushInstallationJpaRepository
        extends JpaRepository<PushInstallationEntity, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO instalaciones_fcm (
                usuario_id,
                identificador_instalacion,
                plataforma,
                estado,
                ultimo_uso_at,
                created_at,
                updated_at,
                version
            )
            SELECT
                usuario.id,
                :installationId,
                'WEB',
                'ACTIVA',
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                0
            FROM usuarios usuario
            WHERE usuario.auth0_subject = :auth0Subject
              AND usuario.estado = 'ACTIVO'
              AND usuario.deleted_at IS NULL
            ON CONFLICT (identificador_instalacion)
                WHERE estado = 'ACTIVA'
            DO UPDATE SET
                usuario_id = EXCLUDED.usuario_id,
                ultimo_uso_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP,
                version = instalaciones_fcm.version + 1
            """, nativeQuery = true)
    int register(
            @Param("auth0Subject") String auth0Subject,
            @Param("installationId") String installationId
    );

    @Modifying
    @Query(value = """
            UPDATE instalaciones_fcm installation
            SET estado = 'REVOCADA',
                revocada_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP,
                version = installation.version + 1
            FROM usuarios usuario
            WHERE installation.usuario_id = usuario.id
              AND usuario.auth0_subject = :auth0Subject
              AND installation.identificador_instalacion = :installationId
              AND installation.estado = 'ACTIVA'
            """, nativeQuery = true)
    int revoke(
            @Param("auth0Subject") String auth0Subject,
            @Param("installationId") String installationId
    );

    @Query(value = """
            SELECT installation.identificador_instalacion
            FROM instalaciones_fcm installation
            WHERE installation.usuario_id IN (:userIds)
              AND installation.estado = 'ACTIVA'
              AND installation.revocada_at IS NULL
            ORDER BY installation.ultimo_uso_at DESC
            """, nativeQuery = true)
    List<String> findActiveInstallationIds(
            @Param("userIds") Collection<UUID> userIds
    );
}
