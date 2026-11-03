# Arquitectura de observabilidad

```mermaid
flowchart LR
  Client[Browser / client] -->|X-Correlation-ID + JWT| Gateway
  Gateway -->|W3C trace context + X-Correlation-ID| Backend
  Backend --> PostgreSQL[(PostgreSQL + audit events)]
  Backend -->|observations| Resend[Resend]
  Backend -->|observations| Blob[Azure Blob / Azurite]
  Backend -->|observations| Firebase[Firebase FCM]
  Scheduler[Meeting reminder scheduler] -->|batch observation| Backend
  Prometheus -->|scrape :9081| Gateway
  Prometheus -->|scrape :9080| Backend
  Gateway -->|OTLP HTTP| Tempo
  Backend -->|OTLP HTTP| Tempo
  Gateway -->|ECS JSON files| Alloy
  Backend -->|ECS JSON files| Alloy
  Alloy --> Loki
  Grafana --> Prometheus
  Grafana --> Loki
  Grafana --> Tempo
```

## Decisiones

- Spring Boot genera JSON ECS; no existe encoder artesanal.
- Los logs se rotan a 10 MB, siete archivos y máximo 100 MB por servicio.
- Alloy lee volúmenes en solo lectura; no monta Docker socket.
- Management usa puertos internos separados para no publicar Prometheus en el
  Gateway.
- Sampling predeterminado: 10 %, configurable mediante
  `OBSERVABILITY_TRACING_SAMPLING_PROBABILITY`; local puede usar `1.0`.
- Propagación distribuida usa Micrometer Tracing/OpenTelemetry y W3C context.
- Prometheus conserva siete días/1 GB; Loki y Tempo, siete días.
- Grafana se provisiona desde archivos y no permite signup/anonymous access.
- Las rutas HTTP de todos los módulos actuales usan las métricas automáticas de
  Spring Boot. Noticias y reuniones se visualizan mediante su `uri` templada,
  sin introducir IDs en labels.
- Resend expone `resend.email.send`; imágenes de noticias exponen
  `azure.blob.news.image`; Firebase conserva `firebase.fcm.send`; el scheduler
  expone `mycomplex.meeting.reminder.batch`. Solo incluyen operación, tipo y
  resultado dentro de conjuntos acotados.
- Este despliegue sigue siendo local. No reemplaza Azure Monitor, Application
  Insights ni una estrategia productiva de retención y alertamiento.

## Auditoría

`users` consume `observability::api`. El use case valida una allowlist de
metadata, obtiene correlation ID mediante un output port y usa un adapter JDBC
de solo inserción. Las operaciones locales transaccionales insertan el evento
en la misma transacción. Invitaciones involucran Auth0: su evento se inserta
después del efecto externo y un fallo se eleva y contabiliza para reconciliación.
