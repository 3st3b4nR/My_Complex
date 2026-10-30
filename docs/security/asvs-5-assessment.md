# Evaluación OWASP ASVS 5.0.0

Objetivo: línea base **Level 1** para la aplicación web, complementada con
controles L2/L3 directamente relevantes al diseño actual. Esta es una
evaluación selectiva y trazable, no una afirmación de conformidad completa.

Los textos se resumen; la referencia normativa es
[OWASP ASVS 5.0.0](https://github.com/OWASP/ASVS/tree/v5.0.0/5.0/en). Los
identificadores están fijados a la versión para evitar cambios semánticos.

## Controles evaluados

| Control | Nivel | Estado | Evaluación y evidencia | Brecha / acción |
|---|---:|---|---|---|
| `v5.0.0-1.2.4` | L1 | VERIFICADO | Consultas JDBC y JPA usan parámetros; los `IN` dinámicos solo generan placeholders | Mantener prohibición de concatenar valores de usuario |
| `v5.0.0-2.2.1` | L1 | VERIFICADO | DTO REST aplican `@Valid`, tamaños, email, UUID y enums | Agregar límites a todo DTO futuro |
| `v5.0.0-2.2.2` | L1 | VERIFICADO | Servicios y adapters vuelven a validar reglas y pertenencia; no dependen de la validación Angular | Mantener validación en el límite confiable |
| `v5.0.0-2.3.3` | L2 | VERIFICADO | Registro, edición, desvinculación e invitación usan transacciones y rollback | Control suplementario al baseline L1 |
| `v5.0.0-2.3.4` | L2 | VERIFICADO | `@Version`, locks de propiedad/membresía y pruebas de stale version | Extender a nuevas escrituras sensibles |
| `v5.0.0-2.4.1` | L2 | PARCIAL | Invitación tiene cooldown e idempotencia | Falta anti-automation general en borde |
| `v5.0.0-3.4.2` | L1 | VERIFICADO LOCAL | Gateway define origin, métodos y headers explícitos | Sustituir por allowlist exacta por ambiente, nunca wildcard con credenciales |
| `v5.0.0-3.4.1` | L1 | PENDIENTE | HSTS no puede verificarse sin HTTPS y dominio desplegado | Activar/probar en borde de producción después de arquitectura Azure |
| `v5.0.0-3.4.3` | L2 | NO VERIFICADO | No hay política CSP explícita inspeccionable | Diseñar con Auth0 y Angular; primero Report-Only |
| `v5.0.0-3.4.4` | L2 | PARCIAL | Spring Security aporta headers seguros por defecto | Crear prueba HTTP que confirme `X-Content-Type-Options` en el despliegue real |
| `v5.0.0-3.5.1` | L1 | VERIFICADO CON SUPUESTO | API stateless usa Bearer header y no cookie de autenticación; CSRF deshabilitado deliberadamente | Reabrir el control si se introduce autenticación por cookie |
| `v5.0.0-3.5.2` | L1 | VERIFICADO | Gateway permite preflight y aplica CORS a origins/métodos/headers definidos | Probar preflight en cada ambiente |
| `v5.0.0-4.1.1` | L1 | VERIFICADO EN ALCANCE | Controllers consumen/producen DTO JSON tipados mediante Spring MVC | Agregar pruebas negativas para content types futuros |
| `v5.0.0-6.1.1` | L1 | PARCIAL | Auth0 realiza autenticación y el código maneja límites de Management API | Rate limiting de autenticación depende del tenant y no fue verificado |
| `v5.0.0-6.3.1` | L1 | NO VERIFICADO | Protección contra credential stuffing/brute force pertenece a configuración Auth0 | Owner de Auth0 debe evidenciar controles y MFA/risk policy aplicables |
| `v5.0.0-6.8.2` | L2 | VERIFICADO | Spring OAuth2 Resource Server valida JWT firmado en Gateway y backend | Verificar JWKS/algoritmos aceptados en pruebas de ambiente |
| `v5.0.0-7.2.1` | L1 | VERIFICADO | El backend, límite confiable, valida el token antes de requests protegidos | Mantener validación aunque exista Gateway |
| `v5.0.0-7.2.2` | L1 | VERIFICADO POR PROVEEDOR | Los tokens provienen de Auth0; no existen tokens estáticos de sesión en código | Configuración real y expiración requieren evidencia del tenant |
| `v5.0.0-8.1.1` | L1 | PARCIAL | Roles y aislamiento están implementados y documentados en pruebas/servicios | Consolidar matriz de permisos al incorporar nuevos módulos |
| `v5.0.0-10.3.1` | L2 | VERIFICADO | `AUTH0_AUDIENCE` se configura en ambos Resource Servers | Probar rechazo de audience incorrecta en ambiente integrado |
| `v5.0.0-10.3.3` | L2 | VERIFICADO | Identidad se deriva del `sub` validado bajo issuer configurado; no del email del request | Mantener issuer por ambiente y unicidad local |
| `v5.0.0-10.3.5` | L3 | NO IMPLEMENTADO | Bearer tokens no están sender-constrained | Riesgo de replay aceptado provisionalmente; no es requisito L1 |
| `v5.0.0-12.1.1` | L1 | PENDIENTE | No hay endpoint remoto para verificar versiones/cifrados TLS | Definir baseline TLS en la fase Azure |
| `v5.0.0-12.2.1` | L1 | PARCIAL | Integración Auth0 usa HTTPS; tráfico local entre componentes usa HTTP | Cifrar tránsito de ambiente remoto según topología aprobada |
| `v5.0.0-15.1.1` | L1 | PARCIAL | Dependabot y scanners detectan hallazgos | Documentar SLA de remediación por severidad |
| `v5.0.0-15.1.2` | L2 | VERIFICADO | CI genera SBOM CycloneDX y usa repositorios/lockfiles del proyecto | Proteger y retener artefactos según política futura |
| `v5.0.0-15.2.1` | L1 | PARCIAL FUERTE | Trivy/CodeQL y gates `HIGH/CRITICAL` bloquean CI | Confirmar operación continua y proceso de excepciones |
| `v5.0.0-15.3.1` | L1 | VERIFICADO | Responses usan DTO/proyecciones y no serializan entidades completas | Revisar minimización por endpoint nuevo |
| `v5.0.0-15.3.3` | L2 | VERIFICADO | Requests usan DTO explícitos; el cliente no enlaza entidades persistentes | Mantener allowlist de campos |
| `v5.0.0-15.4.1` | L3 | VERIFICADO EN GESTIÓN DE USUARIOS | Locks, versión y constraints preservan integridad ante concurrencia | Repetir patrón por agregado futuro |
| `v5.0.0-16.2.1` | L2 | PENDIENTE | Correlation ID existe, pero no hay eventos de seguridad estructurados con quién/qué/resultado | Diseñar esquema de eventos sin PII/token |
| `v5.0.0-16.2.2` | L2 | PARCIAL | Aplicación configura UTC | Sincronización de hosts depende del ambiente futuro |
| `v5.0.0-16.3.2` | L2 | PENDIENTE | Las denegaciones funcionan, pero no se registran como evento de seguridad | Registrar y alertar denegaciones relevantes sin enumeración |
| `v5.0.0-16.5.1` | L2 | PARCIAL FUERTE | Handler entrega mensajes seguros para casos conocidos y Auth0; tests lo comprueban | Añadir fallback genérico y prueba de excepción inesperada |

## Resultado del baseline

- Los controles L1 verificables desde el repositorio están mayoritariamente
  cubiertos en validación, inyección, sesión stateless, JWT y minimización.
- No se puede cerrar L1 de punta a punta sin inspeccionar el tenant Auth0 y un
  ambiente HTTPS real.
- Los principales controles L2 pendientes son anti-automation y security
  logging. Se incluyeron porque reducen riesgos concretos del modelo, no para
  afirmar un nivel ASVS 2.
- Los controles L3 se muestran únicamente como información sobre replay y
  concurrencia; MyComplex no declara objetivo L3.

## Evidencias de prueba destacadas

- `PropertyMemberControllerTest`: usa el JWT subject y mapea denegaciones de
  acceso y errores de gestión a respuestas seguras.
- `PropertyMemberManagementIntegrationTest`: rechaza recursos foráneos,
  protege último administrador, versión y rollback.
- `UserPropertyJpaRepositoryTest`: prueba administrador activo, aislamiento
  por propiedad y enlaces terminados/eliminados.
- `InvitePropertyMemberServiceTest`: evita identidad duplicada, verifica
  ownership y conserva estado recuperable ante fallo de invitación.
- `UserInvitationPersistenceIntegrationTest`: autorización tenant, máquina de
  estados y cooldown.
- `.github/workflows/ci.yml`: pruebas, cobertura, CodeQL, Gitleaks, Trivy y SBOM.
