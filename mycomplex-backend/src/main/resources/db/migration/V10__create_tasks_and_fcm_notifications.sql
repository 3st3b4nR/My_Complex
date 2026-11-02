CREATE TABLE tareas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    copropiedad_id UUID NOT NULL,
    creada_por_usuario_copropiedad_id UUID NOT NULL,
    asignada_a_usuario_copropiedad_id UUID NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    descripcion VARCHAR(2000) NOT NULL,
    prioridad VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    ubicacion VARCHAR(180),
    fecha_limite TIMESTAMP WITH TIME ZONE NOT NULL,
    observacion_administrativa VARCHAR(1000),
    iniciada_at TIMESTAMP WITH TIME ZONE,
    completada_at TIMESTAMP WITH TIME ZONE,
    cancelada_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_tareas_copropiedad
        FOREIGN KEY (copropiedad_id)
        REFERENCES copropiedades (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_tareas_creador_copropiedad
        FOREIGN KEY (creada_por_usuario_copropiedad_id, copropiedad_id)
        REFERENCES usuarios_copropiedades (id, copropiedad_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_tareas_asignado_copropiedad
        FOREIGN KEY (asignada_a_usuario_copropiedad_id, copropiedad_id)
        REFERENCES usuarios_copropiedades (id, copropiedad_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_tareas_titulo
        CHECK (BTRIM(titulo) <> ''),
    CONSTRAINT chk_tareas_descripcion
        CHECK (BTRIM(descripcion) <> ''),
    CONSTRAINT chk_tareas_prioridad
        CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA', 'URGENTE')),
    CONSTRAINT chk_tareas_estado
        CHECK (estado IN ('PENDIENTE', 'EN_PROGRESO', 'COMPLETADA', 'CANCELADA')),
    CONSTRAINT chk_tareas_fechas_terminales
        CHECK (
            (estado = 'COMPLETADA' AND completada_at IS NOT NULL AND cancelada_at IS NULL)
            OR (estado = 'CANCELADA' AND cancelada_at IS NOT NULL AND completada_at IS NULL)
            OR (estado IN ('PENDIENTE', 'EN_PROGRESO') AND completada_at IS NULL AND cancelada_at IS NULL)
        ),
    CONSTRAINT chk_tareas_inicio
        CHECK (estado <> 'PENDIENTE' OR iniciada_at IS NULL),
    CONSTRAINT chk_tareas_version
        CHECK (version >= 0)
);

CREATE INDEX idx_tareas_copropiedad
    ON tareas (copropiedad_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_tareas_trabajador
    ON tareas (asignada_a_usuario_copropiedad_id, estado)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_tareas_estado
    ON tareas (copropiedad_id, estado)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_tareas_vencimiento
    ON tareas (copropiedad_id, fecha_limite)
    WHERE deleted_at IS NULL
      AND estado NOT IN ('COMPLETADA', 'CANCELADA');


CREATE TABLE instalaciones_fcm (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    identificador_instalacion VARCHAR(512) NOT NULL,
    plataforma VARCHAR(20) NOT NULL DEFAULT 'WEB',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    ultimo_uso_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revocada_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_instalaciones_fcm_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_instalaciones_fcm_identificador
        CHECK (BTRIM(identificador_instalacion) <> ''),
    CONSTRAINT chk_instalaciones_fcm_plataforma
        CHECK (plataforma = 'WEB'),
    CONSTRAINT chk_instalaciones_fcm_estado
        CHECK (estado IN ('ACTIVA', 'REVOCADA')),
    CONSTRAINT chk_instalaciones_fcm_revocacion
        CHECK (
            (estado = 'ACTIVA' AND revocada_at IS NULL)
            OR (estado = 'REVOCADA' AND revocada_at IS NOT NULL)
        ),
    CONSTRAINT chk_instalaciones_fcm_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX uq_instalaciones_fcm_identificador_activo
    ON instalaciones_fcm (identificador_instalacion)
    WHERE estado = 'ACTIVA';

CREATE INDEX idx_instalaciones_fcm_usuario
    ON instalaciones_fcm (usuario_id, ultimo_uso_at DESC)
    WHERE estado = 'ACTIVA';


CREATE TABLE eventos_notificacion_fcm (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tarea_id UUID NOT NULL,
    usuario_objetivo_id UUID NOT NULL,
    vinculacion_asignada_id UUID NOT NULL,
    tipo_evento VARCHAR(40) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos INTEGER NOT NULL DEFAULT 0,
    proximo_intento_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_error VARCHAR(120),
    enviado_at TIMESTAMP WITH TIME ZONE,
    mensaje_externo_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_eventos_fcm_tarea
        FOREIGN KEY (tarea_id)
        REFERENCES tareas (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_eventos_fcm_usuario
        FOREIGN KEY (usuario_objetivo_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_eventos_fcm_vinculacion
        FOREIGN KEY (vinculacion_asignada_id)
        REFERENCES usuarios_copropiedades (id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_eventos_fcm_asignacion
        UNIQUE (tarea_id, tipo_evento, vinculacion_asignada_id),
    CONSTRAINT chk_eventos_fcm_tipo
        CHECK (tipo_evento IN ('TAREA_ASIGNADA', 'TAREA_REASIGNADA')),
    CONSTRAINT chk_eventos_fcm_estado
        CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'FALLIDA', 'SIN_DESTINO')),
    CONSTRAINT chk_eventos_fcm_intentos
        CHECK (intentos >= 0),
    CONSTRAINT chk_eventos_fcm_envio
        CHECK (
            (estado = 'ENVIADA' AND enviado_at IS NOT NULL)
            OR (estado <> 'ENVIADA' AND enviado_at IS NULL)
        ),
    CONSTRAINT chk_eventos_fcm_error
        CHECK (ultimo_error IS NULL OR LENGTH(ultimo_error) <= 120)
);

CREATE INDEX idx_eventos_fcm_pendientes
    ON eventos_notificacion_fcm (proximo_intento_at, created_at)
    WHERE estado = 'PENDIENTE';

CREATE INDEX idx_eventos_fcm_tarea
    ON eventos_notificacion_fcm (tarea_id, created_at DESC);

CREATE INDEX idx_eventos_fcm_usuario
    ON eventos_notificacion_fcm (usuario_objetivo_id, created_at DESC);
