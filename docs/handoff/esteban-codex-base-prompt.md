# Prompt base para el Codex de Esteban

Copie desde la línea siguiente hasta la plantilla final al iniciar una tarea.

---

Estás trabajando en el repositorio oficial **MyComplex**. Antes de proponer un
plan o modificar archivos, debes inspeccionar el repositorio real y leer
completamente, como mínimo:

1. `docs/handoff/team-engineering-standards.md`.
2. `docs/handoff/esteban-integration-guide.md`.
3. `docs/architecture/README.md` y las decisiones relevantes.
4. `docs/security/README.md`, el modelo de amenazas y el registro de riesgos.
5. `docs/devsecops/README.md`.
6. `docs/observability/README.md`, arquitectura y eventos de seguridad.

No trates documentos `proposed-*`, controles parciales o decisiones cloud
pendientes como si ya estuvieran implementados. El código, las pruebas y la
configuración versionada son la fuente de verdad.

## Reglas de trabajo obligatorias

### Inspección y alcance

- Confirma la rama, estado y diff mediante operaciones Git exclusivamente de
  lectura.
- Inspecciona implementaciones, imports, interfaces, configuración, migraciones
  y pruebas antes de crear código.
- Conserva cambios previos del usuario y no modifiques archivos ajenos al
  alcance.
- No agregues funcionalidades, capas, abstracciones, tecnologías o
  refactorizaciones que no sean necesarias para la solicitud.
- Si encuentras una ambigüedad que cambie el contrato funcional, propiedad de
  datos o numeración de migración, detente y pide una decisión.

### Arquitectura

- Mantén el monolito modular, Clean Architecture y puertos/adaptadores.
- `domain` no depende de Spring, JPA ni infrastructure.
- `application` no depende de infrastructure.
- Un controller llama un input port/use case, nunca un repository.
- Los application services usan output ports; los adapters implementan esos
  ports.
- No expongas entidades JPA. Usa DTO, DBO/proyecciones y mappers MapStruct con
  mapping estricto cuando corresponda.
- Respeta las APIs públicas entre módulos y Spring Modulith. No importes
  repositories, entities o adapters internos de otro módulo.
- `properties` es dueño de estructuras y unidades residenciales.
- `users` es dueño de usuarios, membresías, roles, estados y asignaciones.
- `properties` no depende de `users`; `users` consume `properties::api`.
- No crees carpetas o capas vacías y no dupliques use cases, adapters,
  validators, exception handlers o configuración.

### Convenciones de código

- Código, clases, paquetes, contratos y nombres técnicos en inglés.
- UI, accesibilidad y mensajes dirigidos al usuario en español.
- SQL compatible con la nomenclatura actual de las migraciones.
- Aplica Clean Code, SOLID, KISS, YAGNI y DRY con criterio: reutiliza conceptos
  reales sin crear frameworks internos prematuros.
- Conserva transacciones, optimistic versioning, locks e idempotencia cuando
  protejan una invariante.
- Conserva borrado lógico y filtros `deleted_at`/estado.

### Seguridad y aislamiento

- La autorización se implementa y prueba en backend; una restricción Angular
  nunca es suficiente.
- Toda operación por copropiedad valida actor, rol/estado y `propertyId` en
  aplicación y persistencia.
- No confíes en roles, tenant IDs o identificadores enviados por el cliente.
- No expongas `auth0Subject`, JWT, tokens, password, Client Secret, documentos,
  headers `Authorization`, variables de entorno, payloads completos ni stack
  traces al cliente.
- No registres esos datos en logs y no los uses como labels de métricas.
- No llames Auth0 Management API desde frontend.
- No envíes invitaciones reales ni crees/modifiques identidades Auth0 salvo
  autorización expresa de la tarea.

### Auditoría, métricas, logs y trazabilidad

- Toda escritura administrativa debe evaluar auditoría durable mediante la API
  de `observability`; evita insertar auditoría manualmente desde controllers.
- Agrega métricas solo si son operables y usa labels de allowlist y baja
  cardinalidad.
- Nunca uses `userId`, `propertyId`, email, correlation ID, trace ID o texto
  libre como label Prometheus.
- Conserva `X-Correlation-ID` extremo a extremo y el contexto W3C de trazas.
- Distingue log estructurado, métrica, traza y auditoría; uno no reemplaza otro.
- No imprimas secretos ni PII para diagnosticar.

### Flyway

- V1–V9 son inmutables; no modificar, renombrar, borrar o recombinar ninguna.
- V2 debe permanecer aunque esté vacía.
- No cambies checksums, no borres `flyway_schema_history` y no ejecutes
  `flyway repair` para ocultar inconsistencias.
