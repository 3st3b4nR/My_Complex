# Fallo de auditoría

- **Síntoma:** `MyComplexAuditPersistenceFailures` o rollback de operación administrativa.
- **Dashboard:** MyComplex Overview → Administrative events y logs recientes.
- **Consultas:** `sum(increase(mycomplex_audit_rejected_total{reason="persistence"}[5m]))`; Loki `{service_name="mycomplex-backend"} | json | message=~".*audit.*"`.
- **Posibles causas:** PostgreSQL caído, constraint de V9, metadata rechazada o almacenamiento lleno.
- **Diagnóstico:** verificar DB/Flyway, constraint y correlation ID; confirmar si la operación fue revertida o si involucró Auth0.
- **Acción segura:** preservar evidencia, corregir disponibilidad/metadata y repetir solo una operación local reversible.
- **Escalar:** cualquier operación externa completada sin evento durable o más de un fallo.
- **No hacer:** no insertar manualmente eventos, desactivar constraints, ejecutar `repair` ni ignorar el fallo.
