# Manual para integrar Gestión de tareas y FCM en tu rama de Noticias

Esteban, ya tienes en tu rama Gestión de usuarios, autorización por copropiedad, Auth0, Noticias, observabilidad base, auditoría durable, CI y controles de seguridad. No vuelvas a implementar ni a configurar desde cero esos componentes.

En este procedimiento incorporarás únicamente Gestión administrativa de tareas, notificaciones FCM, historial de tareas y sus extensiones indispensables. Harás todo en una rama temporal para proteger tu rama de Noticias y conservar intacto tu trabajo original.

La rama fuente inspeccionada es `feature/panel-administrativo-haider`, en el commit `35de57c0544637a24a6ad2c8de37f48e03653a3a`. Como el nombre exacto de tu rama no aparece entre las referencias conocidas, este manual usa `<rama-de-esteban>`: reemplázalo por el nombre real de tu rama de Noticias.

## 1. Qué vas a integrar

Vas a incorporar:

- Listado, detalle, búsqueda y filtros de tareas por copropiedad.
- Creación, edición, asignación, reasignación, cambio de estado y cancelación.
- Autorización `PROPERTY_ADMIN`, aislamiento por `propertyId` y control de versión optimista.
- Protección de tareas terminales.
- Tareas activas: `PENDING` e `IN_PROGRESS`.
- Historial de consulta: `COMPLETED` y `CANCELLED`, sin acciones inválidas.
- Configuración pública Firebase Web.
- Registro y desactivación de instalaciones mediante Firebase Installation ID.
- Service Worker, outbox durable, reintentos, invalidación de destinos y estado de entrega.
- Notificación push cuando asignas o reasignas una tarea.
- Pruebas backend y frontend de Tareas y Notificaciones.
- Extensiones de Tareas sobre auditoría, métricas, Grafana y Prometheus.
- Dependencias Firebase y correcciones de seguridad para Guava y `@grpc/grpc-js`.

No vas a incorporar un panel completo del trabajador. El trabajador solamente inicia sesión, selecciona la copropiedad y activa las notificaciones.

## 2. Qué ya tienes y no debes volver a integrar

Conserva lo que ya existe en tu rama:

- Gestión de usuarios y sus reglas de autorización y aislamiento.
- Auth0.
- Noticias.
- Observabilidad base: Prometheus, Grafana, Loki, Tempo y Alloy.
- Auditoría durable y `X-Correlation-ID`.
- CI, cobertura, CodeQL, Gitleaks, Trivy, SBOM y Sonar opcional.
- Estándares y documentación compartidos.

No fusiones completa `feature/panel-administrativo-haider`: contiene bloques anteriores de Usuarios, observabilidad, seguridad y CI que ya tienes y que pueden haber evolucionado junto con Noticias. Tampoco copies manualmente las carpetas `tasks` o `notifications`, porque dependen de contratos, migraciones, frontend, auditoría y configuración compartida.

## 3. Requisitos previos

Antes de comenzar, confirma que:

- Tu rama de Noticias esté publicada y actualizada en el mismo repositorio.
- No tengas cambios locales pendientes.
- Tengas GitHub Desktop y Docker Desktop funcionando.
- Tengas Java 21 disponible para el backend.
- Uses el Maven Wrapper 3.10.0 incluido en el repositorio.
- Tengas Node.js compatible con Angular 22 y npm 10.9.9.
- Tengas un navegador compatible con Service Workers y notificaciones.
- Tengas acceso al proyecto Firebase y a los valores públicos de su aplicación web.
- Recibas la cuenta de servicio Firebase por un canal seguro, nunca por un commit.

Firebase no requiere instalar una aplicación de escritorio adicional.

## 4. Confirma tu rama y un árbol limpio

1. Abre GitHub Desktop y selecciona el repositorio `My_Complex`.
2. Haz clic en **Fetch origin** para actualizar las referencias.
3. Abre **Current branch** y selecciona tu rama de Noticias, `<rama-de-esteban>`.
4. Si GitHub Desktop muestra **Pull origin**, actualiza tu rama antes de continuar.
5. Confirma en la pestaña **Changes** que no existan archivos modificados o sin seguimiento.
6. Abre **Repository > Open in Terminal** y calcula el punto común:

   ```powershell
   git merge-base "origin/<rama-de-esteban>" "origin/feature/panel-administrativo-haider"
   git show --no-patch --oneline (git merge-base "origin/<rama-de-esteban>" "origin/feature/panel-administrativo-haider")
   git log --oneline --left-right "origin/<rama-de-esteban>...origin/feature/panel-administrativo-haider"
   ```

