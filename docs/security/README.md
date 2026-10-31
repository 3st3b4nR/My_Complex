# Seguridad de MyComplex

Estado: evaluación documental basada en el código de la rama
`feature/panel-administrativo-haider` al 30 de octubre de 2026.

Esta carpeta concentra la línea base de seguridad de MyComplex. No declara una
certificación OWASP ni sustituye pruebas de penetración. Cada control marcado
como implementado tiene una evidencia verificable en el repositorio; lo que
depende del despliegue o de configuración externa se conserva como pendiente.

## Alcance evaluado

- Angular Web App, API Gateway, backend modular y PostgreSQL.
- Autenticación Auth0 mediante OAuth 2.0/OIDC y JWT Bearer.
- Gestión administrativa de usuarios, membresías e invitaciones.
- Integraciones salientes del backend con Auth0 Management API.
- Cadena CI de pruebas, cobertura, análisis de dependencias, secretos, imágenes
  y SBOM.
- Diseño futuro del borde cloud, sin implementar infraestructura Azure ni CD.

Quedan fuera de esta fase la seguridad de la suscripción Azure, la configuración
real del tenant Auth0, la recepción efectiva de correos, pruebas de penetración
dinámicas y cualquier recurso de producción aún inexistente.

## Postura resumida

| Área | Estado | Evidencia principal |
|---|---|---|
| Autenticación | Implementada | Resource Server en backend y gateway; Auth0 Angular SDK |
| Autorización y aislamiento | Implementados | Verificación de `PROPERTY_ADMIN`, `propertyId` y pruebas multi-tenant |
| Sesión web | Implementada como stateless | JWT Bearer; sin sesión de servidor, Basic ni form login |
| CSRF | Aceptado para API stateless | CSRF deshabilitado; el token se envía en `Authorization`, no en cookie |
| Validación e inyección | Implementadas | DTO con Bean Validation y JDBC/JPA parametrizado |
| Concurrencia | Implementada | `@Version`, locks y transacciones en operaciones sensibles |
| Seguridad de dependencias | Implementada en CI | CodeQL, Gitleaks, Trivy, Dependabot y SBOM CycloneDX |
| Registro de seguridad | Implementado para operaciones administrativas cubiertas | Correlation ID, logs JSON, auditoría persistente append-only, métricas y alertas locales |
| Antiabuso | Parcial | Cooldown de reenvío; falta rate limiting general en el borde |
| TLS y headers de producción | Pendiente del despliegue | No existe aún arquitectura Azure aprobada |

## Documentos

- [Modelo de amenazas](threat-model.md)
- [Evaluación OWASP Top 10:2025](owasp-top-10-2025.md)
- [Evaluación OWASP ASVS 5.0.0](asvs-5-assessment.md)
- [Aplicación de Zero Trust](zero-trust.md)
- [Registro priorizado de riesgos](risk-register.md)
- [Propuesta de borde y WAF](../architecture/proposed-edge-waf.md)

## Convenciones de evidencia

- **VERIFICADO**: el control se observó en código, configuración o pruebas.
- **PARCIAL**: existe una defensa, pero no cubre todo el requisito o depende de
  un ambiente no inspeccionado.
- **NO VERIFICADO**: no hay evidencia suficiente; no equivale por sí solo a una
  vulnerabilidad.
- **PENDIENTE**: se requiere implementación o decisión futura.
- **NO APLICA**: el requisito no corresponde al alcance actual y se justifica.

