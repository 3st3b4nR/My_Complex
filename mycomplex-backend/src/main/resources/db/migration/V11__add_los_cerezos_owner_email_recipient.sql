-- Propietario activo de Los Cerezos para recepción de notificaciones.
-- El auth0_subject es provisional hasta que el usuario tenga identidad en Auth0.

INSERT INTO usuarios (
    id,
    auth0_subject,
    correo,
    nombres,
    apellidos,
    es_admin_sistema,
    estado,
    created_at,
    updated_at
)
VALUES (
    '60000000-0000-0000-0000-000000000005',
    'seed|esteban.roldan.propietario',
    'esteban.roldanrm@gmail.com',
    'Esteban',
    'Roldan Marin',
    FALSE,
    'ACTIVO',
    TIMESTAMPTZ '2026-11-02 00:00:00+00',
    TIMESTAMPTZ '2026-11-02 00:00:00+00'
);


INSERT INTO usuarios_copropiedades (
    id,
    usuario_id,
    copropiedad_id,
    rol,
    estado,
    created_at,
    updated_at
)
VALUES (
    '70000000-0000-0000-0000-000000000005',
    '60000000-0000-0000-0000-000000000005',
    '10000000-0000-0000-0000-000000000001',
    'PROPIETARIO',
    'ACTIVA',
    TIMESTAMPTZ '2026-11-02 00:00:00+00',
    TIMESTAMPTZ '2026-11-02 00:00:00+00'
);


INSERT INTO usuarios_copropiedades_unidades (
    id,
    copropiedad_id,
    usuario_copropiedad_id,
    unidad_residencial_id,
    tipo_vinculacion,
    es_principal,
    estado,
    fecha_inicio,
    created_at,
    updated_at
)
VALUES (
    '80000000-0000-0000-0000-000000000006',
    '10000000-0000-0000-0000-000000000001',
    '70000000-0000-0000-0000-000000000005',
    '50000000-0000-0000-0000-000000000102',
    'PROPIETARIO',
    TRUE,
    'ACTIVA',
    DATE '2026-11-02',
    TIMESTAMPTZ '2026-11-02 00:00:00+00',
    TIMESTAMPTZ '2026-11-02 00:00:00+00'
);