7. Comprueba si ya tienes una implementación parcial:

   ```powershell
   git ls-tree -r --name-only "origin/<rama-de-esteban>" -- mycomplex-backend/src/main/java/com/mycomplex/tasks
   git ls-tree -r --name-only "origin/<rama-de-esteban>" -- mycomplex-backend/src/main/java/com/mycomplex/notifications
   git ls-tree -r --name-only "origin/<rama-de-esteban>" -- mycomplex-frontend/src/app/features/task-management
   git ls-tree -r --name-only "origin/<rama-de-esteban>" -- mycomplex-frontend/src/app/features/notifications
   ```

Si aparecen archivos de Tareas o Notificaciones que no esperabas, no continúes automáticamente. Compara primero esa implementación parcial con los commits de este manual.

## 5. Crea la rama temporal en GitHub Desktop

1. Mantén seleccionada tu rama de Noticias.
2. Abre **Branch > New branch**.
3. Escribe exactamente `feature/integration-news-tasks`.
4. Confirma que la nueva rama se cree desde `<rama-de-esteban>`.
5. Comprueba que **Current branch** muestre `feature/integration-news-tasks`.
6. No hagas cambios directamente sobre tu rama original de Noticias.

## 6. Verifica V10 y V11 antes de aplicar commits

En la rama fuente se verificaron estos valores:

| Migración | Git blob | SHA-256 |
|---|---|---|
| `V10__create_tasks_and_fcm_notifications.sql` | `3d28bdc393725b2da19c54fdeb5a1d4c9c9d30e3` | `FCF03757683E3A50E409AF9072C24299124839C803E545D750F26B4198D7DE4F` |
| `V11__add_los_cerezos_owner_email_recipient.sql` | `47ef3e8983ac3c26bdefd0d30c2f0272a9d68a15` | `DE63E90CE18F79D96B9EAA9905B2257DAAF930BCF568E3F8F052148C46427629` |

El Git blob es la comparación principal entre ramas porque no depende de la conversión local de finales de línea.

1. Comprueba si tu rama ya contiene V10 y V11:

   ```powershell
   git ls-tree -r --name-only "origin/<rama-de-esteban>" -- mycomplex-backend/src/main/resources/db/migration
   ```

2. Si tienes V10, confirma que corresponda exactamente a Tareas y FCM:

   ```powershell
   git rev-parse "origin/<rama-de-esteban>:mycomplex-backend/src/main/resources/db/migration/V10__create_tasks_and_fcm_notifications.sql"
   ```

   Debe devolver `3d28bdc393725b2da19c54fdeb5a1d4c9c9d30e3`.

3. Si tienes V11, confirma que sea idéntica:

   ```powershell
   git rev-parse "origin/<rama-de-esteban>:mycomplex-backend/src/main/resources/db/migration/V11__add_los_cerezos_owner_email_recipient.sql"
   ```

   Debe devolver `47ef3e8983ac3c26bdefd0d30c2f0272a9d68a15`.

4. Después de integrar, verifica los SHA-256 del checkout:

   ```powershell
   Get-FileHash mycomplex-backend/src/main/resources/db/migration/V10__create_tasks_and_fcm_notifications.sql -Algorithm SHA256
   Get-FileHash mycomplex-backend/src/main/resources/db/migration/V11__add_los_cerezos_owner_email_recipient.sql -Algorithm SHA256
   ```

No continúes si los nombres, contenidos, Git blobs o checksums son diferentes. No modifiques V1-V11, no elimines V2, no renombres migraciones aplicadas y no crees V12 durante esta integración.

Si aplicaste V11 en una base donde V10 no existía, detente antes de ejecutar Flyway. Esa base tiene riesgo de migración fuera de orden. No habilites `outOfOrder`, no ejecutes `repair` y coordina una solución segura con el equipo.

## 7. Aplica los commits uno por uno y en orden

Aplica exactamente esta cadena:

