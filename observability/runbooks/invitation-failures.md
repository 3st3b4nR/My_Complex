# Fallos repetidos de invitación

- **Síntoma:** `MyComplexInvitationFailures`.
- **Dashboard:** MyComplex Overview → Invitation failures.
- **Consultas:** `sum by (operation,reason) (increase(mycomplex_invitation_failures_total[10m]))`; Loki `{service_name="mycomplex-backend"} | json | security.event="invitation_failed"`.
- **Posibles causas:** 429, Auth0 no disponible, permisos M2M o configuración incorrecta.
- **Diagnóstico:** revisar reason permitido, estado durable local y salud Auth0; diferenciar identidad creada de correo aceptado.
- **Acción segura:** respetar cooldown y reintentar desde UI solo cuando el estado sea recuperable.
- **Escalar:** conflicto de ownership, permisos M2M o fallos durante más de 15 minutos.
- **No hacer:** no crear/eliminar identidades, reenviar masivamente ni imprimir correo, secret o token.
