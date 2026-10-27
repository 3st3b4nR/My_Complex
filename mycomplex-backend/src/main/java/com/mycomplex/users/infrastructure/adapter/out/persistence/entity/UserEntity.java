package com.mycomplex.users.infrastructure.adapter.out.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "usuarios")
public class UserEntity {

    @Id
    private UUID id;

    @Column(name = "auth0_subject", unique = true)
    private String auth0Subject;

    @Column(name = "correo", nullable = false)
    private String email;

    @Column(name = "nombres", nullable = false)
    private String firstNames;

    @Column(name = "apellidos", nullable = false)
    private String lastNames;

    @Column(name = "tipo_documento")
    private String documentType;

    @Column(name = "numero_documento")
    private String documentNumber;

    @Column(name = "es_admin_sistema", nullable = false)
    private boolean systemAdministrator;

    @Column(name = "estado", nullable = false)
    private String status;

    @Column(name = "estado_registro", nullable = false)
    private String registrationStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected UserEntity() {
    }
}
