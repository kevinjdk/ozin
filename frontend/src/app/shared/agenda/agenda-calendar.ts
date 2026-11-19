import { AppointmentView } from '../../core/api/models';
import { AgendaRange } from '../../core/api/salon.api';
import { addDays, formatTime, instantToLocal, localToInstant } from '../../core/time/salon-time';

/**
 * Cálculos de la agenda mensual. Todo se expresa en fechas locales de la sede ('YYYY-MM-DD') y meses
 * 'YYYY-MM', para que el calendario muestre los días del salón y no los del dispositivo.
 */

export function monthOf(localDate: string): string {
  return localDate.slice(0, 7);
}

export function shiftMonth(month: string, delta: number): string {
  const [year, monthNumber] = month.split('-').map(Number);
  const shifted = new Date(Date.UTC(year, monthNumber - 1 + delta, 1));
  return `${shifted.getUTCFullYear()}-${String(shifted.getUTCMonth() + 1).padStart(2, '0')}`;
}

/** Rango en UTC del mes completo; la API admite hasta 31 días, justo lo que mide un mes. */
export function monthRange(month: string, zone: string): AgendaRange {
  return {
    from: localToInstant(`${month}-01T00:00`, zone),
    to: localToInstant(`${shiftMonth(month, 1)}-01T00:00`, zone),
  };
}

export interface CalendarDay {
  date: string;
  day: number;
  inMonth: boolean;
}

/** Semanas del mes, de lunes a domingo, completadas con los días de los meses vecinos. */
export function monthGrid(month: string): CalendarDay[][] {
  const first = `${month}-01`;
  const [year, monthNumber] = month.split('-').map(Number);
  const weekday = new Date(Date.UTC(year, monthNumber - 1, 1)).getUTCDay();
  let cursor = addDays(first, weekday === 0 ? -6 : 1 - weekday);

  const weeks: CalendarDay[][] = [];
  do {
    const week: CalendarDay[] = [];
    for (let index = 0; index < 7; index++) {
      week.push({ date: cursor, day: Number(cursor.slice(8)), inMonth: monthOf(cursor) === month });
      cursor = addDays(cursor, 1);
    }
    weeks.push(week);
  } while (monthOf(cursor) === month);
  return weeks;
}

export function localDateOf(appointment: AppointmentView, zone: string): string {
  return instantToLocal(appointment.start, zone).slice(0, 10);
}

/** Días con al menos una cita. Las canceladas no cuentan: ese hueco vuelve a estar libre. */
export function busyDays(appointments: AppointmentView[], zone: string): Set<string> {
  return new Set(
    appointments.filter((appointment) => appointment.status !== 'CANCELLED').map((appointment) => localDateOf(appointment, zone)),
  );
}

export interface HourGroup {
  hour: string;
  appointments: AppointmentView[];
}

/** Citas de un día agrupadas por la hora en que empiezan ("09:00", "10:00"...). */
export function groupByHour(appointments: AppointmentView[], localDate: string, zone: string): HourGroup[] {
  const groups = new Map<string, AppointmentView[]>();
  const ofDay = appointments
    .filter((appointment) => localDateOf(appointment, zone) === localDate)
    .sort((a, b) => a.start.localeCompare(b.start));
  for (const appointment of ofDay) {
    const hour = `${formatTime(appointment.start, zone).slice(0, 2)}:00`;
    groups.set(hour, [...(groups.get(hour) ?? []), appointment]);
  }
  return [...groups.entries()].map(([hour, list]) => ({ hour, appointments: list }));
}