Las referencias ASVS usan identificadores versionados `v5.0.0-x.y.z`. La fuente
normativa es [OWASP ASVS 5.0.0](https://github.com/OWASP/ASVS/tree/v5.0.0/5.0/en)
y la clasificación de riesgos se alinea con
[OWASP Top 10:2025](https://top10.owasp.org/2025/).

## Catálogo de evidencia verificable

| ID | Ruta exacta | Evidencia |
|---|---|---|
| E-01 | `mycomplex-backend/src/main/java/com/mycomplex/users/infrastructure/config/UserSecurityConfiguration.java` | API stateless, rutas públicas y Resource Server JWT |
| E-02 | `mycomplex-gateway/src/main/java/com/mycomplex/gateway/security/GatewaySecurityConfig.java` | JWT y rutas protegidas del Gateway |
| E-03 | `mycomplex-gateway/src/main/java/com/mycomplex/gateway/filter/CorrelationIdFilter.java` | Correlation ID validado/generado |
| E-04 | `mycomplex-backend/src/main/java/com/mycomplex/users/application/service/FindPropertyMembersService.java` | Autorización activa de administrador por copropiedad |
| E-05 | `mycomplex-backend/src/main/java/com/mycomplex/users/infrastructure/adapter/out/persistence/PropertyMemberManagementPersistenceAdapter.java` | Locks, transacciones, aislamiento y último administrador |
| E-06 | `mycomplex-backend/src/main/java/com/mycomplex/users/infrastructure/adapter/out/persistence/PropertyMemberUpdatePersistenceAdapter.java` | Optimistic locking y actualización transaccional |
| E-07 | `mycomplex-backend/src/main/java/com/mycomplex/users/infrastructure/adapter/out/persistence/UserInvitationPersistenceAdapter.java` | Máquina de estados, autorización y cooldown |
| E-08 | `mycomplex-backend/src/main/java/com/mycomplex/users/infrastructure/adapter/out/auth0/Auth0ExternalIdentityAdapter.java` | Integración M2M, conexión exacta, timeouts y errores seguros |
| E-09 | `mycomplex-backend/src/main/java/com/mycomplex/users/infrastructure/adapter/in/rest/UserRestExceptionHandler.java` | Contrato de errores conocidos sin detalles internos |
| E-10 | `mycomplex-backend/src/main/java/com/mycomplex/properties/infrastructure/adapter/out/persistence/ResidentialUnitQueryPersistenceAdapter.java` | Consulta residencial parametrizada y acotada por propiedad |
| E-11 | `mycomplex-backend/src/test/java/com/mycomplex/users/infrastructure/adapter/out/persistence/PropertyMemberManagementIntegrationTest.java` | Aislamiento, concurrencia, rollback y último administrador |
| E-12 | `mycomplex-backend/src/test/java/com/mycomplex/users/infrastructure/adapter/out/persistence/repository/UserPropertyJpaRepositoryTest.java` | Membresías limitadas a la copropiedad solicitada |
| E-13 | `mycomplex-backend/src/test/java/com/mycomplex/users/application/service/InvitePropertyMemberServiceTest.java` | Ownership de identidad, no duplicación y recuperación |
| E-14 | `.github/workflows/ci.yml` | Pruebas, cobertura, CodeQL, Gitleaks, Trivy y SBOM |
| E-15 | `mycomplex-frontend/src/app/app.config.ts` | Auth0 Angular SDK e interceptor HTTP para la API permitida |
| E-16 | `mycomplex-backend/src/main/resources/application.properties` y `mycomplex-gateway/src/main/resources/application.properties` | issuer, audience, Actuator, métricas, trazas y logging estructurado |
| E-17 | `mycomplex-backend/src/main/java/com/mycomplex/observability/`, `observability/` y `docs/observability/` | Auditoría durable, métricas, logs, trazas, alertas, dashboard y runbooks |

## Reglas que deben preservarse

1. El frontend nunca accede directamente a PostgreSQL ni a Auth0 Management API.
2. Gateway y backend validan JWT; el backend vuelve a aplicar autorización de
   negocio y aislamiento por copropiedad.
3. `properties` es dueño de estructuras y unidades; `users` consume su puerto.
4. Secretos y tokens no se versionan, imprimen ni incluyen en respuestas.
5. Un control de borde futuro complementa, pero no reemplaza, validación,
   autorización ni consultas seguras dentro de la aplicación.
6. Toda nueva operación administrativa sensible debe considerar autorización,
   auditoría, idempotencia/concurrencia y aislamiento multi-tenant.
