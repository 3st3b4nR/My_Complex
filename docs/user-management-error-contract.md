# Contrato de errores de Gestión de usuarios

Los códigos son identificadores técnicos estables en inglés. El backend decide
el estado HTTP y devuelve un mensaje seguro; Angular traduce los casos que
requieren una explicación específica. Ninguna respuesta expone mensajes de
excepciones de infraestructura.

| Categoría | Códigos | HTTP |
|---|---|---|
| Validación | `INVALID_PROPERTY_MEMBER_UPDATE`, `INVALID_PROPERTY_MEMBER_REGISTRATION`, `LAST_PROPERTY_ADMINISTRATOR` | 400 |
| No encontrado | `PROPERTY_MEMBER_NOT_FOUND` | 404 |
| Concurrencia y negocio | `PROPERTY_MEMBER_VERSION_CONFLICT`, `MEMBERSHIP_*`, `INVITATION_*`, `AUTHENTICATED_IDENTITY_CONFLICT` | 409 |
| Rate limiting | `AUTH0_RATE_LIMITED` | 429 |
| Integración no disponible | `AUTH0_NOT_CONFIGURED`, `AUTH0_UNAVAILABLE`, `AUTH0_INVITATION_UNAVAILABLE` | 503 |
| Configuración o permisos externos | `AUTH0_CONFIGURATION_ERROR`, `AUTH0_PERMISSION_DENIED` | 502 |

La lista tipada de referencia está en `UserErrorCode`; el catálogo visual está
en `property-member-error-messages.ts`.
