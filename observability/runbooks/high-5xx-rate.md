# Tasa alta de 5xx

- **Síntoma:** `MyComplexHigh5xxRate` durante cinco minutos.
- **Dashboard:** MyComplex Overview → HTTP errors y Request latency.
- **Consultas:** `sum by (application,status) (rate(http_server_requests_seconds_count{status=~"5.."}[5m]))`; Loki `{service_name=~"mycomplex-(backend|gateway)"} | json | level="ERROR"`.
- **Posibles causas:** PostgreSQL/Auth0 no disponibles, constraint inesperado, timeout o regresión.
- **Diagnóstico:** identificar servicio/ruta, agrupar por código seguro y seguir `traceId` sin copiar PII.
- **Acción segura:** aislar la dependencia, aplicar rollback aprobado o degradación documentada.
- **Escalar:** si supera 10 %, afecta escrituras o persiste diez minutos.
- **No hacer:** no ocultar errores, bajar gates ni registrar payloads para “depurar”.
