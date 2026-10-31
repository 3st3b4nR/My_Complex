# Estándar de ingeniería de MyComplex

Este documento define la línea base común para el trabajo de Haider y Esteban.
Describe el estado verificable de `feature/panel-administrativo-haider` en el
commit `eb494ba` y no convierte propuestas futuras en funcionalidades
implementadas. Ante una contradicción, el código, las pruebas y la configuración
versionada tienen prioridad; la diferencia debe documentarse y resolverse antes
de programar.

## 1. Stack y versiones verificadas

| Elemento | Versión o estado | Fuente versionada |
|---|---|---|
| Java | 21 | `mycomplex-backend/pom.xml`, `mycomplex-gateway/pom.xml`, Dockerfiles y `.github/workflows/ci.yml` |
| Maven | 3.10.0 | `distributionUrl` en ambos `.mvn/wrapper/maven-wrapper.properties` y `maven:3.10.0-eclipse-temurin-21-alpine` en ambos Dockerfiles |
| Maven Wrapper | script 3.3.4, distribución Maven 3.10.0 | `mycomplex-backend/.mvn/wrapper/maven-wrapper.properties` y equivalente del Gateway |
| Spring Boot | 4.1.2 | parent de ambos `pom.xml` |
| Spring Cloud | 2025.1.4 | `mycomplex-gateway/pom.xml` |
| PostgreSQL | imagen `postgres:17-alpine` | `compose.yaml` y servicio PostgreSQL del CI |
| Flyway | gestionado por Spring Boot 4.1.2; resolución observada 12.4.0 | starter y módulo PostgreSQL en `mycomplex-backend/pom.xml`; no existe pin directo propio |
| Angular | 22.2.3 resuelto | `mycomplex-frontend/package.json` y `package-lock.json` |
| Node.js | 24 en CI | `.github/workflows/ci.yml` |
| npm | 10.9.9 | campo `packageManager` de `mycomplex-frontend/package.json` |
| Docker Compose | Compose Specification; versión del plugin del host no fijada | `compose.yaml`, `observability/compose.observability.yaml` y comandos `docker compose` del CI |
| Auth0 Angular SDK | 2.13.0 resuelto | `mycomplex-frontend/package.json` y `package-lock.json` |
| Auth0 backend | OAuth2 Resource Server de Spring y Management API REST; no hay SDK Auth0 propio | POM, `application.properties` y `Auth0ExternalIdentityAdapter.java` |
| Prometheus | 3.15.1 | `observability/compose.observability.yaml` |
| Grafana | 13.3.0 | `observability/compose.observability.yaml` |
| Loki | 3.7.9 | `observability/compose.observability.yaml` |
| Tempo | 3.1.1 | `observability/compose.observability.yaml` |
| Alloy | 1.21.2 | `observability/compose.observability.yaml` |
| JaCoCo | 0.8.14 | POM de backend y Gateway |
| ArchUnit | dependencia transitiva de Spring Modulith; resolución observada 1.4.2 | `spring-modulith-starter-test` en el POM y `LayeredArchitectureTest.java`; no hay pin directo |
| Spring Modulith | 2.1.1 | `mycomplex-backend/pom.xml` |
| MapStruct | 1.6.3 | `mycomplex-backend/pom.xml` |
| CycloneDX Maven | 2.9.1, esquema SBOM 1.6 | ambos POM |
| Vitest / cobertura V8 | 5.0.5 | `mycomplex-frontend/package.json` y lockfile |
| Trivy Action / scanner | `v0.36.0` / `v0.76.1` | `.github/workflows/ci.yml` |
| Gitleaks | 8.30.1 | `.github/workflows/ci.yml` |
| CodeQL Action | v4 | `.github/workflows/ci.yml` |
| Sonar scan action | 8.3.0, opcional | `.github/workflows/ci.yml` |

