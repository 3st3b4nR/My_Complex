# Operación local

## Arranque

1. Configure credenciales no reutilizadas para Grafana en variables de la
   sesión, nunca en Git.
2. Valide: `docker compose -f compose.yaml -f observability/compose.observability.yaml config`.
3. Inicie una vez con `up -d --build`.
4. Espere health checks y consulte `docker compose ... ps`.

## Comprobaciones

- Targets: `http://127.0.0.1:9090/targets` debe mostrar backend y gateway `UP`.
- Reglas: `http://127.0.0.1:9090/rules`.
- Grafana debe contener tres datasources y `MyComplex Overview`.
- Loki: en Explore use `{service_name=~"mycomplex-(backend|gateway)"}`.
- Tempo: invoque una ruta por Gateway y busque la traza por `traceId`.
- Tareas: consulte `mycomplex_tasks_created_total`,
  `mycomplex_task_assignments_total` y
  `mycomplex_task_status_changes_total`.
- FCM: consulte `mycomplex_fcm_deliveries_total`,
  `mycomplex_fcm_delivery_failures_total` y
  `mycomplex_fcm_delivery_retries_total`. La alerta y su runbook no requieren
  un envío real para validar la regla.

## Parada y datos

```text
docker compose -f compose.yaml -f observability/compose.observability.yaml down
```

No use `down -v`: los volúmenes contienen DB, métricas, logs y trazas. Esta fase
no autoriza eliminarlos. Para limpiar recursos temporales, identifique primero
el nombre exacto y solicite autorización.

## Limitaciones

- No hay Alertmanager ni receptor real.
- No hay alta disponibilidad, autenticación entre componentes ni TLS interno.
- El stack está ligado a loopback y es solo para desarrollo.
- Sampling no garantiza que cada request tenga una traza cuando es menor a 1.
