# Modelo de dominio

## Módulos (bounded contexts)

| Módulo | Responsabilidad |
|---|---|
| `identity` | Usuarios, credenciales y roles (`ADMIN`, `STYLIST`, `CUSTOMER`). |
| `branch` | Sedes del salón (nombre, dirección, zona horaria). |
| `catalog` | Servicios del salón (`SalonService`). |
| `staff` | Estilistas, horarios laborales, ausencias y servicios que realizan. |
| `booking` | Citas, cálculo de disponibilidad y ciclo de vida de la cita. **Núcleo del negocio.** |
| `notification` | Envío de notificaciones (email). |

> La entidad del catálogo se llama `SalonService` (y no `Service`) para no confundirla con
> la anotación `@Service` de Spring ni con los servicios de la capa de aplicación.

## Diagrama de entidades

```mermaid
erDiagram
    USER ||--o| CUSTOMER : "es"
    USER ||--o| STYLIST : "es"
    BRANCH ||--o{ STYLIST : "emplea"
    STYLIST }o--o{ SALON_SERVICE : "realiza"
    STYLIST ||--o{ WORKING_HOURS : "tiene"
    STYLIST ||--o{ TIME_OFF : "tiene"
    CUSTOMER ||--o{ APPOINTMENT : "reserva"
    STYLIST ||--o{ APPOINTMENT : "atiende"
    SALON_SERVICE ||--o{ APPOINTMENT : "incluye"
    BRANCH ||--o{ APPOINTMENT : "registra"

    USER { uuid id string email string passwordHash string role boolean enabled }
    BRANCH { uuid id string name string address string timeZone boolean active }
    CUSTOMER { uuid id string fullName string email string phone }
    STYLIST { uuid id uuid branchId string fullName boolean active }
    SALON_SERVICE { uuid id string name int durationMinutes decimal price string currency boolean active }
    WORKING_HOURS { uuid stylistId string dayOfWeek time startTime time endTime }
    TIME_OFF { uuid stylistId timestamptz startsAt timestamptz endsAt string reason }
    APPOINTMENT { uuid id uuid customerId uuid stylistId uuid serviceId uuid branchId timestamptz startsAt timestamptz endsAt decimal price string currency string status string cancelledBy bigint version }
```

> `USER` y sus relaciones se implementan en la Fase 4. `WORKING_HOURS` y `TIME_OFF` son value objects
> del estilista, por lo que no tienen identificador propio.

## Value objects

Son inmutables (`record` de Java) y se validan al construirse: no puede existir un value object inválido.

| Value object | Contenido | Comportamiento |
|---|---|---|
| `TimeSlot` | `start`, `end` (`Instant`) | `overlapsWith(TimeSlot)`, `duration()`; exige `start < end` |
| `Money` | `amount` (`BigDecimal`), `currency` | Escala 2, no negativo, operaciones aritméticas seguras |
| `Email` | `value` | Formato válido, normalizado en minúsculas |
| `PhoneNumber` | `value` | Formato E.164 (por ejemplo `+593991234567`) |
| `*Id` | `UUID` | Identificadores tipados (`AppointmentId`, `StylistId`...) para evitar mezclar IDs |

## Ciclo de vida de la cita

```mermaid
stateDiagram-v2
    [*] --> PENDING : reservar
    PENDING --> CONFIRMED : confirmar
    PENDING --> CANCELLED : cancelar
    CONFIRMED --> CANCELLED : cancelar
    CONFIRMED --> COMPLETED : completar
    CONFIRMED --> NO_SHOW : no asistió
    COMPLETED --> [*]
    CANCELLED --> [*]
    NO_SHOW --> [*]
```

Las transiciones se implementan como métodos de la entidad `Appointment` (`confirm()`, `cancel()`,
`complete()`, `markNoShow()`); el modelo es **rico**, no anémico.
