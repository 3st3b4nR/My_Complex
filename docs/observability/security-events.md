# Eventos de seguridad y privacidad

## Eventos durables

- `MEMBER_REGISTERED`
- `MEMBERSHIP_ROLE_CHANGED`
- `MEMBERSHIP_STATUS_CHANGED`
- `RESIDENTIAL_ASSIGNMENTS_CHANGED`
- `MEMBER_UNLINKED`
- `INVITATION_REQUESTED`
- `INVITATION_RESENT`
- `AUTHENTICATED_IDENTITY_LINKED`

La tabla `eventos_auditoria_seguridad` no tiene adapter de actualización ni
borrado. Guarda UUID, UTC, tipo, resultado, actor local opcional, copropiedad,
usuario objetivo, correlation ID, origen y metadata permitida.

Metadata permitida: `registrationMode`, `changedField` e `invitationMode`, con
valor máximo de 80 caracteres y JSON total máximo de 4096 bytes.

## Eventos no durables

Denegaciones administrativas, fallos de invitación y fallos de persistencia de
auditoría generan métricas de baja cardinalidad y logs estructurados. No se
fuerza su inserción en la transacción fallida.

## Datos prohibidos

JWT, Authorization header, Client Secret, password, Auth0 subject completo,
correo, documento, payload, variables de entorno y valores anteriores con PII.
Los IDs locales se guardan como referencias técnicas; nunca son tags de
Prometheus.

Los stack traces se conservan únicamente en logs internos `ERROR`. El cliente
recibe código `UNEXPECTED_ERROR`, mensaje genérico y correlation ID.
