-- Corrige el estado de registro del propietario creado en V11.
-- La identidad definitiva será asignada por el proceso de invitación de Auth0.

UPDATE usuarios
SET auth0_subject = NULL,
    estado_registro = 'PENDIENTE',
    intentos_registro = 0,
    ultimo_intento_registro_at = NULL,
    ultimo_error_registro = NULL,
    auth0_provisioned_at = NULL,
    invitacion_enviada_at = NULL,
    vinculado_at = NULL,
    updated_at = TIMESTAMPTZ '2026-11-02 00:00:00+00'
WHERE id = '60000000-0000-0000-0000-000000000005'
  AND LOWER(correo) = LOWER('esteban.roldanrm@gmail.com');
