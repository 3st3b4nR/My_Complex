ALTER TABLE usuarios
    ALTER COLUMN auth0_subject DROP NOT NULL;

ALTER TABLE usuarios
    ADD COLUMN estado_registro VARCHAR(20) NOT NULL DEFAULT 'VINCULADO';

ALTER TABLE usuarios
    ADD CONSTRAINT chk_usuarios_estado_registro
        CHECK (estado_registro IN ('PENDIENTE', 'VINCULADO')),
    ADD CONSTRAINT chk_usuarios_identidad_registro
        CHECK (
            (estado_registro = 'PENDIENTE' AND auth0_subject IS NULL)
            OR
            (
                estado_registro = 'VINCULADO'
                AND auth0_subject IS NOT NULL
                AND BTRIM(auth0_subject) <> ''
            )
        );
