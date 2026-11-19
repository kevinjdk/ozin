import { DayOfWeek, WorkingHours } from '../../../core/api/models';

/**
 * Horario semanal de un estilista tal como lo edita el administrador. La API guarda franjas sueltas
 * por día; una jornada con pausa de mediodía son dos franjas (entrada→pausa y fin de pausa→salida).
 *
 * - Modo fijo: un horario para entre semana (lunes a viernes) y otro para el fin de semana.
 * - Modo personalizado: un horario por día, que parte del fijo para cambiar solo lo necesario.
 */

export const WEEK: readonly { day: DayOfWeek; short: string; long: string }[] = [
  { day: 'MONDAY', short: 'Lun', long: 'lunes' },
  { day: 'TUESDAY', short: 'Mar', long: 'martes' },
  { day: 'WEDNESDAY', short: 'Mié', long: 'miércoles' },
  { day: 'THURSDAY', short: 'Jue', long: 'jueves' },
  { day: 'FRIDAY', short: 'Vie', long: 'viernes' },
  { day: 'SATURDAY', short: 'Sáb', long: 'sábado' },
  { day: 'SUNDAY', short: 'Dom', long: 'domingo' },
];

export const WEEKEND: readonly DayOfWeek[] = ['SATURDAY', 'SUNDAY'];
/** Por defecto se trabaja de lunes a sábado y el domingo es el día libre. */
export const DEFAULT_WORK_DAYS: readonly DayOfWeek[] = WEEK.slice(0, 6).map(({ day }) => day);

export type ScheduleMode = 'fixed' | 'custom';

/** Jornada de un día: entrada, salida y, si se activa, una pausa de mediodía. */
export interface DayHours {
  start: string;
  end: string;
  hasBreak: boolean;
  breakStart: string;
  breakEnd: string;
}

export interface WeeklySchedule {
  mode: ScheduleMode;
  workDays: DayOfWeek[];
  /** Modo fijo: lunes a viernes. */
  weekdays: DayHours;
  /** Modo fijo: sábado y domingo. */
  weekend: DayHours;
  /** Modo personalizado: un horario por día. */
  days: Record<DayOfWeek, DayHours>;
}

export function defaultDayHours(): DayHours {
  return { start: '09:00', end: '18:00', hasBreak: true, breakStart: '13:00', breakEnd: '14:00' };
}

function everyDay(hours: (day: DayOfWeek) => DayHours): Record<DayOfWeek, DayHours> {
  return Object.fromEntries(WEEK.map(({ day }) => [day, { ...hours(day) }])) as Record<DayOfWeek, DayHours>;
}

export function defaultSchedule(): WeeklySchedule {
  return {
    mode: 'fixed',
    workDays: [...DEFAULT_WORK_DAYS],
    weekdays: defaultDayHours(),
    weekend: defaultDayHours(),
    days: everyDay(defaultDayHours),
  };
}

export function isWeekend(day: DayOfWeek): boolean {
  return WEEKEND.includes(day);
}

/** Horario que rige un día según el modo activo. */
export function hoursOn(schedule: WeeklySchedule, day: DayOfWeek): DayHours {
  if (schedule.mode === 'custom') {
    return schedule.days[day];
  }
  return isWeekend(day) ? schedule.weekend : schedule.weekdays;
}

/** Pasa a personalizado copiando el horario fijo en cada día, para que solo haya que tocar el que cambia. */
export function toCustom(schedule: WeeklySchedule): WeeklySchedule {
  return { ...schedule, mode: 'custom', days: everyDay((day) => hoursOn(schedule, day)) };
}

function sameHours(a: DayHours, b: DayHours): boolean {
  return (
    a.start === b.start &&
    a.end === b.end &&
    a.hasBreak === b.hasBreak &&
    (!a.hasBreak || (a.breakStart === b.breakStart && a.breakEnd === b.breakEnd))
  );
}

function rangesOf(hours: DayHours): { start: string; end: string }[] {
  return hours.hasBreak
    ? [
        { start: hours.start, end: hours.breakStart },
        { start: hours.breakEnd, end: hours.end },
      ]
    : [{ start: hours.start, end: hours.end }];
}

export function toWorkingHours(schedule: WeeklySchedule): WorkingHours[] {
  return WEEK.filter(({ day }) => schedule.workDays.includes(day)).flatMap(({ day }) =>
    rangesOf(hoursOn(schedule, day)).map((range) => ({ day, ...range })),
  );
}

/** Lee las franjas de un día: la primera entrada, la última salida y, si hay hueco entre franjas, la pausa. */
function dayHoursFrom(ranges: WorkingHours[]): DayHours {
  const sorted = ranges
    .map((range) => ({ start: range.start.slice(0, 5), end: range.end.slice(0, 5) }))
    .sort((a, b) => a.start.localeCompare(b.start));
  const first = sorted[0];
  const last = sorted.at(-1)!;
  if (sorted.length === 1) {
    return { ...defaultDayHours(), start: first.start, end: first.end, hasBreak: false };
  }
  return { start: first.start, end: last.end, hasBreak: true, breakStart: first.end, breakEnd: last.start };
}

