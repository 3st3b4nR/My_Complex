# Gateway caído

- **Síntoma:** `MyComplexGatewayDown`; la API pública local no responde.
- **Dashboard:** MyComplex Overview → Gateway availability.
- **Consultas:** `up{job="mycomplex-gateway"}`; Loki `{service_name="mycomplex-gateway"} | json | level="ERROR"`.
- **Posibles causas:** configuración JWT inválida, backend no saludable, conflicto de puerto o recursos agotados.
- **Diagnóstico:** revisar target, health check, logs y conectividad interna hacia `backend:8080`.
- **Acción segura:** restaurar la dependencia/configuración y reiniciar solo Gateway.
- **Escalar:** si hay sospecha de ataque, fallo de autenticación general o caída mayor a 15 minutos.
- **No hacer:** no desactivar validación JWT, CORS ni seguridad para recuperar servicio.
