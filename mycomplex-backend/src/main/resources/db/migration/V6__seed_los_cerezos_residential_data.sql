INSERT INTO estructuras_copropiedad (
    id,
    copropiedad_id,
    nombre,
    tipo,
    estado,
    created_at,
    updated_at
)
VALUES
    (
        '40000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        'Torre 1',
        'TORRE',
        'ACTIVA',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '40000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        'Torre 2',
        'TORRE',
        'ACTIVA',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    );


INSERT INTO unidades_residenciales (
    id,
    copropiedad_id,
    estructura_id,
    numero,
    tipo,
    estado,
    created_at,
    updated_at
)
VALUES
    (
        '50000000-0000-0000-0000-000000000101',
        '10000000-0000-0000-0000-000000000001',
        '40000000-0000-0000-0000-000000000001',
        '101',
        'APARTAMENTO',
        'ACTIVA',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '50000000-0000-0000-0000-000000000102',
        '10000000-0000-0000-0000-000000000001',
        '40000000-0000-0000-0000-000000000001',
        '102',
        'APARTAMENTO',
        'ACTIVA',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '50000000-0000-0000-0000-000000000201',
        '10000000-0000-0000-0000-000000000001',
        '40000000-0000-0000-0000-000000000002',
        '201',
        'APARTAMENTO',
        'ACTIVA',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '50000000-0000-0000-0000-000000000202',
        '10000000-0000-0000-0000-000000000001',
        '40000000-0000-0000-0000-000000000002',
        '202',
        'APARTAMENTO',
        'ACTIVA',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    );


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
VALUES
    (
        '60000000-0000-0000-0000-000000000001',
        'demo|ana.gomez',
        'ana.gomez@loscerezos.test',
        'Ana',
        'Gómez',
        FALSE,
        'ACTIVO',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '60000000-0000-0000-0000-000000000002',
        'demo|luis.ramirez',
        'luis.ramirez@loscerezos.test',
        'Luis',
        'Ramírez',
        FALSE,
        'ACTIVO',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '60000000-0000-0000-0000-000000000003',
        'demo|carolina.restrepo',
        'carolina.restrepo@loscerezos.test',
        'Carolina',
        'Restrepo',
        FALSE,
        'ACTIVO',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '60000000-0000-0000-0000-000000000004',
        'demo|mateo.alvarez',
        'mateo.alvarez@loscerezos.test',
        'Mateo',
        'Álvarez',
        FALSE,
        'ACTIVO',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
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
VALUES
    (
        '70000000-0000-0000-0000-000000000001',
        '60000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        'PROPIETARIO',
        'ACTIVA',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '70000000-0000-0000-0000-000000000002',
        '60000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        'RESIDENTE',
        'ACTIVA',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '70000000-0000-0000-0000-000000000003',
        '60000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000001',
        'PROPIETARIO',
        'ACTIVA',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '70000000-0000-0000-0000-000000000004',
        '60000000-0000-0000-0000-000000000004',
        '10000000-0000-0000-0000-000000000001',
        'RESIDENTE',
        'ACTIVA',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
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
VALUES
    (
        '80000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        '70000000-0000-0000-0000-000000000001',
        '50000000-0000-0000-0000-000000000101',
        'PROPIETARIO',
        TRUE,
        'ACTIVA',
        DATE '2026-01-01',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '80000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        '70000000-0000-0000-0000-000000000002',
        '50000000-0000-0000-0000-000000000101',
        'RESIDENTE',
        TRUE,
        'ACTIVA',
        DATE '2026-01-01',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '80000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000001',
        '70000000-0000-0000-0000-000000000003',
        '50000000-0000-0000-0000-000000000201',
        'PROPIETARIO',
        TRUE,
        'ACTIVA',
        DATE '2026-01-01',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '80000000-0000-0000-0000-000000000004',
        '10000000-0000-0000-0000-000000000001',
        '70000000-0000-0000-0000-000000000003',
        '50000000-0000-0000-0000-000000000202',
        'PROPIETARIO',
        FALSE,
        'ACTIVA',
        DATE '2026-01-01',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    ),
    (
        '80000000-0000-0000-0000-000000000005',
        '10000000-0000-0000-0000-000000000001',
        '70000000-0000-0000-0000-000000000004',
        '50000000-0000-0000-0000-000000000201',
        'RESIDENTE',
        TRUE,
        'ACTIVA',
        DATE '2026-01-01',
        TIMESTAMPTZ '2026-01-01 00:00:00+00',
        TIMESTAMPTZ '2026-01-01 00:00:00+00'
    );