| Orden | Commit | Qué aporta |
|---:|---|---|
| 1 | `b0aedf204039b5568faa21c946a680c7c23f682c` | Implementación base de Tareas, FCM, V10, frontend y extensiones específicas de observabilidad |
| 2 | `b8774842ddd8d3d5adfabe9a96da1ec500cab4ac` | Puerto y adaptador de configuración Firebase para respetar Clean Architecture; pruebas y cobertura frontend |
| 3 | `634af53d3e9859118d758751f895abe7fe6103d0` | Cobertura backend de controllers, servicios, mappers, persistencia y dispatcher |
| 4 | `e5063c4e1ab20c3cb03e7dd3f62bb3de95dda62e` | Guava 33.7.2-jre y override `@grpc/grpc-js` 1.14.5 |
| 5 | `e0242fae3f554545d07e1209eef159cf4c141d6c` | Separación visual entre Tareas activas e Historial |

Para cada commit en GitHub Desktop:

1. Abre **Current branch** y localiza `feature/panel-administrativo-haider`.
2. Abre su pestaña **History**.
3. Busca el SHA corto indicado en la tabla.
4. Haz clic derecho sobre el commit y selecciona **Cherry-pick commit**.
5. Cuando GitHub Desktop solicite la rama de destino, selecciona `feature/integration-news-tasks`.
6. Confirma que el commit aparezca en el historial de la rama temporal antes de aplicar el siguiente.
7. Respeta el orden; cada corrección depende de la implementación anterior.

Commits condicionales:

- `4fd366531f8f302d73018e941dcbe78734b5516b`: no lo apliques si ya tienes la misma V11. Aplícalo únicamente si V11 no existe, nunca fue aplicada y el historial V10/V11 es seguro.
- `35de57c0544637a24a6ad2c8de37f48e03653a3a`: aplícalo solamente si tus pruebas todavía esperan 10 migraciones, 5 miembros o 5 relaciones. Revisa primero el diff porque modifica una prueba de Tareas y dos pruebas de Usuarios.

No apliques los commits condicionales automáticamente.

Comandos equivalentes, solo como referencia:

```powershell
git switch <rama-de-esteban>
git pull --ff-only origin <rama-de-esteban>
git switch -c feature/integration-news-tasks
git cherry-pick b0aedf204039b5568faa21c946a680c7c23f682c
git cherry-pick b8774842ddd8d3d5adfabe9a96da1ec500cab4ac
git cherry-pick 634af53d3e9859118d758751f895abe7fe6103d0
git cherry-pick e5063c4e1ab20c3cb03e7dd3f62bb3de95dda62e
git cherry-pick e0242fae3f554545d07e1209eef159cf4c141d6c
```

## 8. Resuelve los conflictos sin perder Noticias

Cuando GitHub Desktop muestre **Conflicts**, abre cada archivo y conserva los cambios válidos de ambos lados. No elijas todo **ours** o todo **theirs** cuando el archivo contenga Noticias y Tareas.

| Archivo o área | Conserva de tu rama | Incorpora de Tareas/FCM |
|---|---|---|
| `mycomplex-backend/pom.xml` | Dependencias de Noticias y configuración existente | Firebase Admin 9.11.0 y Guava 33.7.2-jre |
| `application.properties` | Auth0, datasource y observabilidad base | Propiedades `fcm.*`, sin secretos |
| V10/V11 | Tu historial aplicado | Solo migraciones idénticas y en orden seguro |
| `users::api` | Contratos actuales de Usuarios | `TaskAccessDirectory` y su adaptador mínimo |
| `observability::api` | Auditoría y métricas existentes | Eventos y métricas específicos de Tareas/FCM |
| `SecurityAuditEventType` | Eventos de Noticias y demás módulos | Eventos administrativos de Tareas |
| `compose.yaml` | Servicios actuales | Variables FCM sin eliminar servicios existentes |
| `compose.firebase.yaml` | Cualquier ajuste válido existente | Montaje de solo lectura de la cuenta de servicio |
| `package.json` | Dependencias de Noticias | Firebase 13.1.0 y override `@grpc/grpc-js` 1.14.5 |
| `package-lock.json` | Lock coherente de Noticias | Lock coherente con Firebase y el override seguro |
| `app.routes.ts` | Rutas de Noticias y Usuarios | Ruta protegida de Tareas |
| Dashboard y barra lateral | Accesos actuales | Acceso a Gestión de tareas |
| `firebase-messaging-sw.js` | Ajustes válidos existentes | Inicialización y recepción FCM sin secretos |
| Dashboard Grafana | Paneles actuales | Paneles específicos de Tareas/FCM |
| Alertas Prometheus | Alertas actuales | Alerta de fallos FCM |

