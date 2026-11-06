# Reglas de negocio

Reglas validadas para el MVP de Ozin. Cada regla tiene un identificador que se referencia
desde el código (Javadoc) y desde los tests, para mantener trazabilidad requisito → código → test.

| ID | Regla | Valor | Capa donde se aplica |
|---|---|---|---|
| **RN-01** | Un estilista no puede tener dos citas activas solapadas. | — | Dominio + restricción `EXCLUDE` en PostgreSQL (ADR-0007) |
| **RN-02** | La cita debe estar dentro del horario laboral del estilista y fuera de sus ausencias. | — | Dominio |
| **RN-03** | El estilista debe estar habilitado para realizar el servicio elegido. | — | Dominio |
| **RN-04** | Anticipación mínima para reservar. | 1 hora | Dominio (política configurable) |
| **RN-05** | Anticipación máxima para reservar. | 30 días | Dominio (política configurable) |
| **RN-06** | Granularidad de los horarios disponibles ofrecidos. | 15 minutos | Dominio |
| **RN-07** | El cliente solo puede cancelar hasta cierto tiempo antes de la cita; después solo el administrador. | 24 horas | Dominio (`CancellationPolicy`) |
| **RN-08** | Máximo de citas futuras activas (`PENDING`/`CONFIRMED`) por cliente. | 3 | Dominio + bloqueo del cliente al reservar (ADR-0007) |
| **RN-09** | El precio del servicio se congela en la cita al momento de reservar. | — | Dominio |
| **RN-10** | Una cita corresponde a un único servicio (MVP). | — | Dominio |
| **RN-11** | Las fechas se almacenan en UTC (`timestamptz`) y se muestran en la zona horaria de la sede. | `America/Guayaquil` | Infraestructura + Frontend |
| **RN-12** | Reservar requiere un usuario registrado con rol `CUSTOMER`. | — | Seguridad |
| **RN-13** | Cada estilista pertenece a una sede; la cita se registra en la sede del estilista. | — | Dominio |
| **RN-14** | Los montos se manejan con `BigDecimal` en USD; nunca con `double`/`float`. | USD | Dominio (`Money`) |

## Transiciones de estado de una cita

| Desde | Hacia | Quién | Condición |
|---|---|---|---|
| `PENDING` | `CONFIRMED` | Estilista / Admin | — |
| `PENDING`, `CONFIRMED` | `CANCELLED` | Cliente | Respetando RN-07 |
| `PENDING`, `CONFIRMED` | `CANCELLED` | Admin | Sin restricción de tiempo |
| `CONFIRMED` | `COMPLETED` | Estilista / Admin | La hora de inicio ya pasó |
| `CONFIRMED` | `NO_SHOW` | Estilista / Admin | La hora de inicio ya pasó |

Cualquier otra transición es inválida y produce una excepción de dominio.

## Fuera del alcance del MVP

- Reservas como invitado (sin registro).
- Gestión de múltiples sedes desde la UI (el modelo ya las soporta; ver ADR-0006).
- Pagos en línea.
- Recordatorios automáticos programados.
- Bloqueo de clientes por inasistencias reiteradas.
- Dashboard de métricas.