Una dependencia administrada por Spring no debe fijarse arbitrariamente para
“uniformar” versiones. Primero se revisa el BOM efectivo, compatibilidad y
resultado del CI.

## 2. Arquitectura obligatoria

### 2.1 Monolito modular y dirección de dependencias

El backend es un único despliegue Spring Boot dividido en módulos de negocio.
Los módulos actualmente verificables son `users`, `properties` y
`observability`. Spring Modulith declara sus límites en:

- `com.mycomplex.users`: puede depender de `properties::api` y
  `observability::api`.
- `com.mycomplex.properties`: no depende de `users`.
- `com.mycomplex.observability`: publica su API transversal sin depender del
  módulo de usuarios.

Las reglas ejecutables están en `LayeredArchitectureTest.java` y
`ModularArchitectureTest.java`. Queda prohibido:

- importar infraestructura o repositorios internos de otro módulo;
- hacer que `properties` dependa de `users`;
- llamar un repository desde un controller;
- hacer que `domain` dependa de Spring, JPA o `infrastructure`;
- hacer que `application` dependa de `infrastructure`;
- crear dependencias circulares para reutilizar una tabla o una clase.

La comunicación entre módulos se hace mediante una interfaz pública nombrada.
El ejemplo real es `users` consumiendo
`properties.application.port.in.FindAvailableResidentialUnitsUseCase` y
`ResolveResidentialUnitsUseCase`, publicados por el `NamedInterface("api")`
de `properties`. `users` nunca importa
`properties.infrastructure.adapter.out.persistence`.

### 2.2 Capas y responsabilidades

| Capa | Responsabilidad | Ejemplos reales |
|---|---|---|
| Domain | modelos y reglas sin framework | `users/domain/model/PropertyMember.java`, `properties/domain/model/ResidentialUnitSummary.java` |
| Application input ports | contrato de una operación invocable | `UpdatePropertyMemberUseCase.java`, `FindAvailableResidentialUnitsUseCase.java` |
| Application services | orquestación, validación y autorización de negocio | `UpdatePropertyMemberService.java`, `FindPropertyMembersService.java` |
| Application output ports | necesidad de persistencia o servicio externo | `UpdatePropertyMemberPort.java`, `ExternalIdentityPort.java` |
| Input adapters | traducen HTTP/JWT a casos de uso | `PropertyMemberController.java`, `AuthenticatedUserLinkingFilter.java` |
| Output adapters | implementan persistencia o integración externa | `PropertyMemberUpdatePersistenceAdapter.java`, `Auth0ExternalIdentityAdapter.java` |
| Configuration | composición explícita de servicios y decorators | `UserModuleConfig.java`, `PropertyModuleConfig.java`, `ObservabilityModuleConfig.java` |

Los DTO REST viven en el adapter de entrada. Las entidades JPA, DBO y
proyecciones viven en persistencia. Nunca se serializa una entidad JPA. Los
mappers MapStruct usan `componentModel = SPRING` y
`unmappedTargetPolicy = ERROR`; una propiedad nueva debe mapearse o excluirse
con una justificación explícita.

### 2.3 Flujo real que debe imitar una funcionalidad nueva

El flujo de edición de membresía es el patrón de referencia:

1. `PropertyMemberController.updatePropertyMember` recibe `propertyId`,
   `membershipId`, JWT y `UpdatePropertyMemberRequest` validado.
2. `PropertyMemberRestMapper` crea `PropertyMemberUpdate`; el controller llama
   al input port `UpdatePropertyMemberUseCase`.
3. `UserModuleConfig` compone el servicio, el decorator de auditoría y el
   wrapper transaccional.
4. `AuditedUpdatePropertyMemberUseCase` verifica acceso, toma el estado previo
   y registra solamente los cambios administrativos efectivos.
5. `UpdatePropertyMemberService` valida administrador activo, propiedad de las
   unidades y reglas residenciales.
6. El servicio llama a `UpdatePropertyMemberPort`, implementado por
   `PropertyMemberUpdatePersistenceAdapter`.
