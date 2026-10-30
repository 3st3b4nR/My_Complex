# Borde de aplicación y WAF

Estado: **Proposed**. No implementado ni aprobado como arquitectura Azure.

## Contexto

MyComplex solo tiene un entorno local canónico. El Gateway enruta `/api/**`,
valida JWT y aplica CORS, pero no debe asumir responsabilidades propias de un
borde público administrado: terminación TLS, mitigación volumétrica, reglas WAF
y rate limiting global.

## Decisión propuesta

Cuando el equipo defina el despliegue en Azure, evaluar **Azure Front Door con
Web Application Firewall** delante del frontend y del Gateway. El origen debe
aceptar tráfico únicamente desde el borde aprobado cuando la plataforma elegida
lo permita.

```text
Internet → Azure Front Door + WAF → frontend / Gateway → backend → PostgreSQL
                                  ↘ observabilidad de seguridad futura
```

La selección definitiva depende de la arquitectura cloud, costos, dominios,
certificados, ambientes y requisitos de disponibilidad. Este documento no crea
recursos, políticas ni compromiso de licenciamiento.

## Responsabilidades propuestas

- TLS y certificados del dominio público.
- Reglas administradas OWASP, inicialmente en modo detección.
- Rate limiting por ruta y señales técnicas; límites funcionales se definirán
  con métricas reales.
- Restricción geográfica o de bots solo si surge un requisito verificable.
- Correlation ID y envío de telemetría sin almacenar tokens ni datos sensibles.
- Protección particular de login/redirecciones e invitaciones sin interferir
  con el flujo OIDC de Auth0.

## Lo que el WAF no reemplaza

- Validación de JWT en Gateway y backend.
- Autorización `PROPERTY_ADMIN` y aislamiento por `propertyId`.
- Bean Validation, consultas parametrizadas y transacciones.
- Protección del último administrador, versiones e idempotencia.
- Auditoría funcional y registro de eventos de seguridad.
- Validación de archivos cuando esa funcionalidad exista.

## Riesgos mitigados parcialmente

- Patrones comunes de inyección y requests malformados.
- Enumeración y automatización básica.
- Abuso de endpoints costosos o de invitación.
- Exposición directa accidental de origins.

No corrige IDOR, permisos de negocio, secretos expuestos en código, fraude con
una cuenta legítima ni vulnerabilidades lógicas.

## Alternativas consideradas

| Alternativa | Decisión | Motivo |
|---|---|---|
| Solo Gateway Spring | Rechazada como borde público | Mezcla lógica de aplicación con mitigación perimetral y no absorbe ataques volumétricos |
| Rate limiter propio en memoria | Rechazada | Estado no compartido, operación compleja y falsa sensación de protección |
| Azure API Management | No decidido | Puede ser útil para gobierno de APIs, pero no hay requisito ni presupuesto aprobados |
| Front Door + WAF | Candidata | Integra borde global, TLS y WAF sin introducir lógica de negocio |

## Costos y trade-offs

- Costo mensual por servicio, reglas, tráfico y retención de logs.
- Falsos positivos que podrían bloquear Auth0, APIs o futuras cargas de archivos.
- Trabajo operativo para ajustar reglas, excepciones, alertas y respuesta.
- Mayor dependencia de Azure y diagnóstico extremo a extremo más complejo.
- A cambio, la política perimetral queda centralizada sin contaminar el
  monolito con mitigaciones de red que no le corresponden.

## Prerrequisitos para aceptar la decisión

1. Arquitectura Azure y ambientes aprobados.
2. Inventario de dominios, origins y certificados.
3. Presupuesto y responsable operativo.
4. Rutas, límites y exclusiones obtenidos de pruebas reales.
5. Plan detección → ajuste → prevención, con rollback.
6. Integración de logs y alertas sin datos sensibles.
7. Pruebas E2E de Auth0, CORS, subida de archivos futura y APIs administrativas.

Hasta cumplirlos, el WAF debe figurar únicamente como control futuro propuesto.
