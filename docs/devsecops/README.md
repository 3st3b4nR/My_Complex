# Integración continua y DevSecOps

Estado: implementado localmente, pendiente de validación en GitHub Actions.

Esta fase implementa integración continua y controles DevSecOps. No implementa
despliegue continuo, publicación de imágenes ni autenticación contra Azure.

## Flujo de la canalización

```text
push / pull request / ejecución manual
  ├─ Backend: verify → pruebas → Modulith/ArchUnit → JaCoCo → JAR/SBOM
  ├─ Gateway: verify → pruebas → JaCoCo → JAR/SBOM
  ├─ Frontend: npm ci → Vitest/V8 → build de producción
  ├─ Docker: compose config → build local de backend y gateway
  ├─ CodeQL: Java/Kotlin y JavaScript/TypeScript
  ├─ Repositorio: Gitleaks → Trivy dependencias/configuración
  ├─ Imágenes: Trivy backend/gateway → SBOM CycloneDX
  └─ Sonar: análisis opcional → Quality Gate
```

Los jobs de compilación, calidad y seguridad están separados para que cada
fallo tenga una causa identificable. Ningún job hace `push` de imágenes, login
en registros, despliegue o modificación de infraestructura.

## Workflow existente fortalecido

La Fase 3 reutiliza el workflow de CI que ya existía. Se conservaron sus
validaciones de backend, gateway y construcción local de imágenes; se
completaron sus puertas y se integraron los controles de seguridad como jobs
del mismo `.github/workflows/ci.yml`. No se creó una segunda canalización.

### `.github/workflows/ci.yml`

- `backend`: Java 21, Maven Wrapper, PostgreSQL 17 de pruebas, suite completa,
  Spring Modulith, ArchUnit, JaCoCo, verificación de cobertura y SBOM.
- `gateway`: Java 21, Maven Wrapper, suite completa, JaCoCo, verificación de
  cobertura y SBOM.
- `frontend`: Node 24, `npm ci`, 49 pruebas Vitest en jsdom, cobertura V8 y
  build Angular de producción.
- `docker`: valida el Compose canónico y construye localmente las imágenes de
  backend y gateway; sobre esas mismas imágenes ejecuta Trivy y genera SBOM.
  Construir no equivale a publicar ni desplegar.
- `repository-security`: Gitleaks examina el historial con redacción total y
  Trivy analiza dependencias, lockfiles y configuración.
- `codeql`: analiza `java-kotlin` y `javascript-typescript` con consultas
  `security-extended`.
- `sonar`: se omite hasta que el equipo habilite explícitamente Sonar.
- Los hallazgos `HIGH` y `CRITICAL` hacen fallar la puerta correspondiente.
- Se generan SBOM CycloneDX para frontend e imágenes. Los POM generan las SBOM
  de backend y gateway.

Los reportes se retienen durante siete días. No se incluyen `.env`, tokens,
credenciales, `node_modules`, `target`, `dist` ni coberturas en Git.

## Versiones de la línea base

| Herramienta | Versión configurada |
|---|---:|
| Java | 21 |
| Maven Wrapper | 3.10.0 |
| Spring Boot | 4.1.2 |
| Node.js en CI | 24 |
| Angular | 22.2.3 |
| Vitest | 5.0.5 resuelto por lockfile |
| JaCoCo | 0.8.14 |
| CycloneDX Maven | 2.9.1 |
| Trivy Action / scanner | 0.36.0 / 0.76.1 |
| Gitleaks | 8.30.1 |
| CodeQL Action | v4 |

Las acciones versionadas se mantienen mediante Dependabot. Las actualizaciones
mayores requieren revisión humana y nunca se fusionan automáticamente.

## Cobertura y umbrales

La línea base fue medida antes de definir las puertas.

| Componente | Líneas | Ramas | Umbral de líneas | Umbral de ramas |
|---|---:|---:|---:|---:|
| Backend | 79,58 % | 56,14 % | 79 % | 56 % |
| Gateway | 95,24 % | 83,33 % | 95 % | 83 % |
| Frontend, todo `src/app` | 79,48 % | 69,43 % | 79 % | 69 % |

Frontend también exige 77 % de statements y 74 % de funciones. Los umbrales
son una línea base, no una meta final: evitan regresiones sin fingir una
cobertura superior a la realmente medida.

Exclusiones justificadas:

- Clases Java `*Application`: bootstrap técnico sin reglas de negocio.
- Implementaciones `*MapperImpl`: código generado por MapStruct.
- Especificaciones frontend: son pruebas, no código productivo.
- Directorios generados `target`, `dist`, `node_modules` y `coverage`.

No se excluyen servicios, dominio, controladores, adaptadores ni componentes de
negocio.

## Ejecución local

Los siguientes valores son exclusivamente de prueba. No se debe cargar `.env`
para ejecutar las validaciones.

### Backend

Con PostgreSQL de pruebas disponible:

```text
cd mycomplex-backend
./mvnw verify
```

JaCoCo queda en `target/site/jacoco` y la SBOM en
`target/classes/META-INF/sbom/application.cdx.json`.

### Gateway

```text
cd mycomplex-gateway
./mvnw verify
```

### Frontend

```text
cd mycomplex-frontend
npm ci
npm run test:ci
npm run build -- --configuration production
```

