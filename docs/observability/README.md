# Observabilidad de MyComplex

Estado: stack local mínimo y funcional. No es una plataforma Azure ni una
solución de producción.

La fase incorpora métricas Prometheus, logs ECS en Loki, trazas OpenTelemetry
en Tempo, dashboard Grafana, alertas locales y auditoría durable append-only.

## Inicio rápido

Defina en la sesión local `GRAFANA_ADMIN_USER` y
`GRAFANA_ADMIN_PASSWORD`; no los escriba en archivos versionados. Después use:

```text
docker compose -f compose.yaml -f observability/compose.observability.yaml up -d --build
```

Interfaces locales:

- Grafana: `http://127.0.0.1:3000`
- Prometheus: `http://127.0.0.1:9090`
- Loki: `http://127.0.0.1:3100/ready`
- Tempo: `http://127.0.0.1:3200/ready`
- Alloy: `http://127.0.0.1:12345`

Todos los puertos están ligados a loopback. Los management ports `9080` y
`9081` solo se exponen en la red Docker.

## Documentos

- [Arquitectura](architecture.md)
- [Operación local](local-operations.md)
- [Eventos de seguridad y privacidad](security-events.md)
- [Runbooks](../../observability/runbooks/)

Las alertas se evalúan en Prometheus. No se envían notificaciones: Alertmanager
y un receptor requieren una decisión operativa futura.
