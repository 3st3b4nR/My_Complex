# Guía de integración para Esteban

Objetivo: integrar `feature/panel-administrativo-haider` con el trabajo de
Noticias sin perder ninguno de los dos conjuntos de cambios. Esta guía no
indica que el merge ya se haya realizado.

Estado de origen verificado:

- rama de Haider: `feature/panel-administrativo-haider`;
- commit entregado: `eb494ba feat(observability): add audit, metrics, logs and tracing`;
- último MyComplex CI conocido sobre ese commit: exitoso;
- rama de Noticias de Esteban: **no identificable en las referencias locales**.
  En esta guía se representa como `<rama-de-esteban-noticias>` y debe
  sustituirse por el nombre confirmado en GitHub Desktop.

## 1. Estado previo obligatorio

Antes de integrar, Esteban debe:

1. Abrir el repositorio correcto en GitHub Desktop y confirmar
   `Current repository` y `Current branch`.
2. Seleccionar `<rama-de-esteban-noticias>` y revisar la pestaña `Changes`.
3. Guardar el trabajo de Noticias en archivos y revisar que no haya temporales,
   secretos, `target`, `dist`, `node_modules`, logs o archivos del IDE.
4. Crear un commit profesional de Noticias y usar `Push origin`.
5. Confirmar en GitHub que el commit aparece en la rama correcta.
6. Confirmar que no quedan cambios importantes sin commit. No continuar con
   cambios parciales que puedan confundirse con conflictos del merge.
7. Confirmar que `feature/panel-administrativo-haider` está publicada y que su
   último GitHub Actions está verde.
8. No copiar carpetas manualmente, no descargar ZIP y no enviar archivos
   versionados por WhatsApp o correo.

Si el CI de cualquiera de las ramas está rojo por código, la integración debe
esperar. Un job `skipped` de Sonar es esperado cuando Sonar no está habilitado.

## 2. Estrategia recomendada: rama temporal desde Noticias

La estrategia más segura es crear una rama temporal **a partir de la rama de
Esteban** y fusionar en ella la rama de Haider. Esto conserva intactas ambas
ramas originales, permite resolver y probar el resultado combinado y facilita
descartar el intento sin perder trabajo.

Nombre recomendado para la rama temporal:

```text
feature/integration-news-users
```

### Paso a paso en GitHub Desktop

1. Pulse `Fetch origin` para actualizar referencias. No use `Pull origin` sin
   revisar primero qué rama está seleccionada y qué cambios recibirá.
2. En `Current branch`, seleccione `<rama-de-esteban-noticias>`.
3. Compruebe otra vez que `Changes` esté vacío y que el commit de Noticias esté
   publicado.
4. Use `Branch > New branch…`.
5. Cree `feature/integration-news-users` desde
   `<rama-de-esteban-noticias>` y selecciónela como rama actual.
6. Use `Branch > Merge into current branch…`.
7. Como origen del merge seleccione exactamente
   `feature/panel-administrativo-haider`.
8. Antes de confirmar, verifique esta dirección:

   ```text
   actual: feature/integration-news-users
   origen: feature/panel-administrativo-haider
   ```

9. Si no hay conflictos, revise el historial y el diff combinado antes de
   validar. Si hay conflictos, GitHub Desktop mostrará los archivos en
   `Conflicted files`.
10. Resuelva cada conflicto por intención. No seleccione globalmente “ours” o
    “theirs”. En una rama temporal, “ours” es Noticias y “theirs” es la rama de
    Haider, pero ambos pueden contener cambios obligatorios.
11. Si aparece un conflicto que no entiende, use `Branch > Abort merge`,
    confirme que volvió al estado anterior y coordine con Haider. No use reset,
    restore ni borrado manual del repositorio.
12. Cuando todos los conflictos estén resueltos, revise `Changes`, el diff y la
    lista de archivos. Deben conservarse Noticias, Gestión de usuarios,
    arquitectura, CI, seguridad y observabilidad.