7. El adapter usa SQL parametrizado, `propertyId`, `version`, locks y borrado
   lógico; PostgreSQL conserva las invariantes.
8. El resultado vuelve como dominio y el mapper produce un DTO REST, nunca una
   entidad persistente.

Para Noticias se debe reproducir la dirección, no copiar nombres o lógica de
usuarios: `NewsController → UseCase → ApplicationService → OutputPort →
Persistence/Blob Adapter`. Si se usa Blob Storage, PostgreSQL conserva datos y
metadatos y el binario queda detrás de un output port; el controller no llama
al SDK de almacenamiento.

### 2.4 Transacciones, concurrencia y borrado lógico

- La frontera transaccional de escrituras se compone explícitamente mediante
  wrappers como `TransactionalUpdatePropertyMemberUseCase`.
- Una escritura sensible usa la columna `version`, `@Version` o comprobación
  equivalente y devuelve conflicto ante una versión obsoleta.
- Los locks `FOR UPDATE` se reservan para invariantes concurrentes, como último
  administrador, reactivación o máquina de estados de invitación.
- El borrado funcional existente es lógico mediante `deleted_at` y estado. Las
  consultas deben excluir registros eliminados y preservar trazabilidad.
- No se agrega `@Transactional` al controller ni se extiende una transacción a
  una llamada remota sin diseñar recuperación e idempotencia.

### 2.5 Autorización, multi-tenant y auditoría

Auth0 autentica, pero no decide las reglas de negocio. Gateway y backend
validan issuer y audience; el backend debe volver a validar el rol, el estado
activo y la pertenencia al `propertyId`. Nunca se confía en un rol enviado por
Angular ni en un identificador sin acotarlo a la copropiedad.

Toda escritura administrativa debe evaluar:

1. actor autenticado obtenido del JWT;
2. autorización real en backend;
3. recurso acotado por `propertyId`;
4. validación, concurrencia e idempotencia;
5. auditoría durable después de una operación exitosa;
6. métrica/log seguro para denegaciones o fallos operativos relevantes.

`users` registra auditoría mediante decorators, no duplicando inserciones en
cada adapter. Una nueva funcionalidad debe reutilizar
`RecordSecurityAuditUseCase` si sus eventos pertenecen al catálogo común o
ampliar el catálogo de manera controlada. No se registran secretos, PII ni
payloads completos.

## 3. Principios de código

### 3.1 Convenciones obligatorias

- Código, paquetes, clases, interfaces, contratos y nombres técnicos: inglés.
- UI, accesibilidad y mensajes dirigidos al usuario: español.
- SQL: conserva nombres existentes en español (`copropiedad_id`, `deleted_at`)
  y convenciones de constraints e índices de las migraciones actuales.
- Un caso de uso representa una intención de negocio; no un passthrough para
  justificar otra carpeta.
- Las clases pequeñas deben tener una responsabilidad concreta y dependencias
  inyectadas por constructor.
- Los errores conocidos usan códigos estables y mensajes seguros; el fallback
  inesperado no expone stack trace y devuelve correlation ID.

### 3.2 Aplicación práctica

| Principio | Permitido | Prohibido |
|---|---|---|
| Clean Code | nombres de intención, métodos acotados, validaciones centrales | métodos gigantes, abreviaturas opacas, comentarios que contradicen el código |
| Clean Architecture | dominio y aplicación independientes de adapters | controller → repository o dominio con anotaciones JPA |
| SOLID | ports pequeños orientados al caso de uso, decorators de auditoría | interfaces “god”, herencia para compartir efectos laterales |
| KISS | servicio directo con los ports necesarios | bus de eventos, CQRS o framework adicional sin requisito |
| YAGNI | crear el adapter Blob cuando Noticias realmente almacene una imagen | carpetas vacías, interfaces para posibilidades hipotéticas |
| DRY | reutilizar validadores, error codes y API pública de módulos | copiar consultas/autorización en varios controllers; tampoco crear una abstracción prematura por dos líneas parecidas |