/**
 * Reconstruye el horario guardado. Si todos los días de entre semana coinciden entre sí, y también los
 * del fin de semana, se muestra como fijo; si no, como personalizado.
 */
export function fromWorkingHours(workingHours: WorkingHours[]): WeeklySchedule {
  const workDays: DayOfWeek[] = [];
  const days = everyDay(defaultDayHours);
  for (const { day } of WEEK) {
    const ranges = workingHours.filter((range) => range.day === day);
    if (ranges.length > 0) {
      workDays.push(day);
      days[day] = dayHoursFrom(ranges);
    }
  }

  const weekdays = workDays.filter((day) => !isWeekend(day));
  const weekend = workDays.filter(isWeekend);
  const uniform = (group: DayOfWeek[]) => group.every((day) => sameHours(days[day], days[group[0]]));
  const isFixed = uniform(weekdays) && uniform(weekend);

  return {
    mode: isFixed ? 'fixed' : 'custom',
    workDays,
    weekdays: { ...(weekdays.length > 0 ? days[weekdays[0]] : defaultDayHours()) },
    weekend: { ...(weekend.length > 0 ? days[weekend[0]] : defaultDayHours()) },
    days,
  };
}

/** Error de una jornada, o null si es válida. */
export function dayHoursError(hours: DayHours): string | null {
  if (!hours.start || !hours.end || hours.start >= hours.end) {
    return 'la salida debe ser después de la entrada';
  }
  if (!hours.hasBreak) {
    return null;
  }
  if (!hours.breakStart || !hours.breakEnd || hours.breakStart >= hours.breakEnd) {
    return 'la pausa debe terminar después de empezar';
  }
  if (hours.breakStart <= hours.start || hours.breakEnd >= hours.end) {
    return 'la pausa debe quedar dentro de la jornada';
  }
  return null;
}

/** Mensajes de error de los horarios que se van a guardar, por ejemplo "Entre semana: la pausa...". */
export function scheduleErrors(schedule: WeeklySchedule): string[] {
  const targets: { label: string; hours: DayHours }[] =
    schedule.mode === 'fixed'
      ? [
          ...(schedule.workDays.some((day) => !isWeekend(day)) ? [{ label: 'Entre semana', hours: schedule.weekdays }] : []),
          ...(schedule.workDays.some(isWeekend) ? [{ label: 'Fin de semana', hours: schedule.weekend }] : []),
        ]
      : WEEK.filter(({ day }) => schedule.workDays.includes(day)).map(({ day, long }) => ({
          label: long.charAt(0).toUpperCase() + long.slice(1),
          hours: schedule.days[day],
        }));
  return targets.flatMap(({ label, hours }) => {
    const error = dayHoursError(hours);
    return error ? [`${label}: ${error}.`] : [];
  });
}

/** Texto amable sobre la pausa de mediodía del horario tal como está configurado. */
export function breakSummary(schedule: WeeklySchedule): string {
  const hours = schedule.workDays.map((day) => hoursOn(schedule, day));
  if (hours.length === 0) {
    return 'Este horario aún no tiene días de trabajo.';
  }
  const withBreak = hours.filter((day) => day.hasBreak);
  if (withBreak.length === 0) {
    return 'Este horario no tiene pausa al mediodía: la jornada es continua.';
  }
  const ranges = new Set(withBreak.map((day) => `${day.breakStart} a ${day.breakEnd}`));
  if (ranges.size > 1) {
    return 'Este horario tiene pausas al mediodía distintas según el día.';
  }
  const range = [...ranges][0];
  return withBreak.length === hours.length
    ? `Este horario tiene definida la pausa al mediodía de ${range}.`
    : `Este horario tiene definida la pausa al mediodía de ${range}, solo en algunos días.`;
}

/** Resumen para la tabla de estilistas, por ejemplo "Lun a Sáb · 09:00–18:00, pausa 13:00–14:00". */
export function describeSchedule(workingHours: WorkingHours[]): string {
  const schedule = fromWorkingHours(workingHours);
  if (schedule.workDays.length === 0) {
    return 'Sin horario';
  }
  const indexes = schedule.workDays.map((day) => WEEK.findIndex((entry) => entry.day === day));
  const consecutive = indexes.every((index, position) => position === 0 || index === indexes[position - 1] + 1);
  const days =
    consecutive && indexes.length > 2
      ? `${WEEK[indexes[0]].short} a ${WEEK[indexes.at(-1)!].short}`
      : indexes.map((index) => WEEK[index].short).join(', ');

  const texts = new Set(
    schedule.workDays.map((day) => {
      const hours = schedule.days[day];
      const base = `${hours.start}–${hours.end}`;
      return hours.hasBreak ? `${base}, pausa ${hours.breakStart}–${hours.breakEnd}` : base;
    }),
  );
  return texts.size === 1 ? `${days} · ${[...texts][0]}` : `${days} · horario variable`;
}
