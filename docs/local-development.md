# Desarrollo local

El archivo `compose.yaml` de la raíz es la única definición canónica de Docker
Compose para MyComplex. Incluye PostgreSQL, backend y gateway, conserva el
volumen `mycomplex_postgres_data` y publica PostgreSQL únicamente en loopback.

Desde la raíz del repositorio:

```bash
docker compose config
docker compose up --build -d
docker compose ps
```

Para detener los servicios sin eliminar datos:

```bash
docker compose down
```

No se debe usar `docker compose down -v` en un entorno con datos que deban
conservarse. Las variables requeridas se suministran mediante la configuración
local del desarrollador; los secretos reales no se versionan.