Reglas de seguridad innegociables:

- no exponer entidades JPA ni `auth0Subject` en DTO;
- no registrar JWT, `Authorization`, password, Client Secret, documento,
  correo, payload completo o variables de entorno;
- no usar `userId`, `propertyId`, correo, correlation ID, trace ID u otros
  valores por usuario como labels Prometheus;
- no duplicar use cases, adapters, exception handlers o fuentes de
  configuración;
- no crear capas o carpetas vacías para completar visualmente una arquitectura.

## 4. Definition of Done

Para cada cambio se revisa esta lista. “No aplica” exige una frase concreta; no
se crea código artificial para marcar una casilla.

- [ ] Dominio: modelo/regla solo si la funcionalidad introduce un concepto.
- [ ] Caso de uso: intención de negocio explícita.
- [ ] Input/output ports: solo para fronteras reales.
- [ ] Adapters: REST, persistencia o integración necesarios.
- [ ] Autorización: comprobada en backend, no solo en UI.
- [ ] Validación: DTO y regla de aplicación/persistencia apropiada.
- [ ] Persistencia: tenant, parámetros, índices y borrado lógico revisados.
- [ ] Errores: códigos, HTTP y mensajes seguros y testeados.
- [ ] Auditoría: obligatoria para escritura administrativa; “no aplica” para
  una lectura sin requisito de auditoría.
- [ ] Métrica de negocio: solo si habilita operación/alerta; sin alta
  cardinalidad.
- [ ] Logs: estructurados, útiles y sin datos sensibles.
- [ ] Correlation ID/traza: preservados en llamadas HTTP; no se inventa una
  integración distribuida cuando no existe.
- [ ] Pruebas focalizadas: comportamiento feliz, autorización, tenant,
  validación y error relevante.
- [ ] Documentación: contrato/operación/decisión actualizados cuando cambia.
- [ ] CI: último workflow del commit en verde o bloqueo documentado.
- [ ] Migraciones: número libre, orden, compatibilidad y reconstrucción desde
  una base vacía.
- [ ] Secretos: diff, logs y configuración revisados.
- [ ] Diff: solo archivos del alcance, sin `target`, `dist`, IDE o `.env`.

## 5. Estrategia eficiente de pruebas

Durante el desarrollo se ejecutan pruebas del servicio, adapter, controller o
componente afectado. No se repite una prueba si el código relacionado no cambió.
Antes del push se hace una sola verificación final razonable por componente
afectado. La suite completa, cobertura, CodeQL, Trivy, Gitleaks, build de
imágenes y SBOM corresponden al CI.

Está prohibido:

- eliminar o debilitar aserciones para obtener verde;
- excluir código de negocio para elevar cobertura;
- cambiar el resultado esperado sin que haya cambiado el contrato;
- ejecutar suites completas decenas de veces sin cambios intermedios;
- ocultar un fallo externo como si el control hubiera aprobado.

## 6. Métricas de calidad y CI

### 6.1 Umbrales reales

| Componente | Líneas | Ramas | Otros |
|---|---:|---:|---|
| Backend | 79 % | 56 % | gate JaCoCo `BUNDLE`; baseline medida 79,58 % / 56,14 % |
| Gateway | 95 % | 83 % | gate JaCoCo `BUNDLE`; baseline medida 95,24 % / 83,33 % |
| Frontend | 79 % | 69 % | statements 77 %, funciones 74 %; baseline medida 79,48 % / 69,43 % |

Backend y Gateway se configuran en sus POM. Frontend se configura en
`mycomplex-frontend/angular.json`. Un valor inferior hace fallar `verify` o
`npm run test:ci`.

### 6.2 Controles

