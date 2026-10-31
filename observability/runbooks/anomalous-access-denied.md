# Accesos administrativos denegados anómalos

- **Síntoma:** `MyComplexAdministrativeAccessDeniedSpike`.
- **Dashboard:** MyComplex Overview → Administrative access denied.
- **Consultas:** `sum by (operation) (increase(mycomplex_administrative_access_denied_total[10m]))`; Loki `{service_name="mycomplex-backend"} | json | security.event="administrative_access_denied"`.
- **Posibles causas:** cliente desactualizado, rol suspendido, token de otro tenant o abuso.
- **Diagnóstico:** revisar operación, ventana temporal, trace/correlation ID y estado de membresía sin exponer identidad.
- **Acción segura:** validar configuración/rol; revocar sesión mediante el procedimiento Auth0 si existe evidencia de compromiso.
- **Escalar:** patrón sostenido, varias operaciones o indicios de acceso horizontal.
- **No hacer:** no revelar si un recurso de otra copropiedad existe ni registrar JWT/subject.
