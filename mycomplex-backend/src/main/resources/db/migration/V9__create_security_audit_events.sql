CREATE TABLE eventos_auditoria_seguridad (
    id UUID PRIMARY KEY,
    fecha_evento TIMESTAMP WITH TIME ZONE NOT NULL,
    tipo_evento VARCHAR(80) NOT NULL,
    resultado VARCHAR(20) NOT NULL,
    actor_usuario_id UUID,
    copropiedad_id UUID,
    usuario_objetivo_id UUID,
    correlation_id VARCHAR(36) NOT NULL,
    origen VARCHAR(80) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT fk_auditoria_actor
        FOREIGN KEY (actor_usuario_id) REFERENCES usuarios (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_auditoria_copropiedad
        FOREIGN KEY (copropiedad_id) REFERENCES copropiedades (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_auditoria_usuario_objetivo
        FOREIGN KEY (usuario_objetivo_id) REFERENCES usuarios (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_auditoria_resultado
        CHECK (resultado IN ('SUCCESS', 'FAILURE', 'DENIED')),
    CONSTRAINT chk_auditoria_correlation_id
        CHECK (correlation_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'),
    CONSTRAINT chk_auditoria_origen
        CHECK (BTRIM(origen) <> ''),
    CONSTRAINT chk_auditoria_metadata_objeto
        CHECK (jsonb_typeof(metadata) = 'object'),
    CONSTRAINT chk_auditoria_metadata_tamano
        CHECK (octet_length(metadata::text) <= 4096)
);

CREATE INDEX idx_auditoria_fecha
    ON eventos_auditoria_seguridad (fecha_evento DESC);

CREATE INDEX idx_auditoria_copropiedad_fecha
    ON eventos_auditoria_seguridad (copropiedad_id, fecha_evento DESC);

CREATE INDEX idx_auditoria_tipo_fecha
    ON eventos_auditoria_seguridad (tipo_evento, fecha_evento DESC);

CREATE INDEX idx_auditoria_resultado_fecha
    ON eventos_auditoria_seguridad (resultado, fecha_evento DESC);