Resuelve primero `package.json` y después regenera `package-lock.json` con npm 10.9.9. No combines manualmente fragmentos incompatibles del lockfile. No debilites Trivy ni agregues excepciones.

Aborta inmediatamente si encuentras V10/V11 distintas, una implementación parcial incompatible, pérdida de Noticias, una dependencia circular o cambios que obliguen a reescribir Usuarios u observabilidad base.

## 9. Agrega únicamente las variables FCM

No reemplaces tu `.env` completo. Conserva tus variables de PostgreSQL, Auth0, Grafana y observabilidad. Agrega únicamente:

| Variable | Requisito |
|---|---|
| `FCM_ENABLED` | Opcional; predeterminado `false`. El override Firebase lo fija en `true` para backend |
| `FIREBASE_PROJECT_ID` | Obligatoria para FCM real |
| `GOOGLE_APPLICATION_CREDENTIALS` | Obligatoria para FCM real; Compose la fija en `/run/secrets/firebase-service-account.json` |
| `FIREBASE_WEB_API_KEY` | Obligatoria para la app web |
| `FIREBASE_WEB_AUTH_DOMAIN` | Obligatoria para la app web |
| `FIREBASE_WEB_PROJECT_ID` | Obligatoria para la app web |
| `FIREBASE_WEB_STORAGE_BUCKET` | Requerida por la configuración web actual |
| `FIREBASE_WEB_MESSAGING_SENDER_ID` | Obligatoria para mensajería web |
| `FIREBASE_WEB_APP_ID` | Obligatoria para identificar la aplicación web |
| `FIREBASE_WEB_VAPID_KEY` | Obligatoria para suscripciones push |
| `FCM_MAX_RETRIES` | Opcional; predeterminado `3` |
| `FCM_RETRY_DELAY` | Opcional; predeterminado `1m` |
| `FCM_DISPATCH_INTERVAL` | Opcional; predeterminado `5s` |

No subas `.env`. Recibe los valores reales por un canal seguro. Las variables Firebase Web son configuración pública del cliente, pero no son la clave privada de la cuenta de servicio.

## 10. Guarda el JSON Firebase fuera del repositorio

1. Recibe el JSON de cuenta de servicio por un canal privado y controlado.
2. Guárdalo fuera de `My_Complex`.
3. No lo pegues en documentación, `.env`, issues, chats públicos ni logs.
4. No lo copies dentro de una imagen Docker.
5. Desde la carpeta raíz de `My_Complex`, abre PowerShell y configura:

   ```powershell
   $env:FIREBASE_CREDENTIALS_PATH = (
     Resolve-Path 'C:\ruta-segura\firebase-service-account.json'
   ).Path
   ```

`compose.firebase.yaml` montará el archivo como solo lectura en `/run/secrets/firebase-service-account.json`. Dentro del contenedor, `GOOGLE_APPLICATION_CREDENTIALS` apuntará a esa ruta interna.

## 11. Levanta la aplicación

Ejecuta desde la carpeta raíz de `My_Complex`:

```powershell
docker compose `
  -f compose.yaml `
  -f compose.firebase.yaml `
  -f observability/compose.observability.yaml `
  config --quiet

docker compose `
  -f compose.yaml `
  -f compose.firebase.yaml `
  -f observability/compose.observability.yaml `
  up -d --build

docker compose `
  -f compose.yaml `
  -f compose.firebase.yaml `
  -f observability/compose.observability.yaml `
  ps
```

PostgreSQL, backend y gateway deben quedar saludables. Confirma que tus servicios de observabilidad existentes sigan respondiendo; no necesitas reinstalarlos.

Después inicia Angular:

```powershell
Set-Location mycomplex-frontend
npm ci
npm start
```

## 12. Registra el navegador del trabajador

1. Usa o registra un miembro con rol `TRABAJADOR` y completa su cuenta Auth0.
2. Abre al trabajador en un navegador o perfil distinto al administrador.
3. Inicia sesión, selecciona la copropiedad y pulsa **Activar notificaciones**.
4. Confirma que el permiso quede en `granted`.
5. Confirma en las herramientas del navegador que `firebase-messaging-sw.js` esté activo.
6. Confirma `POST /api/me/fcm-installations` con HTTP `200` o `201`.

