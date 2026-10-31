# Backend caído

- **Síntoma:** `MyComplexBackendDown`; target `mycomplex-backend` en `DOWN`.
- **Dashboard:** MyComplex Overview → Backend availability.
- **Consultas:** `up{job="mycomplex-backend"}`; Loki `{service_name="mycomplex-backend"} | json | level="ERROR"`.
- **Posibles causas:** proceso detenido, PostgreSQL no saludable, Flyway falló, configuración inválida o falta de recursos.
- **Diagnóstico:** revisar `docker compose ps`, health check, logs recientes y estado de PostgreSQL; correlacionar el primer error por `traceId`/`correlationId`.
- **Acción segura:** corregir configuración no sensible o dependencia caída y reiniciar únicamente backend después de preservar evidencia.
- **Escalar:** si hay pérdida de datos, migración inconsistente, exposición de secretos o caída superior a 15 minutos.
- **No hacer:** no ejecutar Flyway `repair`, borrar volúmenes, imprimir variables ni modificar migraciones aplicadas.
