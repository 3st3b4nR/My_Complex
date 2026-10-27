CREATE TABLE noticias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    copropiedad_id UUID NOT NULL,
    autor_id UUID NOT NULL,
    actualizado_por_id UUID NOT NULL,

    titulo VARCHAR(180) NOT NULL,
    contenido TEXT NOT NULL,

    imagen_blob_path VARCHAR(512),
    imagen_content_type VARCHAR(100),

    publicada_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_noticias_copropiedad
        FOREIGN KEY (copropiedad_id)
        REFERENCES copropiedades (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_noticias_autor
        FOREIGN KEY (autor_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_noticias_actualizado_por
        FOREIGN KEY (actualizado_por_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_noticias_titulo
        CHECK (BTRIM(titulo) <> ''),

    CONSTRAINT chk_noticias_contenido
        CHECK (BTRIM(contenido) <> ''),

    CONSTRAINT chk_noticias_imagen
        CHECK (
            (imagen_blob_path IS NULL
                AND imagen_content_type IS NULL)
            OR
            (imagen_blob_path IS NOT NULL
                AND imagen_content_type IS NOT NULL)
        ),

    CONSTRAINT chk_noticias_version
        CHECK (version >= 0)
);

CREATE INDEX idx_noticias_copropiedad_publicacion
    ON noticias (copropiedad_id, publicada_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_noticias_autor
    ON noticias (autor_id);