13. Ejecute la validación focalizada del código combinado y después una única
    verificación final apropiada. GitHub Actions seguirá siendo el gate total.
14. Complete el commit de merge/integración que GitHub Desktop proponga, con
    una descripción que mencione ambas funcionalidades. No mezcle correcciones
    funcionales no relacionadas en ese commit.
15. Use `Push origin` sobre la rama temporal.
16. Abra GitHub Actions, confirme todos los jobs obligatorios y revise sus
    artefactos. No lleve el resultado a una rama compartida mientras haya un
    job rojo o un conflicto pendiente.
17. Solo cuando el resultado integrado esté verde y revisado, creen un Pull
    Request desde `feature/integration-news-users` hacia la rama
    compartida acordada. No asumir que esa rama es `main` sin decisión del
    equipo.

## 3. Conflictos críticos

### `compose.yaml`

La raíz contiene el Compose canónico con `postgres`, `backend` y `gateway`,
health checks, volúmenes y configuración de observabilidad. Conserve los
servicios de Haider y agregue únicamente dependencias reales de Noticias. No
recupere `mycomplex-backend/compose.yaml`, eliminado durante la consolidación,
ni duplique PostgreSQL. Valide el resultado con `docker compose config`.

### `.github/workflows/ci.yml`

No reemplace el workflow fortalecido por una versión anterior. Deben seguir
existiendo los jobs `backend`, `gateway`, `frontend`, `docker`,
`repository-security`, `codeql` y `sonar`. Conserve gates, SBOM, cache Maven
antes de Trivy y versiones de actions. Integre pruebas de Noticias dentro del
job correspondiente; no cree una segunda CI paralela.

### POM

Combine dependencias, no archivos completos. Mantenga Java 21, Boot 4.1.2,
Modulith 2.1.1, MapStruct 1.6.3, JaCoCo 0.8.14, CycloneDX 2.9.1 y los plugins de
cobertura/SBOM. Una dependencia de Azure Blob para Noticias, si forma parte del
diseño aprobado, debe añadirse una sola vez y quedar detrás de un adapter.

### `package.json` y `package-lock.json`

Conserve scripts, Angular 22.2.3, npm 10.9.9, Auth0 y cobertura. Integre las
dependencias reales de Noticias en `package.json` y regenere el lockfile con
`npm install` o el comando acordado; no edite manualmente cientos de entradas
del lockfile ni acepte un lockfile que omita dependencias de una rama. Para una
instalación reproducible posterior use `npm ci`.

### Seguridad

Conserve `UserSecurityConfiguration`, `GatewaySecurityConfig`, validación JWT
doble, CORS acotado y linking posterior a autenticación. Noticias debe validar
autorización y `propertyId` en backend. No convierta Gateway en lugar de lógica
de negocio y no habilite rutas públicas por comodidad.

### Configuración modular

`UserModuleConfig`, `PropertyModuleConfig`, `ObservabilityModuleConfig` y los
`package-info.java` definen composición y límites. Si Noticias crea un módulo
`news`, debe publicar/consumir APIs explícitas y pasar Modulith/ArchUnit. No
importe repositorios de `users` para resolver autor o copropiedad.

### Rutas Angular

Combine `app.routes.ts` conservando login, selección, dashboard y
`/properties/:propertyId/users`, y agregue la ruta real de Noticias sin borrar
guards ni títulos. Revise también enlaces del dashboard, no solo el archivo de
rutas.

### Flyway

Reglas obligatorias:

- V1–V9 son inmutables.
- V2 no puede eliminarse aunque esté vacía.
- No edite una migración aplicada ni su checksum.
- V4 ya crea `noticias`; no vuelva a crear la misma tabla.
- Antes de crear otra migración, determine la última versión después del merge.
- Si ambas ramas contienen el mismo número con contenidos diferentes, detenga
  la integración y coordine una renumeración de la migración aún no integrada.
