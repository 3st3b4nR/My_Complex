# Evaluación OWASP Top 10:2025

Esta matriz usa las categorías oficiales de
[OWASP Top 10:2025](https://top10.owasp.org/2025/). El estado expresa cobertura
en el repositorio, no cumplimiento certificado.

| Categoría | Aplicabilidad | Estado | Controles y evidencia | Brecha y riesgo residual | Acción / fase |
|---|---|---|---|---|---|
| A01 Broken Access Control | Alta | Partial | JWT, `PROPERTY_ADMIN`, scope por `propertyId` y pruebas negativas (E-01, E-02, E-04, E-11, E-12) | Un endpoint futuro puede omitir política; falta auditoría. Residual Medio | Tests negativos y audit port / Fase 5 |
| A02 Security Misconfiguration | Alta | Partial | Stateless, Basic/form deshabilitados, CORS allowlist y Actuator limitado (E-01, E-02, E-16) | Headers/TLS de producción no verificables. Residual Medio | Baseline y pruebas HTTP / Despliegue |
| A03 Software Supply Chain Failures | Alta | Partial | Dependabot, lockfiles, CodeQL, Gitleaks, Trivy, SBOM y gates (E-14) | Pinning/provenance y branch protection no verificados. Residual Medio | Política de remediación y workflow endurecido / CI continuo |
| A04 Cryptographic Failures | Alta | Partial | HTTPS hacia Auth0 y validación JWT por Spring Security/Auth0 (E-01, E-02, E-08) | TLS, at-rest y rotación remotos no existen. Residual Medio | Definirlos con Azure / Despliegue |
| A05 Injection | Alta | Implemented | Bean Validation, UUID/enums, JPQL/JDBC parametrizado y CodeQL (E-05, E-06, E-10, E-14) | Puede reaparecer con consultas dinámicas. Residual Bajo | Revisión y pruebas por nueva entrada / Continuo |
| A06 Insecure Design | Alta | Partial | Puertos, tenant isolation, transacciones, idempotencia y último admin (E-05 a E-13) | Antiabuso y auditoría no están implementados. Residual Medio | Registro de riesgos como criterio / Fase 5 |
| A07 Authentication Failures | Alta | Partial | Auth0 OIDC/SDK, issuer/audience, linking por `sub` y conexión exacta (E-01, E-02, E-08, E-15, E-16) | MFA y defensa de credenciales del tenant no verificadas. Residual Medio | Checklist del owner Auth0 / Antes de producción |
| A08 Software or Data Integrity Failures | Alta | Partial | Locks, rollback, Flyway inmutable, estado durable de invitación y CI (E-05 a E-07, E-11, E-14) | Provenance y backups no verificados. Residual Medio | Pinning/provenance y backup / Despliegue |
| A09 Security Logging and Alerting Failures | Alta | Not Implemented | Correlation ID, UTC y logs técnicos son solo base parcial (E-03, E-16) | Sin eventos, auditoría, retención ni alertas. Residual Alto | Audit port y observabilidad, expresamente pendientes / Fase 5 |
| A10 Mishandling of Exceptional Conditions | Alta | Partial | Handler seguro conocido, timeouts, 429, estado recuperable y rollback (E-07 a E-09, E-11, E-13) | Excepción inesperada/degradación no verificadas E2E. Residual Medio | Fallback genérico, pruebas y runbooks / Estabilización |

## Controles transversales que deben mantenerse

- El Gateway no contiene reglas de negocio; el backend no confía en que el
  Gateway haya autorizado la operación.
- CSRF permanece deshabilitado solo mientras la API sea stateless y el JWT se
  transporte en `Authorization`, sin autenticación por cookie.
- CORS no es autorización: únicamente reduce llamadas de navegadores desde
  origins no aprobados.
- Un WAF futuro no convierte entradas en confiables ni corrige IDOR.
- Los estados de integración se persisten antes/después de efectos externos
  para que una falla pueda reconciliarse sin duplicar identidades.

Los identificadores E-xx apuntan al catálogo de rutas exactas de
[`README.md`](README.md#catálogo-de-evidencia-verificable).
