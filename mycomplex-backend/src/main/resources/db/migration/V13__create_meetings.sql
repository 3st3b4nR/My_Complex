CREATE TABLE reuniones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    copropiedad_id UUID NOT NULL,
    creado_por_id UUID NOT NULL,
    actualizado_por_id UUID NOT NULL,

    titulo VARCHAR(180) NOT NULL,
    descripcion TEXT NOT NULL,
    enlace_reunion VARCHAR(2048) NOT NULL,
    programada_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recordatorio_enviado_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_reuniones_copropiedad
        FOREIGN KEY (copropiedad_id)
        REFERENCES copropiedades (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_reuniones_creado_por
        FOREIGN KEY (creado_por_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_reuniones_actualizado_por
        FOREIGN KEY (actualizado_por_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_reuniones_titulo
        CHECK (BTRIM(titulo) <> ''),

    CONSTRAINT chk_reuniones_descripcion
        CHECK (BTRIM(descripcion) <> ''),

    CONSTRAINT chk_reuniones_enlace_https
        CHECK (enlace_reunion ~* '^https://'),

    CONSTRAINT chk_reuniones_version
        CHECK (version >= 0)
);

CREATE INDEX idx_reuniones_copropiedad_programacion
    ON reuniones (copropiedad_id, programada_at)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_reuniones_recordatorio_pendiente
    ON reuniones (programada_at)
    WHERE deleted_at IS NULL
      AND recordatorio_enviado_at IS NULL;