- Antes de crear una migración, inspecciona todas las ramas integradas y
  confirma la última versión libre.
- Si existe el mismo número con contenidos diferentes, detente y solicita
  coordinación; no decidas una renumeración silenciosa.
- No recrees tablas o columnas ya existentes. Una base vacía debe reconstruirse
  ejecutando todas las migraciones en orden.

### Protección de funcionalidades existentes

- No modifiques Gestión de usuarios salvo que la integración o la tarea lo
  requiera directamente.
- Conserva autorización `PROPERTY_ADMIN`, aislamiento multi-tenant, último
  administrador, versionado, invitaciones, linking, auditoría y estados de
  Gestión de usuarios.
- No reemplaces `.github/workflows/ci.yml`, `compose.yaml`, observabilidad o
  configuración de seguridad por versiones anteriores.
- No implementes Azure, CD, WAF, publicación de imágenes o recursos cloud si
  la solicitud no los autoriza explícitamente.

### Pruebas y CI

- Durante el desarrollo ejecuta únicamente pruebas focalizadas sobre código
  afectado.
- No repitas pruebas que ya pasaron si ese código no volvió a cambiar.
- Realiza una sola verificación final razonable por componente afectado y deja
  suite completa, cobertura, CodeQL, Gitleaks, Trivy, SBOM e imágenes al CI.
- No modifiques pruebas para ocultar errores, no elimines aserciones válidas y
  no agregues exclusiones injustificadas para elevar cobertura.
- No desactives scanners, gates ni severidades para obtener verde.
- Si un control no pudo ejecutarse por una dependencia externa, documéntalo
  como pendiente; no lo declares aprobado.

### Archivos locales y Git

- No leas ni modifiques `.env`; usa solo nombres de variables provenientes de
  archivos versionados.
- No muestres secretos ni valores sensibles.
- No incluyas `.env`, `.sonarlint`, `node_modules`, `target`, `dist`, coverage
  o logs en la propuesta de commit.
- No ejecutes `git add`, commit, push, pull, fetch, merge, rebase, checkout,
  switch, reset, restore, clean, stash, staging ni cambio de rama.
- Puedes usar únicamente comandos Git de lectura cuando sean necesarios para
  inspeccionar el estado.
- El usuario realizará selección, commit, push e integración mediante GitHub
  Desktop.

## Forma de entrega

Al terminar:

1. Explica qué cambió y por qué.
2. Enumera rutas exactas de archivos creados, reemplazados o modificados.
3. Indica dependencias entre archivos o pasos pendientes.
4. Reporta comandos de prueba ejecutados y resultados exactos.
5. Declara verificaciones no ejecutadas y el motivo.
6. Enumera archivos exactos que el usuario debe seleccionar en GitHub Desktop.
7. Enumera archivos que debe excluir y el motivo.
8. Propón un mensaje Conventional Commit profesional siguiendo el historial
   real del repositorio.
9. Confirma que no se leyó/modificó `.env` y que no hubo operaciones Git de
   escritura o sincronización.

Detente antes de modificar archivos si encuentras:

- migraciones con número duplicado o checksum divergente;
- un secreto versionado o expuesto;
- conflicto que obligue a elegir entre contratos funcionales;
- dependencia circular entre módulos;
- necesidad de alterar Gestión de usuarios fuera del alcance;
- efecto externo irreversible no autorizado.

## Plantilla de la tarea concreta

Completa y agrega este bloque al final del prompt:

```text
FUNCIONALIDAD SOLICITADA:
<describir el resultado funcional exacto>

MÓDULO PROPIETARIO:
<por ejemplo: news; escribir PENDIENTE DE CONFIRMACIÓN si no está claro>

ACTORES Y ROLES AUTORIZADOS:
<roles y acciones permitidas>

COPROPIEDAD / AISLAMIENTO:
<cómo se obtiene y valida propertyId>

CONTRATOS ESPERADOS:
<rutas REST, DTO o comportamiento visible ya acordado; no inventar>

PERSISTENCIA / MIGRACIÓN:
<cambios necesarios o SIN MIGRACIÓN; confirmar última versión antes de crear>

AUDITORÍA Y OBSERVABILIDAD:
<eventos administrativos, métricas o logs requeridos; NO APLICA con razón>

ARCHIVOS PROTEGIDOS O FUERA DE ALCANCE:
<lista exacta>

PRUEBAS MÍNIMAS REQUERIDAS:
<servicio, adapter, controller, frontend, integración>

RESTRICCIONES ADICIONALES:
<efectos externos, secretos, infraestructura o decisiones pendientes>
```

---
