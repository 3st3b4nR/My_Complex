CREATE TABLE eventos_correo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    destinatario VARCHAR(254) NOT NULL,
    asunto VARCHAR(255) NOT NULL,
    cuerpo TEXT NOT NULL,
    clave_entrega VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos INTEGER NOT NULL DEFAULT 0,
    proximo_intento_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    ultimo_error VARCHAR(120),
    enviado_at TIMESTAMP WITH TIME ZONE,
    mensaje_externo_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_eventos_correo_entrega
        UNIQUE (clave_entrega, destinatario),
    CONSTRAINT chk_eventos_correo_estado
        CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),
    CONSTRAINT chk_eventos_correo_intentos
        CHECK (intentos >= 0),
    CONSTRAINT chk_eventos_correo_envio
        CHECK (
            (estado = 'ENVIADO' AND enviado_at IS NOT NULL)
            OR (estado <> 'ENVIADO' AND enviado_at IS NULL)
        ),
    CONSTRAINT chk_eventos_correo_error
        CHECK (ultimo_error IS NULL OR LENGTH(ultimo_error) <= 120)
);

CREATE INDEX idx_eventos_correo_pendientes
    ON eventos_correo (proximo_intento_at, created_at)
    WHERE estado = 'PENDIENTE';
