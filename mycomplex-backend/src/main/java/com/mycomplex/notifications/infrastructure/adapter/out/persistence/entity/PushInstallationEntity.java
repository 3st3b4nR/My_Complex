package com.mycomplex.notifications.infrastructure.adapter.out.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "instalaciones_fcm")
public class PushInstallationEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID userId;

    @Column(name = "identificador_instalacion", nullable = false)
    private String installationId;

    @Column(name = "plataforma", nullable = false)
    private String platform;

    @Column(name = "estado", nullable = false)
    private String status;

    @Column(name = "ultimo_uso_at", nullable = false)
    private OffsetDateTime lastUsedAt;

    @Column(name = "revocada_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PushInstallationEntity() {
    }
}