- No copie tablas existentes a una migración nueva.
- No borre ni edite `flyway_schema_history` y no use `repair` para ocultar el
  conflicto.
- Una base vacía debe reconstruirse ejecutando todas las migraciones en orden.

V4 es el conflicto potencial más importante con Noticias. Compare el modelo de
Esteban con `V4__create_noticias.sql` antes de resolver cualquier conflicto.

### Documentación y observabilidad

Combine documentación por tema. No borre evaluaciones `PARCIAL` o `PENDIENTE`
para declarar cumplimiento inexistente. Conserve `observability/`, dashboard,
datasources, alertas y runbooks. Si Noticias añade métricas, use labels acotados;
si publica una noticia y eso es una acción administrativa, agregue auditoría y
solo las alertas justificadas.

## 4. Qué se transfiere automáticamente con la rama

Todo elemento versionado llega mediante el merge; no se envía manualmente.

| Elemento | Rutas principales |
|---|---|
| Backend y módulos | `mycomplex-backend/src/main/`, `src/test/` |
| Gateway | `mycomplex-gateway/` |
| Frontend Angular | `mycomplex-frontend/src/` y configuración Angular |
| Migraciones V1–V9 | `mycomplex-backend/src/main/resources/db/migration/` |
| CI | `.github/workflows/ci.yml` |
| Dependabot | `.github/dependabot.yml` |
| Sonar opcional | `sonar-project.properties` y job `sonar` |
| Docker | ambos Dockerfiles y `compose.yaml` |
| Observabilidad local | `observability/compose.observability.yaml` |
| Prometheus y alertas | `observability/prometheus/` |
| Dashboard y datasources Grafana | `observability/grafana/` |
| Loki, Tempo y Alloy | `observability/loki/`, `tempo/`, `alloy/` |
| Runbooks | `observability/runbooks/` |
| Arquitectura | `docs/architecture/` |
| Seguridad | `docs/security/` |
| DevSecOps | `docs/devsecops/` |
| Observabilidad | `docs/observability/` |
| Pruebas | árboles `src/test` y archivos `*.spec.ts` |
| Maven/npm | POM, wrappers, `package.json`, `package-lock.json` |

## 5. Qué no se transfiere automáticamente

| Elemento | ¿Se necesita? / acción segura |
|---|---|
| `.env` | Sí para Compose local. Esteban crea el suyo con nombres documentados; nunca se sube. Compartir valores solo mediante gestor de secretos o canal seguro acordado. |
| Passwords/credenciales PostgreSQL | Sí localmente; generar valores propios y no reutilizarlos. Nunca Git/WhatsApp. |
| Credenciales Grafana | Sí para observabilidad; crear usuario/password locales. Nunca versionarlos. |
| Credenciales Auth0 M2M | Solo para invitaciones reales. Debe recibir acceso/secretos del owner por un canal seguro; no pegarlos en chats o commits. |
| Token/variables Sonar | Solo si Sonar se habilita; un administrador las configura como GitHub Secret/Variables. |
| GitHub Actions Secrets/Variables | Permanecen en Settings del repositorio, no viajan en Git. Configurarlas con permisos mínimos. |
| Tenant Auth0 | Configuración externa: aplicaciones, API, connection, grants, callbacks y permisos. Requiere acceso autorizado. |
| Volúmenes Docker | No viajan; Esteban crea volúmenes locales. No copiar bases ni borrar volúmenes ajenos. |
| Identidades Auth0 reales | No viajan ni deben clonarse; pertenecen al tenant. |
| IDE / `.sonarlint` | No son entrega funcional; regenerar localmente y excluir del commit. |
| `node_modules` | No; regenerar con `npm ci`. Nunca Git. |
| `target` | No; Maven lo regenera. Nunca Git. |
| `dist` / `coverage` | No; Angular/CI los regenera. Nunca Git. |
| Logs locales | No; contienen diagnóstico potencialmente sensible. No compartir sin sanitizar. |

