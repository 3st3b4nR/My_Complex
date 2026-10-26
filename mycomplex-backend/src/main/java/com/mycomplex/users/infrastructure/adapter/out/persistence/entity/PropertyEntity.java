package com.mycomplex.users.infrastructure.adapter.out.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "copropiedades")
public class PropertyEntity {

    @Id
    private UUID id;

    @Column(name = "nombre", nullable = false)
    private String name;

    @Column(name = "nit", nullable = false)
    private String taxId;

    @Column(name = "direccion", nullable = false)
    private String address;

    @Column(name = "ciudad", nullable = false)
    private String city;

    @Column(name = "correo_contacto")
    private String contactEmail;

    @Column(name = "telefono_contacto")
    private String contactPhone;

    @Column(name = "estado", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PropertyEntity() {
    }
}