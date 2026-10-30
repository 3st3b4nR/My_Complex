ALTER TABLE usuarios
    DROP CONSTRAINT chk_usuarios_estado_registro,
    DROP CONSTRAINT chk_usuarios_identidad_registro;

ALTER TABLE usuarios
    ADD COLUMN intentos_registro INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN ultimo_intento_registro_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN ultimo_error_registro VARCHAR(500),
    ADD COLUMN auth0_provisioned_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN invitacion_enviada_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN vinculado_at TIMESTAMP WITH TIME ZONE;

UPDATE usuarios
SET vinculado_at = COALESCE(vinculado_at, updated_at)
WHERE estado_registro = 'VINCULADO';

ALTER TABLE usuarios
    ADD CONSTRAINT chk_usuarios_estado_registro
        CHECK (estado_registro IN (
            'PENDIENTE',
            'APROVISIONANDO',
            'PROVISIONADO',
            'INVITADO',
            'VINCULADO'
        )),
    ADD CONSTRAINT chk_usuarios_intentos_registro
        CHECK (intentos_registro >= 0),
    ADD CONSTRAINT chk_usuarios_identidad_registro
        CHECK (
            (
                estado_registro = 'PENDIENTE'
                AND auth0_subject IS NULL
            )
            OR
            (
                estado_registro = 'APROVISIONANDO'
                AND (
                    auth0_subject IS NULL
                    OR BTRIM(auth0_subject) <> ''
                )
            )
            OR
            (
                estado_registro IN (
                    'PROVISIONADO', 'INVITADO', 'VINCULADO'
                )
                AND auth0_subject IS NOT NULL
                AND BTRIM(auth0_subject) <> ''
            )
        );