La cobertura queda en `coverage/mycomplex-frontend`, incluyendo `lcov.info`.

### Compose e imágenes

Con variables locales de prueba definidas en la sesión:

```text
docker compose config --quiet
docker compose build backend gateway
```

Estos comandos no levantan servicios ni publican imágenes.

### Gitleaks

```text
docker run --rm -v "<repositorio>:/repo" ghcr.io/gitleaks/gitleaks:v8.30.1 git /repo --redact=100 --no-banner
```

La opción `--redact=100` es obligatoria para no imprimir un secreto detectado.

### Trivy

```text
trivy fs --scanners vuln,misconfig --severity HIGH,CRITICAL .
trivy image --severity HIGH,CRITICAL mycomplex-backend:local
trivy image --severity HIGH,CRITICAL mycomplex-gateway:local
```

Si Trivy no está instalado localmente, GitHub Actions usa la acción fijada en
el workflow. Un escaneo no ejecutado debe reportarse como pendiente, nunca como
aprobado.

## SonarQube o SonarCloud

Sonar no estaba conectado al repositorio. El job permanece omitido mientras la
variable `SONAR_ENABLED` no sea `true`; el resto de la CI funciona sin Sonar.

Un administrador debe configurar:

| Tipo | Nombre | Contenido |
|---|---|---|
| Secret | `SONAR_TOKEN` | Token creado por Sonar para este proyecto |
| Variable | `SONAR_ENABLED` | `true` para habilitar el job |
| Variable | `SONAR_HOST_URL` | URL de SonarQube o SonarCloud |
| Variable | `SONAR_PROJECT_KEY` | Identificador real del proyecto |

Después debe crear el proyecto en Sonar, asociar el repositorio y definir un
Quality Gate que cubra bugs, vulnerabilidades, deuda, duplicación y cobertura.
El workflow espera hasta cinco minutos por el resultado. No se declara ningún
Quality Gate como aprobado antes de una ejecución real.

## CodeQL y permisos de GitHub

CodeQL necesita `security-events: write`. En repositorios o planes donde GitHub
Code Scanning no esté habilitado, el análisis puede compilar correctamente y
fallar al publicar resultados. Un administrador debe habilitar Code Security o
documentar esa limitación. No se usa `pull_request_target` y los workflows no
exponen secretos a código proveniente de forks.

## Dependabot

Dependabot revisa semanalmente:

- Maven del backend.
- Maven del gateway.
- npm del frontend.
- GitHub Actions.
- Imágenes base de ambos Dockerfiles.

Las actualizaciones minor y patch compatibles se agrupan. Todas las pull
requests requieren revisión y ejecución completa de la CI.

## Artefactos

| Artefacto | Contenido |
|---|---|
| `backend-quality-reports` | Surefire, JaCoCo HTML/XML y SBOM JSON |
| `gateway-quality-reports` | Surefire, JaCoCo HTML/XML y SBOM JSON |
| `frontend-quality-reports` | cobertura HTML/LCOV y build Angular |
| `repository-security-reports` | Gitleaks redactado, Trivy y SBOM frontend |
| `image-security-reports` | Trivy y SBOM de imágenes backend/gateway |

## Interpretación de fallos

- `verify` falla: corregir primero pruebas, arquitectura o umbral; no omitirlas.
- Cobertura falla: agregar pruebas de comportamiento o justificar una revisión
  explícita de la línea base; no excluir clases de negocio.
- Gitleaks falla: revocar la credencial, eliminarla de la fuente y coordinar la
  limpieza segura del historial. No agregar una exclusión genérica.
- Trivy falla: identificar paquete o imagen, aplicar una actualización segura y
  repetir el escaneo. Si no existe corrección, registrar propietario, impacto y
  fecha de revisión.
- CodeQL falla: revisar el resultado en Security; no silenciar consultas sin una
  justificación concreta.
- Sonar se omite: comprobar `SONAR_ENABLED` y las tres configuraciones externas.
- Docker falla: revisar `compose.yaml` y los Dockerfiles; la CI no debe realizar
  login ni publicación para resolverlo.

## Controles pendientes

- Validación real de todos los jobs después de commit y push.
- Habilitación administrativa de GitHub Code Scanning si el plan lo requiere.
- Creación del proyecto y Quality Gate real en Sonar.
- Revisión y remediación de los primeros hallazgos remotos.
- Protección de rama basada en los checks aprobados por el equipo.

## CD y Azure: fase posterior

Esta canalización no implementa CD. Antes de desplegar se debe definir y aprobar:

1. Arquitectura Azure y ambientes.
2. Registro de imágenes y estrategia de identidad federada.
3. Administración de secretos.
4. Migraciones controladas y compatibilidad hacia atrás.
5. Aprobaciones, rollback y promoción entre ambientes.
6. Primer despliegue manual y validado en un ambiente de pruebas.

No deben añadirse credenciales Azure, `azure/login`, publicación a ACR ni pasos
de despliegue hasta completar esa definición.

## Cobertura de la rúbrica

Esta fase cubre CI, compilación, pruebas, cobertura, arquitectura, SAST,
detección de secretos, dependencias, infraestructura como código, análisis de
imágenes, SBOM, Dependabot, artefactos y preparación honesta de Sonar. No cubre
CD, Azure, observabilidad, WAF, broker, service mesh, data mesh o Kubernetes.