| Control | Qué mide / propósito | Configuración | Falla cuando | Obligatorio |
|---|---|---|---|---|
| Pruebas Maven/Vitest | comportamiento y regresiones | POM, `package.json`, tests | una prueba falla | Sí |
| ArchUnit | dirección de capas y módulos | `LayeredArchitectureTest.java` | aparece dependencia prohibida | Sí, dentro del backend |
| Spring Modulith | ciclos y límites declarados | `ModularArchitectureTest.java`, `package-info.java` | módulo viola su API | Sí, dentro del backend |
| JaCoCo | líneas y ramas Java | ambos POM | baja de umbral | Sí |
| Cobertura V8 | statements/functions/lines/branches Angular | `angular.json` | baja de umbral | Sí |
| Angular production build | compilación y budgets | `angular.json`, CI | error TS/build o budget máximo | Sí |
| Compose | sintaxis y resolución | `compose.yaml`, CI | `docker compose config --quiet` falla | Sí |
| Docker build | imágenes reproducibles locales | Dockerfiles y CI | una imagen no compila | Sí |
| Gitleaks 8.30.1 | secretos versionados en historial | CI | hallazgo con exit code 1 | Sí |
| Trivy 0.76.1 | vulnerabilidades y misconfiguración | CI | hallazgo HIGH/CRITICAL o escaneo fallido | Sí |
| CodeQL v4 | SAST Java y JS/TS, `security-extended` | CI | análisis/build/publicación falla | Sí, sujeto a permisos de GitHub |
| CycloneDX | inventario de componentes | POM y Trivy | su paso no genera SBOM | Sí |
| Dependabot | actualización semanal Maven/npm/Actions/Docker | `.github/dependabot.yml` | no es un job; genera PR para revisión | Configurado |
| Sonar | quality gate externo | `sonar-project.properties`, CI | habilitado y gate/análisis falla | Opcional |

Sonar aparece `skipped` correctamente si `SONAR_ENABLED` no es `true`. Para
habilitarlo se requieren el secret `SONAR_TOKEN` y las variables
`SONAR_HOST_URL` y `SONAR_PROJECT_KEY`. Omitir Sonar no omite JaCoCo, V8,
CodeQL, Gitleaks ni Trivy.

Los artefactos, con retención de siete días, son:

- `backend-quality-reports`: Surefire, JaCoCo y SBOM;
- `gateway-quality-reports`: Surefire, JaCoCo y SBOM;
- `frontend-quality-reports`: cobertura y `dist`;
- `repository-security-reports`: Gitleaks, Trivy y SBOM frontend;
- `image-security-reports`: reportes Trivy y SBOM de ambas imágenes.

## 7. Métricas de observabilidad

Prometheus obtiene backend en `backend:9080/actuator/prometheus` y Gateway en
`gateway:9081/actuator/prometheus`. Grafana provisiona el dashboard
`MyComplex Overview`.

| Métrica Prometheus | Productor | Significado / labels usados | Visualización / alerta |
|---|---|---|---|
| `up` | Prometheus | scrape disponible; `job`, `instance` | paneles de disponibilidad; `MyComplexBackendDown`, `MyComplexGatewayDown` |
| `http_server_requests_seconds_count` | Micrometer backend/Gateway | volumen y errores; dashboard agrupa por `application` y `status` | RPS, 4xx/5xx; `MyComplexHigh5xxRate` (>5 % por 5 min) |
| `http_server_requests_seconds_bucket` | Micrometer backend/Gateway | histograma HTTP; dashboard agrega por `le`, `application` | p50/p95/p99; `MyComplexHighLatency` (p95 >1,5 s por 10 min) |
| `jvm_memory_used_bytes` | JVM de ambos | memoria por `application`, `area` | panel JVM memory |
| `jvm_threads_live_threads` | JVM de ambos | hilos vivos por aplicación | panel Live JVM threads |
| `hikaricp_connections_active` | backend | conexiones activas del pool | panel HikariCP |
| `hikaricp_connections_pending` | backend | solicitudes esperando conexión | panel HikariCP |
| `mycomplex_administrative_events_total` | backend | auditorías administrativas exitosas; label `event` de enum acotado | panel Administrative events |
| `mycomplex_administrative_access_denied_total` | backend | rechazo de operación; label `operation` en allowlist | panel; alerta >10 en 10 min durante 5 min |
| `mycomplex_invitation_failures_total` | backend | fallo de invitación; labels acotados `operation`, `reason` | panel; alerta >3 en 10 min durante 5 min |
| `mycomplex_audit_rejected_total` | backend | auditoría rechazada; `reason=validation|persistence` | alerta crítica ante persistencia >0 en 5 min |

