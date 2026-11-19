import { TimeOff } from '../../../core/api/models';
import { addDays, formatDay, formatTime, instantToLocal, localToInstant } from '../../../core/time/salon-time';

/**
 * Ausencias tal como las piensa el administrador: uno o varios días completos, o unas horas de un día.
 * La API solo conoce instantes; un día completo va de las 00:00 de la sede a las 00:00 del día siguiente.
 */
export type TimeOffInput =
  | { kind: 'days'; from: string; to: string }
  | { kind: 'hours'; date: string; start: string; end: string };

export function toTimeOffPeriod(input: TimeOffInput, zone: string): { start: string; end: string } {
  if (input.kind === 'days') {
    return {
      start: localToInstant(`${input.from}T00:00`, zone),
      end: localToInstant(`${addDays(input.to, 1)}T00:00`, zone),
    };
  }
  return {
    start: localToInstant(`${input.date}T${input.start}`, zone),
    end: localToInstant(`${input.date}T${input.end}`, zone),
  };
}

/** Mensaje de error para el formulario, o null si el periodo es válido. */
export function timeOffInputError(input: TimeOffInput): string | null {
  if (input.kind === 'days') {
    if (!input.from || !input.to) {
      return 'Indica desde y hasta qué día.';
    }
    return input.to < input.from ? 'El último día no puede ser anterior al primero.' : null;
  }
  if (!input.date || !input.start || !input.end) {
    return 'Indica el día y el horario.';
  }
  return input.end <= input.start ? 'La hora de fin debe ser posterior a la de inicio.' : null;
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** "Lunes, 24 de noviembre · día completo", "Del lunes, 24 de noviembre al miércoles, 26 de noviembre"... */
export function describeTimeOff(off: TimeOff, zone: string): string {
  const [startDate, startTime] = instantToLocal(off.start, zone).split('T');
  const [endDate, endTime] = instantToLocal(off.end, zone).split('T');
  const day = (localDate: string) => formatDay(localToInstant(localDate, zone), zone);

  if (startTime === '00:00' && endTime === '00:00') {
    const lastDay = addDays(endDate, -1);
    return lastDay === startDate
      ? `${capitalize(day(startDate))} · día completo`
      : `Del ${day(startDate)} al ${day(lastDay)}`;
  }
  if (startDate === endDate) {
    return `${capitalize(day(startDate))} · ${formatTime(off.start, zone)} a ${formatTime(off.end, zone)}`;
  }
  return `Del ${day(startDate)} ${formatTime(off.start, zone)} al ${day(endDate)} ${formatTime(off.end, zone)}`;
}

/** Las ausencias ya terminadas no se muestran: solo interesan las vigentes y las futuras. */
export function isCurrentOrUpcoming(off: TimeOff, now: Date): boolean {
  return new Date(off.end).getTime() > now.getTime();
}
