# ADR-0005: Frontend con Angular y PrimeNG

- **Estado:** Aceptado
- **Fecha:** 2026-11-05

## Contexto
El frontend necesita formularios complejos (reserva, horarios), tablas y calendarios,
y debe seguir convenciones habituales en aplicaciones empresariales.

## Decisión
- **Angular** con standalone components, **signals** y lazy loading por feature.
- **PrimeNG** como librería de componentes (DataTable, DatePicker, Stepper).
- Organización por features con separación `data-access` / `ui` / `pages` (smart y dumb components).

## Consecuencias
- (+) Stack alineado con el usado en el sector financiero.
- (+) Componentes ricos sin construirlos desde cero.
- (−) Mayor tamaño de bundle que una librería minimalista (mitigado con lazy loading).