## 6. Dependencias de Esteban

### Software que debe instalar

- GitHub Desktop.
- Docker Desktop con el plugin `docker compose`.
- Java 21 si ejecutará Maven fuera de Docker.
- Node.js 24 y npm 10.9.9 para coincidir con CI.
- Un navegador moderno.
- IDE opcional, por ejemplo IntelliJ IDEA o VS Code.

Maven global no es obligatorio: backend y Gateway traen `mvnw.cmd` y descargan
Maven 3.10.0. Los archivos Maven Wrapper, POM, npm lockfile, Compose y
configuración ya vienen por Git.

### Accesos externos

- acceso de lectura/escritura al repositorio según su rol;
- acceso al tenant Auth0 solo si probará invitaciones reales;
- permisos de GitHub Code Security para publicar resultados CodeQL, si el plan
  los exige;
- configuración administrativa de Sonar solo si el equipo decide habilitarlo.

## 7. Configuración local

No copie un `.env` ajeno. Cree `.env` en la raíz, agregue únicamente estas
variables y manténgalo fuera de Git.

### Requeridas por el Compose base

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
AUTH0_ISSUER
AUTH0_AUDIENCE
AUTH0_MANAGEMENT_DOMAIN
AUTH0_MANAGEMENT_CLIENT_ID
AUTH0_MANAGEMENT_CLIENT_SECRET
AUTH0_MANAGEMENT_AUDIENCE
AUTH0_DATABASE_CONNECTION
AUTH0_APPLICATION_CLIENT_ID
```

### Requeridas al levantar observabilidad

```text
GRAFANA_ADMIN_USER
GRAFANA_ADMIN_PASSWORD
```

### Opcionales/configurables con valor predeterminado

```text
AUTH0_MANAGEMENT_CONNECT_TIMEOUT
AUTH0_MANAGEMENT_RESPONSE_TIMEOUT
AUTH0_MANAGEMENT_TOKEN_EXPIRY_MARGIN
AUTH0_PROVISIONING_TIMEOUT
AUTH0_INVITATION_RESEND_COOLDOWN
OBSERVABILITY_TRACING_SAMPLING_PROBABILITY
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT
OBSERVABILITY_LOG_FILE
MANAGEMENT_SERVER_PORT
BACKEND_URL
SPRING_DOCKER_COMPOSE_ENABLED
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_REACTOR_CONTEXT_PROPAGATION
```

Las últimas variables son establecidas por Compose o tienen defaults de
desarrollo; no es necesario repetirlas en `.env` salvo que se ejecute un
componente fuera de Compose. El frontend actual no lee variables de entorno en
runtime: `src/environments/environment.ts` contiene configuración pública de
Auth0 y `apiUrl`; nunca se debe colocar allí un Client Secret.

Configuración externa de CI/Sonar:

```text
SONAR_TOKEN
SONAR_ENABLED
SONAR_HOST_URL
SONAR_PROJECT_KEY
```

## 8. Puesta en marcha en Windows PowerShell

### 8.1 Abrir y validar herramientas

```powershell
Set-Location 'C:\ruta\al\repositorio\My_Complex'
docker version
docker compose version
java -version
node --version
npm --version
```

### 8.2 Instalar frontend de forma reproducible

```powershell
Set-Location .\mycomplex-frontend
npm ci --no-audit --no-fund
Set-Location ..
```

### 8.3 Levantar el sistema base

```powershell
docker compose config
docker compose up -d --build
docker compose ps
```

### 8.4 Levantar base más observabilidad

```powershell
docker compose -f compose.yaml -f observability/compose.observability.yaml config
docker compose -f compose.yaml -f observability/compose.observability.yaml up -d --build
docker compose -f compose.yaml -f observability/compose.observability.yaml ps
```

### 8.5 Confirmar Flyway V1–V9

El contenedor ya recibe las variables definidas por Compose, por lo que no es
necesario imprimir sus valores en PowerShell:

```powershell
docker compose exec postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'
```

La salida debe contener V1 a V9 con `success=true`; V2 debe aparecer aunque no
tenga SQL funcional. No ejecute `repair` ante una diferencia.

### 8.6 Salud de backend y Gateway

```powershell
docker compose exec backend wget -q -O - http://localhost:9080/actuator/health
docker compose exec gateway wget -q -O - http://localhost:9081/actuator/health
```

El API público pasa por el Gateway en `http://localhost:8081/api/**`; el backend
no publica su puerto de aplicación al host.

