# ADR-0002: Maven multimódulo para hacer cumplir las capas

- **Estado:** Aceptado
- **Fecha:** 2026-11-05

## Contexto
En un proyecto de un solo módulo, las reglas de la arquitectura hexagonal son solo una convención:
nada impide importar `jakarta.persistence` dentro del dominio.

## Decisión
Dividir el backend en los módulos `ozin-domain`, `ozin-application`, `ozin-infrastructure` y `ozin-bootstrap`.
`ozin-domain` no declara dependencias de frameworks, por lo que **el compilador** rechaza cualquier violación.
Además se agregan tests de **ArchUnit** como segunda línea de defensa.

## Alternativas consideradas
- **Un módulo + solo ArchUnit:** detecta violaciones en los tests, no al compilar.
- **Spring Modulith:** buena opción para límites entre módulos de negocio, pero no separa capas técnicas.

## Consecuencias
- (+) Violaciones de arquitectura imposibles de compilar.
- (−) Configuración de build algo más extensa.