No expongas el Firebase Installation ID. Si reutilizas el mismo perfil para identidades distintas, puedes recibir HTTP `409`; usa perfiles o navegadores separados.

## 13. Prueba la asignación y la notificación

1. Mantén abierta la sesión del trabajador con notificaciones activas.
2. En otro perfil, inicia sesión como `PROPERTY_ADMIN`.
3. Abre Gestión de tareas para la misma copropiedad.
4. Crea una tarea, asígnala al trabajador y confirma que aparezca como `PENDING`.
5. Confirma la recepción de la notificación push en el perfil del trabajador.
6. Distingue los resultados: un outbox procesado o una aceptación de FCM no garantizan por sí solos que el navegador haya mostrado el mensaje.

## 14. Verifica el historial

1. Cambia la tarea de `PENDING` a `IN_PROGRESS` y confirma que permanezca en **Tareas activas**.
2. Completa o cancela la tarea.
3. Confirma que desaparezca de Tareas activas y aparezca en **Historial**.
4. Confirma que `COMPLETED` y `CANCELLED` no muestren edición, reasignación, reapertura ni cambios de estado.
5. Comprueba que Noticias y Gestión de usuarios continúen funcionando.

## 15. Ejecuta pruebas y comprueba GitHub Actions

Antes de publicar, verifica:

1. V10 y V11 correctas y sin duplicados.
2. Flyway V1-V11 aprobado.
3. Backend y frontend compilan y sus pruebas pasan.
4. Noticias y Usuarios no presentan regresiones.
5. Tareas, registro FCM, push e Historial funcionan.
6. Las extensiones de Tareas no rompen tu observabilidad existente.
7. Trivy no reporta vulnerabilidades `HIGH` o `CRITICAL`.

No reduzcas estos umbrales:

- Backend: líneas 79 %, ramas 56 %.
- Frontend: statements 77 %, branches 69 %, functions 74 %, lines 79 %.

Después:

1. Regresa a GitHub Desktop y confirma `feature/integration-news-tasks`.
2. Haz clic en **Publish branch**.
3. Abre **Actions** en GitHub y selecciona la ejecución de la rama temporal.
4. Espera que todos los jobs terminen en verde.
5. No desactives CodeQL, Gitleaks, Trivy, SBOM, cobertura ni Sonar opcional para conseguir una aprobación artificial.

## 16. Qué hacer si algo falla

- Si un cherry-pick presenta conflictos, resuelve únicamente los archivos afectados.
- Si no puedes conservar simultáneamente Noticias y Tareas, aborta y coordina antes de reintentar.
- Si V10/V11 difieren o V11 fue aplicada antes de V10, detente y no ejecutes Flyway.
- Si Firebase está deshabilitado, revisa solo las variables nuevas y la ruta del JSON; no reemplaces todo tu `.env`.
- Si recibes HTTP `409`, usa un perfil diferente para el trabajador.
- Si FCM acepta el mensaje pero no aparece el push, revisa permiso, Service Worker y perfil antes de modificar backend.
- Si Trivy falla, conserva Guava 33.7.2-jre y `@grpc/grpc-js` 1.14.5; no agregues excepciones.
- Si CI falla, corrige únicamente la rama temporal.

Para abortar un cherry-pick desde terminal:

```powershell
git cherry-pick --abort
```

Si la rama temporal queda dudosa, conserva intactas tus ramas originales y recrea únicamente `feature/integration-news-tasks` desde tu rama de Noticias.

## 17. Integra la rama temporal únicamente cuando esté aprobada

Integra `feature/integration-news-tasks` en tu rama definitiva solo cuando:

- V10 y V11 estén verificadas y sean seguras para tu base.
- Los cinco commits obligatorios estén presentes en orden.
- Los commits condicionales se hayan aplicado u omitido justificadamente.
- Noticias y Usuarios no tengan regresiones.
- Tareas, FCM e Historial superen el smoke test.
- `package-lock.json` sea coherente y conserve `@grpc/grpc-js` 1.14.5.
- El backend conserve Guava 33.7.2-jre.
- Las extensiones de auditoría, métricas, Grafana y Prometheus estén presentes.
- GitHub Actions esté completamente aprobado.

Hasta cumplir todos estos puntos, mantén tu rama definitiva sin cambios y trabaja únicamente en la rama temporal.
