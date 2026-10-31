# Registro de riesgos de seguridad

Estado al 30 de octubre de 2026. Los riesgos se priorizan por el sistema actual
y se revisarán al definir el primer ambiente remoto. **Aceptado** significa que
el equipo conoce el residual; no significa que el control deje de ser necesario.

| ID | Riesgo / amenazas | Prob. | Impacto | Nivel | Control existente | Tratamiento pendiente | Owner sugerido | Fase / estado |
|---|---|---:|---:|---:|---|---|---|---|
| R-01 | Auditoría local sin operación remota completa (`TM-R-01`) | Media | Alto | Alto | Audit port, V9 append-only, eventos estructurados, correlation ID, alertas y runbooks | Alertmanager, receptor, control de acceso/retención remotos y respuesta operada | Backend + Security | Fase 5 / Mitigado parcialmente |
| R-02 | Abuso automatizado o saturación de APIs (`TM-D-01`, `TM-D-02`) | Alta | Medio | Alto | Cooldown de resend, idempotencia, timeouts | Rate limiting/WAF administrado, métricas y alertas | DevOps + Backend | Despliegue / Abierto |
| R-03 | Robo/replay de Bearer token (`TM-S-02`) | Media | Alto | Alto | Auth0 SDK, JWT corto según proveedor, doble validación | CSP/headers, política tenant, revocación y runbook; evaluar sender constraint solo si se justifica | Frontend + Auth0 owner | Antes de producción / Abierto |
| R-04 | Exceso o fuga de privilegios M2M Auth0 (`TM-E-03`) | Media | Alto | Alto | Secreto fuera del código y acceso solo backend | Scope mínimo, rotación, identidad por ambiente y evidencia de configuración | Auth0 owner | Antes de ambiente remoto / Abierto |
| R-05 | Exposición por excepción inesperada (`TM-I-01`) | Media | Alto | Alto | Handler seguro para errores conocidos | Fallback genérico, prueba de stack trace y configuración productiva | Backend | Próxima estabilización / Abierto |
| R-06 | Compromiso de supply chain (`TM-T-03`) | Media | Alto | Alto | Dependabot, CodeQL, Trivy, Gitleaks, SBOM y gates | Pinning/provenance, branch protection y SLA de remediación | DevOps + Maintainers | CI continuo / Mitigado parcialmente |
| R-07 | Caída o throttling de Auth0 (`TM-D-03`) | Media | Alto | Alto | Timeouts, cache M2M, 429 y estados recuperables | Runbook, métricas y política de retry acotada | Backend + Operations | Antes de producción / Mitigado parcialmente |
| R-08 | TLS, headers y CORS incorrectos en producción | Media | Alto | Alto | Configuración local restrictiva y defaults Spring | Baseline verificable por ambiente, HSTS/CSP y pruebas externas | DevOps + Frontend | Despliegue / Pendiente |
| R-09 | IDOR o escalación en endpoint futuro (`TM-I-02`, `TM-E-01`) | Baja | Alto | Medio | Servicios autorizan, repositorios acotan y tests negativos | Criterio obligatorio por PR y matriz de permisos | Backend | Continuo / Mitigado |
| R-10 | PII, tokens o secretos en logs futuros (`TM-I-04`) | Media | Alto | Alto | No se observa logging de tokens; Gitleaks redacta | Esquema con allowlist, masking, pruebas y acceso restringido | Security + Operations | Junto con R-01 / Abierto |
| R-11 | Carrera rompe invariante de membresía (`TM-T-01`, `TM-E-02`) | Baja | Alto | Medio | Locks, versión, transacciones y constraints | Mantener pruebas de concurrencia al ampliar casos de uso | Backend | Continuo / Mitigado |
| R-12 | Configuración real de Auth0 no cumple MFA/anti-bruteforce | Media | Alto | Alto | Proveedor externo adoptado; código valida tokens | Checklist firmado del tenant, políticas de ataque y responsables | Auth0 owner | Antes de producción / No verificado |

## Orden recomendado

1. R-01 y R-10 deben resolverse juntos: registrar eventos sin una política de
   minimización convertiría los logs en una nueva fuga.
2. R-02 y R-08 se cierran durante el diseño del borde; no deben resolverse con
   un limiter en memoria dentro del monolito.
3. R-04 y R-12 requieren evidencia del owner de Auth0, no cambios especulativos
   en el repositorio.
4. R-05 puede corregirse con bajo impacto en una fase específica, acompañada de
   pruebas que confirmen el contrato de errores.
5. R-03 no justifica introducir almacenamiento propio de tokens; primero deben
   configurarse correctamente SDK, CSP y políticas del proveedor.

## Criterios de revisión

Este registro debe revisarse cuando cambie el modelo de autenticación, se cree
un endpoint de escritura, se introduzcan archivos/imágenes, se defina Azure, se
publique una API o un scanner CI reporte un hallazgo `HIGH`/`CRITICAL`.
