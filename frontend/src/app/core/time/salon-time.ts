/**
 * Fechas en la zona horaria de la sede (RN-11). La API trabaja en UTC, pero un cliente de viaje
 * debe ver las 10:00 del salón, no la hora de su dispositivo.
 *
 * Una fecha local se representa como 'YYYY-MM-DD'.
 */

const LOCALE = 'es-EC';

function format(iso: string, zone: string, options: Intl.DateTimeFormatOptions): string {
  return new Intl.DateTimeFormat(LOCALE, { timeZone: zone, ...options }).format(new Date(iso));
}

export function formatTime(iso: string, zone: string): string {
  return format(iso, zone, { hour: '2-digit', minute: '2-digit', hour12: false });
}

export function formatDay(iso: string, zone: string): string {
  return format(iso, zone, { weekday: 'long', day: 'numeric', month: 'long' });
}

export function formatMoney(amount: number, currency: string): string {
  return new Intl.NumberFormat(LOCALE, { style: 'currency', currency }).format(amount);
}

function partsInZone(instant: Date, zone: string): Record<string, number> {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: zone,
    hourCycle: 'h23',
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
    second: 'numeric',
  }).formatToParts(instant);
  return Object.fromEntries(parts.map((part) => [part.type, Number(part.value)]));
}

/** Fecha local de la sede en un instante dado. */
export function localDateIn(instant: Date, zone: string): string {
  const { year, month, day } = partsInZone(instant, zone);
  return toLocalDate(year, month, day);
}

function toLocalDate(year: number, month: number, day: number): string {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

/** Convierte una fecha y hora de la sede ('YYYY-MM-DDTHH:mm') en un instante ISO-8601 en UTC. */
export function localToInstant(localDateTime: string, zone: string): string {
  const [date, time = '00:00'] = localDateTime.split('T');
  const [year, month, day] = date.split('-').map(Number);
  const [hour, minute] = time.split(':').map(Number);
  const wallClock = Date.UTC(year, month - 1, day, hour, minute);

  const inZone = partsInZone(new Date(wallClock), zone);
  const shown = Date.UTC(inZone['year'], inZone['month'] - 1, inZone['day'], inZone['hour'], inZone['minute'], inZone['second']);
  return new Date(wallClock - (shown - wallClock)).toISOString();
}

/** Inversa de {@link localToInstant}: instante a 'YYYY-MM-DDTHH:mm' de la sede. */
export function instantToLocal(iso: string, zone: string): string {
  const parts = partsInZone(new Date(iso), zone);
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${toLocalDate(parts['year'], parts['month'], parts['day'])}T${pad(parts['hour'])}:${pad(parts['minute'])}`;
}

export function addDays(localDate: string, days: number): string {
  const [year, month, day] = localDate.split('-').map(Number);
  const result = new Date(Date.UTC(year, month - 1, day + days));
  return toLocalDate(result.getUTCFullYear(), result.getUTCMonth() + 1, result.getUTCDate());
}

/** Lunes de la semana de una fecha local. */
export function startOfWeek(localDate: string): string {
  const [year, month, day] = localDate.split('-').map(Number);
  const weekday = new Date(Date.UTC(year, month - 1, day)).getUTCDay();
  return addDays(localDate, weekday === 0 ? -6 : 1 - weekday);
}

/** Convierte la fecha elegida en un calendario (hora local del dispositivo) en una fecha local. */
export function pickerDateToLocal(date: Date): string {
  return toLocalDate(date.getFullYear(), date.getMonth() + 1, date.getDate());
}

/** Inversa de {@link pickerDateToLocal}. */
export function localToPickerDate(localDate: string): Date {
  const [year, month, day] = localDate.split('-').map(Number);
  return new Date(year, month - 1, day);
}
