# Dependencia de users hacia properties

**Estado:** Proposed

## Contexto y problema

`users` necesita datos residenciales para listar y validar asignaciones sin
convertirse en propietario de unidades.

## Alternativas

- Acceso directo a repositorios/tablas de `properties`.
- Dependencia bidireccional.
- Puerto público unidireccional.

## Decisión propuesta

La única dirección permitida es `users → properties::api`, mediante input ports
y modelos públicos de lectura. `properties` no importa `users`. La resolución
de IDs se hace en lote para evitar N+1.

## Consecuencias y riesgos

El contrato interno queda explícito y testeable. Añade una fase de composición
en aplicación. Una validación y escritura usan la misma base PostgreSQL; la FK
protege carreras residuales y la transacción de escritura conserva locking y
versionado existentes.

## Evidencia y pruebas

`ResolveResidentialUnitsUseCase`, `PropertyMemberResidentialUnitEnricher` y
`ModularArchitectureTest` documentan y verifican el flujo.
