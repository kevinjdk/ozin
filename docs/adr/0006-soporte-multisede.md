# ADR-0006: Modelo preparado para múltiples sedes

- **Estado:** Aceptado
- **Fecha:** 2026-11-05

## Contexto
El negocio inicia con una sede, pero se prevé abrir más. Agregar el concepto de sede más tarde
obligaría a migrar datos y modificar todas las consultas de agenda.

## Decisión
Incluir desde el inicio la entidad `Branch` (sede) con su zona horaria. Cada estilista pertenece
a una sede y cada cita registra la sede. El MVP crea una sede por defecto mediante migración y
**no** expone gestión de sedes en la UI.

## Consecuencias
- (+) Abrir una nueva sede no requiere migraciones estructurales.
- (+) La zona horaria se resuelve por sede, no como constante global.
- (−) Un campo y una relación extra que en el MVP siempre apuntan a la misma sede.
