CREATE TABLE copropiedades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    nombre VARCHAR(150) NOT NULL,
    nit VARCHAR(30) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    ciudad VARCHAR(100) NOT NULL,
    correo_contacto VARCHAR(254),
    telefono_contacto VARCHAR(30),

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_copropiedades_nombre
        CHECK (BTRIM(nombre) <> ''),

    CONSTRAINT chk_copropiedades_nit
        CHECK (BTRIM(nit) <> ''),

    CONSTRAINT chk_copropiedades_estado
        CHECK (estado IN ('ACTIVA', 'INACTIVA')),

    CONSTRAINT chk_copropiedades_version
        CHECK (version >= 0)
);


CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    auth0_subject VARCHAR(255) NOT NULL,
    correo VARCHAR(254) NOT NULL,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,

    tipo_documento VARCHAR(20),
    numero_documento VARCHAR(50),

    es_admin_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_usuarios_auth0_subject
        UNIQUE (auth0_subject),

    CONSTRAINT chk_usuarios_correo
        CHECK (BTRIM(correo) <> ''),

    CONSTRAINT chk_usuarios_nombres
        CHECK (BTRIM(nombres) <> ''),

    CONSTRAINT chk_usuarios_apellidos
        CHECK (BTRIM(apellidos) <> ''),

    CONSTRAINT chk_usuarios_documento
        CHECK (
            (tipo_documento IS NULL AND numero_documento IS NULL)
            OR
            (
                tipo_documento IS NOT NULL
                AND numero_documento IS NOT NULL
                AND BTRIM(tipo_documento) <> ''
                AND BTRIM(numero_documento) <> ''
            )
        ),

    CONSTRAINT chk_usuarios_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO')),

    CONSTRAINT chk_usuarios_version
        CHECK (version >= 0)
);


CREATE TABLE usuarios_copropiedades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    usuario_id UUID NOT NULL,
    copropiedad_id UUID NOT NULL,

    rol VARCHAR(30) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_usuarios_copropiedades_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_usuarios_copropiedades_copropiedad
        FOREIGN KEY (copropiedad_id)
        REFERENCES copropiedades (id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_usuario_copropiedad
        UNIQUE (usuario_id, copropiedad_id),

    CONSTRAINT chk_usuarios_copropiedades_rol
        CHECK (
            rol IN (
                'ADMIN_COPROPIEDAD',
                'PROPIETARIO',
                'RESIDENTE',
                'VIGILANTE',
                'TRABAJADOR'
            )
        ),

    CONSTRAINT chk_usuarios_copropiedades_estado
        CHECK (estado IN ('ACTIVA', 'INACTIVA', 'SUSPENDIDA')),

    CONSTRAINT chk_usuarios_copropiedades_version
        CHECK (version >= 0)
);


CREATE UNIQUE INDEX uq_copropiedades_nit_activo
    ON copropiedades (LOWER(nit))
    WHERE deleted_at IS NULL;


CREATE UNIQUE INDEX uq_usuarios_correo_activo
    ON usuarios (LOWER(correo))
    WHERE deleted_at IS NULL;


CREATE UNIQUE INDEX uq_usuarios_documento_activo
    ON usuarios (tipo_documento, numero_documento)
    WHERE deleted_at IS NULL
      AND tipo_documento IS NOT NULL
      AND numero_documento IS NOT NULL;


CREATE INDEX idx_usuarios_copropiedades_usuario
    ON usuarios_copropiedades (usuario_id);


CREATE INDEX idx_usuarios_copropiedades_copropiedad
    ON usuarios_copropiedades (copropiedad_id);


CREATE INDEX idx_usuarios_copropiedades_consulta_activa
    ON usuarios_copropiedades (copropiedad_id, usuario_id)
    WHERE deleted_at IS NULL
      AND estado = 'ACTIVA';