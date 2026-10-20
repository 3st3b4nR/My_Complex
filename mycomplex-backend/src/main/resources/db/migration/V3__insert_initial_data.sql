-- Primera copropiedad registrada en MyComplex

INSERT INTO copropiedades (
    id,
    nombre,
    nit,
    direccion,
    ciudad,
    correo_contacto,
    telefono_contacto,
    estado
)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    'Los Cerezos',
    '901845372-1',
    'Calle 63 # 54-38',
    'Rionegro',
    'administracion@loscerezos.test',
    '6045550138',
    'ACTIVA'
);


-- Primer usuario real del sistema, identificado mediante Auth0

INSERT INTO usuarios (
    id,
    auth0_subject,
    correo,
    nombres,
    apellidos,
    es_admin_sistema,
    estado
)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    'auth0|6ad6a66c5703ba3cd5629f70',
    'admin@mycomplex.com',
    'John Esteban',
    'Roldan Marin',
    FALSE,
    'ACTIVO'
);


-- Vinculación del usuario con Los Cerezos como administrador

INSERT INTO usuarios_copropiedades (
    id,
    usuario_id,
    copropiedad_id,
    rol,
    estado
)
VALUES (
    '30000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    'ADMIN_COPROPIEDAD',
    'ACTIVA'
);