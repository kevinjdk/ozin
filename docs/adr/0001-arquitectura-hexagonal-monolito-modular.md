# ADR-0001: Arquitectura hexagonal en un monolito modular

- **Estado:** Aceptado
- **Fecha:** 2026-11-05

## Contexto
Ozin tiene reglas de negocio no triviales (disponibilidad, solapamientos, políticas de cancelación)
que deben poder probarse sin depender de la base de datos ni del framework. El equipo es pequeño
y el MVP debe llegar a producción rápido.

## Decisión
Usar **arquitectura hexagonal (puertos y adaptadores)** dentro de un **monolito modular**.
Los módulos de negocio (`booking`, `staff`, `catalog`, `identity`, `branch`, `notification`)
están aislados por paquetes, de modo que podrían extraerse a servicios independientes si fuera necesario.

## Alternativas consideradas
- **Arquitectura en capas tradicional (controller/service/repository):** acopla el negocio a JPA y Spring.
- **Microservicios desde el inicio:** complejidad operativa (red, consistencia eventual, despliegues)
  injustificada para el volumen y el tamaño del equipo.

## Consecuencias
- (+) El dominio se prueba con tests unitarios puros y rápidos.
- (+) La infraestructura (BD, email) se puede reemplazar sin tocar el negocio.
- (−) Más clases y mapeos (entidad de dominio ↔ entidad JPA ↔ DTO).