Los valores permitidos se normalizan en
`MicrometerObservabilityMetricsAdapter`; valores desconocidos se convierten en
`other`. Nunca se añaden como labels `userId`, `propertyId`, email, documento,
Auth0 subject, correlation ID, trace ID, texto de excepción o cualquier valor
variable por usuario/request.

## 8. Logs, trazas, auditoría y correlación

| Concepto | Finalidad | Almacenamiento/herramienta |
|---|---|---|
| Log estructurado | diagnóstico temporal de la ejecución | Spring ECS JSON → archivo rotado → Alloy → Loki |
| Métrica | serie numérica agregable y de baja cardinalidad | Micrometer → Prometheus → Grafana/alertas |
| Traza distribuida | recorrido y duración Gateway/backend | OpenTelemetry OTLP → Tempo → Grafana |
| Evento de auditoría | evidencia durable de una acción administrativa | `eventos_auditoria_seguridad` en PostgreSQL |
| Correlation ID | relacionar request, respuesta y logs | `X-Correlation-ID`, validado/generado en Gateway y backend, MDC |
| Trace ID / Span ID | identificar traza y segmento técnico | OpenTelemetry; visibles en logs/Tempo |

Correlation ID no reemplaza trace ID y una traza no reemplaza auditoría. Loki
no es la fuente durable de acciones administrativas. Alloy solo lee los
volúmenes de logs y no monta el socket Docker.

Eventos durables actuales:

- `MEMBER_REGISTERED`;
- `MEMBERSHIP_ROLE_CHANGED`;
- `MEMBERSHIP_STATUS_CHANGED`;
- `RESIDENTIAL_ASSIGNMENTS_CHANGED`;
- `MEMBER_UNLINKED`;
- `INVITATION_REQUESTED`;
- `INVITATION_RESENT`;
- `AUTHENTICATED_IDENTITY_LINKED`.

La metadata está limitada a `registrationMode`, `changedField` e
`invitationMode`, 80 caracteres por valor y 4096 bytes de JSON. Una función
futura debe definir un evento estable, actor, copropiedad, objetivo, resultado
y metadata mínima en allowlist. No se agregan estado anterior completo, PII,
tokens o request body.

## 9. Inventario de fases posteriores a la revisión de rúbrica

### Fase 1 — Línea base y calidad

- **Objetivo/problema:** medir el estado real antes de imponer gates y evitar
  metas ficticias.
- **Evidencia:** baselines documentadas en `docs/devsecops/README.md`, tests
  existentes y commits funcionales de Gestión de usuarios.
- **Cambios funcionales:** ninguno por sí mismo; sirvió de inventario.
- **Cambios no funcionales:** umbrales basados en resultados reales.
- **Pendiente consciente:** aumentar cobertura gradualmente sin bajar gates.
- **No eliminar:** pruebas relevantes ni exclusiones justificadas acotadas.

### Fase 2 — Fortalecimiento arquitectónico

- **Commit principal:** `957817c refactor(architecture): enforce modular boundaries`.
- **Problema resuelto:** responsabilidades residenciales y transaccionales
  mezcladas, dependencias no verificadas y frontend con demasiada orquestación.