### 8.7 Frontend y recorrido funcional

En otra terminal:

```powershell
Set-Location 'C:\ruta\al\repositorio\My_Complex\mycomplex-frontend'
npm start
```

Abra `http://localhost:4200`, inicie sesión mediante Auth0, seleccione una
copropiedad donde el usuario sea `PROPERTY_ADMIN` y entre a
`/properties/{propertyId}/users`. No envíe una invitación real sin autorización
y configuración M2M correcta.

### 8.8 Observabilidad

- Grafana: `http://127.0.0.1:3000`
- Prometheus targets: `http://127.0.0.1:9090/targets`
- Prometheus rules: `http://127.0.0.1:9090/rules`
- Loki ready: `http://127.0.0.1:3100/ready`
- Tempo ready: `http://127.0.0.1:3200/ready`
- Alloy: `http://127.0.0.1:12345`

En Grafana abra `MyComplex Overview`. Para Loki use en Explore:

```text
{service_name=~"mycomplex-(backend|gateway)"}
```

Para Tempo invoque una ruta por el Gateway y busque la traza. Con sampling
predeterminado de 10 % no todos los requests se almacenan; para demostración
local puede configurarse temporalmente una probabilidad de `1.0` en la sesión.

### 8.9 Detener sin borrar datos

```powershell
docker compose -f compose.yaml -f observability/compose.observability.yaml down
```

No use `down -v`: elimina bases, métricas, logs y trazas locales.

## 9. Solución rápida

| Problema | Acción |
|---|---|
| `ng` no reconocido | Use `npm start` o `npm exec ng`; no requiere Angular CLI global. |
| Dependencias npm ausentes/inconsistentes | Confirme Node 24 y ejecute `npm ci --no-audit --no-fund` dentro de `mycomplex-frontend`. |
| Docker Desktop apagado | Inicie Docker Desktop y espere a que `docker version` muestre cliente y servidor. |
| Variable no definida | Revise solo nombres contra esta guía/Compose y agréguela al `.env` local; no imprima ni publique valores. |
| Grafana rechaza credenciales | Confirme `GRAFANA_ADMIN_USER` y `GRAFANA_ADMIN_PASSWORD`. Un volumen existente conserva el usuario inicial; no borre el volumen sin autorización. |
| Contenedor `unhealthy` | Use `docker compose ps` y `docker compose logs <servicio>`; no publique logs sin sanitizar secretos/PII. |
| Error Flyway | Compare `flyway_schema_history` con V1–V9 y checksums. Deténgase; no edite migraciones, historial ni ejecute `repair`. |
| Puerto ocupado | Identifique el proceso que usa 5433, 8081, 3000, 9090, 3100, 3200, 12345 o 4200. No cambie puertos versionados sin coordinar. |
| Auth0 no configurado | Listado local puede funcionar con JWT válido, pero invitaciones requieren Management API, connection y grants. Coordine con el owner; no cree identidades de prueba sin permiso. |
| Grafana vacío al inicio | Genere tráfico por Gateway, espere el scrape de 15 s y recuerde el sampling de trazas. Compruebe `/targets`, Alloy y datasources. |

