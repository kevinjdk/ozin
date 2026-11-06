# ADR-0007: Transacciones sin framework y control de concurrencia

- **Estado:** Aceptado
- **Fecha:** 2026-11-06

## Contexto
Un caso de uso como "reservar una cita" lee y escribe varios datos que deben confirmarse juntos,
pero la capa de aplicación no puede depender de Spring (ADR-0002), por lo que no puede usar `@Transactional`.

Además, validar una regla y guardar después no es atómico: dos peticiones simultáneas pueden pasar
la misma validación y guardar datos que juntos la incumplen.

## Decisión

**Transacciones.** La capa de aplicación define el puerto `UnitOfWork`, y la infraestructura lo implementa
con `TransactionTemplate`. Las notificaciones se envían después de confirmar la transacción, nunca dentro.

**Concurrencia.** Cada riesgo se resuelve con el mecanismo adecuado:

| Riesgo | Mecanismo | Resultado para el cliente |
|---|---|---|
| Dos reservas del mismo horario (RN-01) | Restricción `EXCLUDE USING gist` en PostgreSQL | `409` con código `RN-01` |
| Un cliente supera su límite de citas (RN-08) | Bloqueo pesimista de la fila del cliente durante la reserva | `422` con código `RN-08` |
| Dos cambios de estado sobre la misma cita | Bloqueo optimista (`@Version`) | `409` con código `CONCURRENT_MODIFICATION` |
| Dos registros con el mismo email | Restricción `UNIQUE` | `409` con código `EMAIL_ALREADY_REGISTERED` |

## Alternativas consideradas
- **`@Transactional` en los servicios de aplicación:** es lo habitual, pero acopla esa capa a Spring.
- **Decorar los casos de uso con transacciones desde la configuración:** mantiene limpio el servicio, pero oculta
  dónde empieza y termina la transacción, y obligaría a mantener las notificaciones fuera por otra vía.
- **Aislamiento `SERIALIZABLE`:** cubriría RN-01 y RN-08, pero obliga a reintentar transacciones
  en toda la aplicación, incluso en operaciones sin conflicto.

## Consecuencias
- (+) Los casos de uso se prueban sin Spring, sustituyendo `UnitOfWork` en el test.
- (+) Las reglas se cumplen bajo concurrencia; lo verifican tests con peticiones simultáneas contra PostgreSQL real.
- (−) Las reservas simultáneas de un mismo cliente se serializan. Es aceptable: un cliente reserva muy pocas veces.
- (−) Si el proceso cae justo después de confirmar la transacción, se pierde el aviso. Se podrá resolver con
  un patrón *outbox* cuando existan notificaciones reales.