- **Archivos principales:** `package-info.java` de módulos,
  `LayeredArchitectureTest.java`, `ModularArchitectureTest.java`, API de
  `properties`, validators, adapters de persistencia separados, wrappers
  transaccionales, `UserManagementFacade` y `docs/architecture/`.
- **Decisiones:** `properties` posee estructuras/unidades; `users` consume su
  API; Compose raíz es canónico; linking ocurre tras JWT.
- **Pendiente:** los documentos `proposed-*` requieren aceptación formal del
  equipo aunque el código ya verifica parte de esas decisiones.
- **No eliminar:** tests de arquitectura, interfaces públicas, wrappers
  transaccionales ni `compose.yaml` canónico.

### Fase 3 — CI y DevSecOps

- **Commits:** `da7bd16`, `6126e21`, `2f1f27b`.
- **Problema resuelto:** CI parcial sin gates completos y escaneos reproducibles.
- **Archivos:** `.github/workflows/ci.yml`, `.github/dependabot.yml`, ambos POM,
  `angular.json`, lockfile, `sonar-project.properties`,
  `docs/devsecops/README.md`.
- **Herramientas:** JaCoCo, V8, Modulith/ArchUnit, CodeQL, Gitleaks, Trivy,
  CycloneDX, Dependabot y Sonar opcional.
- **Decisión:** solo CI; no CD, Azure, publicación ni despliegue.
- **Evidencia:** workflow exitoso para `eb494ba`.
- **Pendientes:** Sonar externo, branch protection y política formal de
  remediación.
- **No eliminar:** gates finales, caché Maven previa a Trivy, SBOM y artefactos.

### Fase 4 — Seguridad y modelado de amenazas

- **Commit:** `8cfeb9c docs(security): add threat model and OWASP assessment`.
- **Problema resuelto:** controles existentes sin trazabilidad ni riesgos
  explícitos.
- **Archivos:** `docs/security/` y propuesta `proposed-edge-waf.md`.
- **Alcance:** modelo STRIDE, evaluación selectiva OWASP Top 10/ASVS 5,
  Zero Trust y registro de riesgos.
- **Estado honesto:** evaluación documental parcial, no certificación. WAF,
  Azure, TLS/HSTS/CSP remotos, rate limiting general y tenant Auth0 continúan
  pendientes o no verificados.
- **No eliminar:** calificaciones `PARCIAL`, `PENDIENTE` y riesgos abiertos.

### Fase 5 — Observabilidad, auditoría y security logging

- **Commit:** `eb494ba feat(observability): add audit, metrics, logs and tracing`.
- **Problema resuelto:** falta de correlación extremo a extremo, auditoría
  durable, métricas operativas y plataforma local demostrable.
- **Archivos:** módulo `observability`, V9, filtros de correlación, decorators
  auditados, configuración Actuator/OpenTelemetry/ECS, `observability/`,
  `docs/observability/`, Dockerfiles y Compose.
- **Herramientas:** Prometheus, Grafana, Loki, Alloy, Tempo, Micrometer y
  OpenTelemetry.
- **Decisiones:** auditoría append-only en PostgreSQL; logs en Loki; trazas en
  Tempo; Grafana provisionado; puertos de management internos.
- **Pendientes:** Alertmanager/receptor, producción, TLS interno, alta
  disponibilidad y operación remota.
- **No eliminar:** V9, decorators de auditoría, `X-Correlation-ID`, volúmenes de
  logs, reglas/alertas, datasources, dashboard y runbooks.

## 10. Migraciones

V1–V9 son inmutables. V2 debe permanecer aunque esté vacía. V4 ya crea
`noticias`, por lo que es el punto más probable de conflicto con el trabajo de
Esteban. Nunca se modifica una migración aplicada, no se borra
`flyway_schema_history` y no se ejecuta `repair` para ocultar una divergencia.
Después de integrar ramas se confirma el último número libre; una migración
nueva debe ser posterior a todas las existentes y una base vacía debe poder
reconstruirse ejecutándolas en orden.
