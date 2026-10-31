# Latencia alta

- **Síntoma:** `MyComplexHighLatency`; p95 superior a 1,5 s por diez minutos.
- **Dashboard:** MyComplex Overview → Request latency, JVM, threads y HikariCP.
- **Consultas:** `histogram_quantile(0.95, sum by (le,application) (rate(http_server_requests_seconds_bucket[5m])))`; revisar trazas lentas en Tempo.
- **Posibles causas:** pool DB saturado, consulta lenta, Auth0, GC o recursos locales.
- **Diagnóstico:** comparar Gateway/backend, pool, memoria y spans; ubicar dependencia lenta.
- **Acción segura:** reducir carga, restaurar dependencia o corregir consulta tras reproducirla.
- **Escalar:** si crece la cola Hikari, hay timeouts o la latencia persiste 20 minutos.
- **No hacer:** no aumentar timeouts/pools sin medir ni reiniciar PostgreSQL a ciegas.
