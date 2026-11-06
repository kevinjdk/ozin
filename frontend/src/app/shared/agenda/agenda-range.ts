import { AgendaRange } from '../../core/api/salon.api';
import { addDays, localToInstant, startOfWeek } from '../../core/time/salon-time';

export type AgendaView = 'day' | 'week';

/** Rango en UTC que cubre el día o la semana de una fecha local de la sede. */
export function agendaRange(localDate: string, view: AgendaView, zone: string): AgendaRange {
  const first = view === 'week' ? startOfWeek(localDate) : localDate;
  const last = addDays(first, view === 'week' ? 7 : 1);
  return { from: localToInstant(first, zone), to: localToInstant(last, zone) };
}
