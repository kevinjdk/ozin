# Historias de usuario (MVP)

Prioridad según MoSCoW: **M** = Must, **S** = Should, **C** = Could.

## Épica 1 — Reserva (cliente)

### HU-01 · Ver catálogo de servicios — M
**Como** visitante **quiero** ver los servicios con precio y duración **para** decidir qué reservar.

- **Dado** que existen servicios activos, **cuando** abro el catálogo, **entonces** veo nombre, descripción, duración y precio de cada uno.
- **Dado** un servicio inactivo, **cuando** abro el catálogo, **entonces** no aparece.

### HU-02 · Consultar disponibilidad — M
**Como** cliente **quiero** elegir servicio, estilista y fecha **para** ver los horarios libres.

- **Dado** un estilista habilitado para el servicio, **cuando** consulto una fecha, **entonces** veo horarios cada 15 min (RN-06) que caben completos en su jornada (RN-02) y no se solapan con otras citas (RN-01).
- **Dado** una fecha a más de 30 días, **cuando** consulto, **entonces** no se muestran horarios (RN-05).
- **Dado** que el estilista tiene una ausencia ese día, **cuando** consulto, **entonces** no se muestran horarios en ese rango.

### HU-03 · Reservar una cita — M
**Como** cliente **quiero** reservar un horario **para** asegurar mi atención.

- **Dado** un horario libre, **cuando** reservo, **entonces** la cita queda confirmada (`CONFIRMED`) con el precio congelado (RN-09) y recibo un email de confirmación.
- **Dado** que otro cliente reservó el mismo horario segundos antes, **cuando** reservo, **entonces** recibo un error de conflicto (HTTP 409) (RN-01).
- **Dado** que ya tengo 3 citas activas, **cuando** intento reservar, **entonces** el sistema lo rechaza (RN-08).
- **Dado** un horario a menos de 1 hora, **cuando** reservo, **entonces** el sistema lo rechaza (RN-04).

### HU-04 · Ver mis citas — M
**Como** cliente **quiero** ver mis citas próximas y pasadas **para** organizarme.

### HU-05 · Cancelar una cita — M
**Como** cliente **quiero** cancelar una cita **para** liberar el horario.

- **Dado** una cita a más de 1 hora, **cuando** cancelo, **entonces** pasa a `CANCELLED` y recibo un email.
- **Dado** una cita a menos de 1 hora, **cuando** intento cancelar, **entonces** el sistema lo rechaza indicando que contacte al salón (RN-07).

## Épica 2 — Identidad y acceso

### HU-06 · Registro e inicio de sesión — M
**Como** visitante **quiero** registrarme e iniciar sesión **para** poder reservar (RN-12).

- Email único, contraseña con política mínima (8+ caracteres, mayúscula, número).
- Al iniciar sesión recibo un access token de corta duración y un refresh token.

### HU-07 · Control de acceso por rol — M
**Como** sistema **quiero** restringir pantallas y endpoints por rol (`ADMIN`, `STYLIST`, `CUSTOMER`).

## Épica 3 — Estilista

### HU-08 · Ver mi agenda — M
**Como** estilista **quiero** ver mi agenda en un calendario mensual **para** saber de un vistazo qué días tengo citas.

- El calendario del mes está a la izquierda; los días con al menos una cita (sin contar las canceladas) se pintan de rojo.
- Al elegir un día, a la derecha se listan sus citas separadas por hora.

### HU-09 · Registrar la asistencia — M
**Como** estilista **quiero** marcar si el cliente asistió o no **para** llevar el control de mis citas.

- Las citas llegan ya confirmadas: el estilista no tiene que aprobarlas.
- Una vez llegada la hora de inicio, la cita se marca como "asistió" (`COMPLETED`) o "no asistió" (`NO_SHOW`).

## Épica 4 — Administración

### HU-10 · Gestionar catálogo de servicios — M
CRUD de servicios con nombre, descripción opcional (hasta 500 caracteres), duración y precio;
activar y desactivar sin borrar el histórico. La descripción se muestra al cliente en el catálogo y al reservar.

### HU-11 · Gestionar estilistas — M
Alta, edición, baja lógica y asignación de servicios que realiza cada estilista.

### HU-12 · Gestionar horarios y ausencias — M
Configurar el horario desde la ficha del estilista y registrar ausencias (vacaciones, permisos).

- Todo estilista nuevo empieza con el horario fijo: de lunes a sábado, de 09:00 a 18:00 con pausa al mediodía de
  13:00 a 14:00, y el domingo libre.
- El día libre se elige marcando los días que trabaja.
- **Fijo:** un horario para entre semana (lunes a viernes) y otro para el fin de semana, cada uno con su pausa.
- **Personalizado:** un horario por día que parte del fijo, para cambiar solo el día que lo necesite.
- La pausa al mediodía se activa con una casilla, marcada por defecto; desmarcada, la jornada es continua.
- El formulario resume la pausa tal como está configurada, por ejemplo "Este horario tiene definida la pausa al
  mediodía de 13:00 a 14:00".
- Una ausencia puede ser de uno o varios días completos, o de unas horas de un mismo día; se puede eliminar si ya no aplica.
- Ni el horario ni las ausencias cancelan las citas ya reservadas: el administrador las revisa en la agenda.

### HU-13 · Agenda general del salón — S
La misma vista de calendario de HU-08, con las citas de todos los estilistas y un filtro por estilista.
