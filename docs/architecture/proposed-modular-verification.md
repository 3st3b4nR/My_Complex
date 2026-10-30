# Verificación modular

**Estado:** Proposed

## Contexto y problema

Los límites del monolito modular dependían de revisión manual.

## Alternativas

- Solo revisión de código.
- ArchUnit para todas las reglas.
- Spring Modulith para módulos y ArchUnit solo si una regla no es expresable.

## Decisión propuesta

Usar `ApplicationModules.verify()` para ciclos, APIs nombradas y dependencias
permitidas. Evitar duplicar la misma regla con dos herramientas.

## Consecuencias y riesgos

Las violaciones fallan en pruebas. Los paquetes públicos deben declararse con
precisión para no convertir infraestructura en API accidental.

## Evidencia y pruebas

`ModularArchitectureTest` y los `package-info.java` de `users` y `properties`.
