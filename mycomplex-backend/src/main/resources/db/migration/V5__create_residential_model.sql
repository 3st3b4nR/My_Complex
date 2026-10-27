ALTER TABLE usuarios_copropiedades
    ADD CONSTRAINT uq_usuarios_copropiedades_id_copropiedad
        UNIQUE (id, copropiedad_id);


CREATE TABLE estructuras_copropiedad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    copropiedad_id UUID NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_estructuras_copropiedad
        FOREIGN KEY (copropiedad_id)
        REFERENCES copropiedades (id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_estructuras_id_copropiedad
        UNIQUE (id, copropiedad_id),

    CONSTRAINT chk_estructuras_nombre
        CHECK (BTRIM(nombre) <> ''),

    CONSTRAINT chk_estructuras_tipo
        CHECK (tipo IN ('TORRE', 'BLOQUE', 'SECTOR', 'OTRO')),

    CONSTRAINT chk_estructuras_estado
        CHECK (estado IN ('ACTIVA', 'INACTIVA')),

    CONSTRAINT chk_estructuras_version
        CHECK (version >= 0)
);


CREATE UNIQUE INDEX uq_estructuras_nombre_activo
    ON estructuras_copropiedad (
        copropiedad_id,
        LOWER(nombre)
    )
    WHERE deleted_at IS NULL;


CREATE INDEX idx_estructuras_copropiedad
    ON estructuras_copropiedad (copropiedad_id);


CREATE TABLE unidades_residenciales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    copropiedad_id UUID NOT NULL,
    estructura_id UUID,
    numero VARCHAR(50) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_unidades_copropiedad
        FOREIGN KEY (copropiedad_id)
        REFERENCES copropiedades (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_unidades_estructura_copropiedad
        FOREIGN KEY (estructura_id, copropiedad_id)
        REFERENCES estructuras_copropiedad (id, copropiedad_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_unidades_id_copropiedad
        UNIQUE (id, copropiedad_id),

    CONSTRAINT chk_unidades_numero
        CHECK (BTRIM(numero) <> ''),

    CONSTRAINT chk_unidades_tipo
        CHECK (
            tipo IN (
                'APARTAMENTO',
                'CASA',
                'LOCAL',
                'OFICINA',
                'OTRO'
            )
        ),

    CONSTRAINT chk_unidades_estado
        CHECK (estado IN ('ACTIVA', 'INACTIVA')),

    CONSTRAINT chk_unidades_version
        CHECK (version >= 0)
);


CREATE UNIQUE INDEX uq_unidades_estructura_numero_activo
    ON unidades_residenciales (
        copropiedad_id,
        estructura_id,
        LOWER(numero)
    )
    WHERE deleted_at IS NULL
      AND estructura_id IS NOT NULL;


CREATE UNIQUE INDEX uq_unidades_sin_estructura_numero_activo
    ON unidades_residenciales (
        copropiedad_id,
        LOWER(numero)
    )
    WHERE deleted_at IS NULL
      AND estructura_id IS NULL;


CREATE INDEX idx_unidades_copropiedad
    ON unidades_residenciales (copropiedad_id);


CREATE INDEX idx_unidades_estructura
    ON unidades_residenciales (estructura_id);


CREATE TABLE usuarios_copropiedades_unidades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    copropiedad_id UUID NOT NULL,
    usuario_copropiedad_id UUID NOT NULL,
    unidad_residencial_id UUID NOT NULL,

    tipo_vinculacion VARCHAR(20) NOT NULL,
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_inicio DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_fin DATE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_usuarios_unidades_membresia_copropiedad
        FOREIGN KEY (usuario_copropiedad_id, copropiedad_id)
        REFERENCES usuarios_copropiedades (id, copropiedad_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_usuarios_unidades_unidad_copropiedad
        FOREIGN KEY (unidad_residencial_id, copropiedad_id)
        REFERENCES unidades_residenciales (id, copropiedad_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_usuarios_unidades_tipo_vinculacion
        CHECK (tipo_vinculacion IN ('PROPIETARIO', 'RESIDENTE')),

    CONSTRAINT chk_usuarios_unidades_estado
        CHECK (estado IN ('ACTIVA', 'INACTIVA', 'FINALIZADA')),

    CONSTRAINT chk_usuarios_unidades_fechas
        CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),

    CONSTRAINT chk_usuarios_unidades_version
        CHECK (version >= 0)
);


CREATE UNIQUE INDEX uq_usuarios_unidades_vinculacion_activa
    ON usuarios_copropiedades_unidades (
        usuario_copropiedad_id,
        unidad_residencial_id,
        tipo_vinculacion
    )
    WHERE deleted_at IS NULL
      AND estado = 'ACTIVA';


CREATE UNIQUE INDEX uq_usuarios_unidades_principal_activa
    ON usuarios_copropiedades_unidades (usuario_copropiedad_id)
    WHERE deleted_at IS NULL
      AND estado = 'ACTIVA'
      AND es_principal = TRUE;


CREATE INDEX idx_usuarios_unidades_copropiedad
    ON usuarios_copropiedades_unidades (copropiedad_id);


CREATE INDEX idx_usuarios_unidades_membresia
    ON usuarios_copropiedades_unidades (usuario_copropiedad_id);


CREATE INDEX idx_usuarios_unidades_unidad
    ON usuarios_copropiedades_unidades (unidad_residencial_id);
