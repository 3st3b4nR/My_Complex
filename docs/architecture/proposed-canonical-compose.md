# Docker Compose canónico

**Estado:** Proposed

## Contexto y problema

Existían dos Compose contradictorios; el secundario solo levantaba PostgreSQL,
exponía el puerto en todas las interfaces y no tenía consumidores documentados.

## Alternativas

- Mantener ambos.
- Convertir el secundario en override.
- Usar exclusivamente el Compose raíz.

## Decisión propuesta

`/compose.yaml` es la fuente canónica para PostgreSQL, backend y gateway. El
Compose secundario se elimina y los comandos se documentan desde la raíz.

## Consecuencias y riesgos

Se elimina deriva de configuración y PostgreSQL permanece ligado a loopback.
Quien usara informalmente el archivo secundario deberá migrar al comando raíz.

## Evidencia y pruebas

CI ejecuta `docker compose config` y `docker compose build` desde la raíz; no se
encontraron scripts ni documentación consumidores del archivo secundario.
