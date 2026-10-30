# Propiedad modular de datos residenciales

**Estado:** Proposed

## Contexto y problema

Las estructuras y unidades residenciales estaban siendo consultadas desde SQL
del módulo `users`, aunque su ciclo de vida pertenece a `properties`.

## Alternativas

- Mantener consultas cruzadas por simplicidad.
- Duplicar datos residenciales en `users`.
- Exponer una API interna pública y por lote desde `properties`.

## Decisión propuesta

`properties` es propietario de estructuras, unidades, estado, pertenencia y
borrado lógico. `users` conserva membresías y asociaciones usuario-unidad. Las
claves foráneas siguen siendo la última defensa de integridad.

## Consecuencias y riesgos

Mejora el límite modular y evita duplicación, a cambio de componer la lectura
con dos consultas. El riesgo es omitir el enriquecimiento antes de responder;
las pruebas de servicio y persistencia lo protegen.

## Evidencia y pruebas

`ResidentialUnitQueryPersistenceAdapter` es el único acceso a tablas
residenciales. `ModularArchitectureTest` y la búsqueda de SQL desde `users`
protegen esta decisión.