## 10. CI en la rama de Esteban

El CI viaja con `.github/workflows/ci.yml`. Se ejecuta en:

- `push` a `main` o `feature/**`;
- `pull_request` dirigido a `main`;
- ejecución manual `workflow_dispatch`.

La rama temporal recomendada comienza por `feature/`, por lo que cada push
dispara el workflow automáticamente. Una rama con otro prefijo necesitaría
`workflow_dispatch` o un pull request dirigido a `main` para ejecutar este
workflow.

Jobs:

1. `backend`: PostgreSQL 17, Maven verify, pruebas, arquitectura, cobertura y
   SBOM.
2. `gateway`: pruebas, cobertura y SBOM.
3. `frontend`: `npm ci`, pruebas/cobertura y build production.
4. `docker`: espera los tres anteriores, valida Compose, construye imágenes,
   escanea con Trivy y genera SBOM.
5. `repository-security`: espera Maven, restaura/puebla caché, ejecuta Gitleaks,
   Trivy filesystem y SBOM frontend.
6. `codeql`: matrices Java/Kotlin y JavaScript/TypeScript.
7. `sonar`: opcional, depende de backend/Gateway/frontend y configuración
   externa.

CodeQL requiere `security-events: write` y que el plan/configuración de GitHub
permita Code Scanning. Dependabot usa `.github/dependabot.yml` y crea PR
semanales; no necesita que el desarrollador instale nada. Sonar requiere
`SONAR_TOKEN`, `SONAR_ENABLED`, `SONAR_HOST_URL` y `SONAR_PROJECT_KEY`; si no se
habilita, `skipped` es correcto.

Interpretación:

- verde: el job concluyó correctamente;
- rojo: revisar step y artefacto; el gate no está aprobado;
- omitido: condición falsa, esperado únicamente en controles opcionales como
  Sonar;
- cancelado: `concurrency.cancel-in-progress` pudo reemplazar una ejecución por
  un push más reciente; revise la nueva ejecución.

Si Trivy encuentra HIGH/CRITICAL, identifique dependencia/imagen, actualice de
forma compatible o documente el riesgo y coordine; no use `ignore-unfixed`, no
baje severidad y no cambie `exit-code`. Ante un `429` externo de Maven, confirme
que el job restaura cache y ejecuta `dependency:go-offline`; reintente más tarde
solo cuando sea claramente throttling, sin bucles ni esperas de 1800 segundos.

Nunca se debe “poner verde” el CI eliminando pruebas, bajando cobertura,
deshabilitando scanners, usando `continue-on-error` en el gate final,
excluyendo POM/lockfiles, ocultando secretos o aceptando globalmente un hallazgo.

## 11. Riesgos específicos de integración con Noticias

1. **V4 ya existe y crea `noticias`:** riesgo alto de checksum, tabla duplicada
   o mismo número con contenido diferente.
2. **Modelo modular:** Noticias no debe importar repositories internos de
   `users` para autor/copropriedad ni apropiarse de unidades de `properties`.
3. **Blob Storage:** todavía no hay adapter Azure implementado en esta rama. Si
   Esteban lo añadió, debe conservar el binario fuera de PostgreSQL y mantener
   metadatos/ruta en `noticias`, sin secretos en código.
4. **Rutas/dashboard Angular:** ambos trabajos pueden modificar los mismos
   archivos de navegación.
5. **POM/lockfiles:** probables conflictos por dependencias de almacenamiento,
   frontend o pruebas.
6. **Auditoría:** publicar, editar o inactivar una noticia administrativa debe
   integrarse con la API de observabilidad sin acoplar `observability` a `news`.
7. **CI/Compose:** no reemplazar configuración nueva con versiones antiguas de
   la rama de Noticias.
8. **Documentación de seguridad:** actualizar el estado de controles sin
   declarar certificación OWASP/ASVS/Zero Trust.
