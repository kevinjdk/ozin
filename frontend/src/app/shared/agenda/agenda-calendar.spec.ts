import { AppointmentStatus, AppointmentView } from '../../core/api/models';
import { busyDays, groupByHour, monthGrid, monthOf, monthRange, shiftMonth } from './agenda-calendar';

const ZONE = 'America/Guayaquil'; // UTC-5

function appointment(start: string, status: AppointmentStatus = 'CONFIRMED', id = start): AppointmentView {
  return {
    id,
    start,
    end: start,
    price: 12,
    currency: 'USD',
    status,
    customerId: 'c',
    customerName: 'Cliente',
    stylistId: 's',
    stylistName: 'Ana',
    serviceId: 'x',
    serviceName: 'Corte',
    branchId: 'b',
  };
}

describe('agenda calendar', () => {
  it('moves between months across years', () => {
    expect(monthOf('2026-11-19')).toBe('2026-11');
    expect(shiftMonth('2026-12', 1)).toBe('2027-01');
    expect(shiftMonth('2026-01', -1)).toBe('2025-12');
  });

  it('queries the whole month in the salon time zone', () => {
    expect(monthRange('2026-11', ZONE)).toEqual({
      from: '2026-11-01T05:00:00.000Z',
      to: '2026-12-01T05:00:00.000Z',
    });
  });

  it('lays out the month in weeks from Monday to Sunday', () => {
    const weeks = monthGrid('2026-11'); // el 1 de noviembre de 2026 es domingo

    expect(weeks).toHaveLength(6);
    expect(weeks[0][0]).toEqual({ date: '2026-10-26', day: 26, inMonth: false });
    expect(weeks[0][6]).toEqual({ date: '2026-11-01', day: 1, inMonth: true });
    expect(weeks.at(-1)!.at(-1)).toEqual({ date: '2026-12-06', day: 6, inMonth: false });
    expect(weeks.flat().filter((day) => day.inMonth)).toHaveLength(30);
  });

  it('does not add an empty week when the month ends on a Sunday', () => {
    const weeks = monthGrid('2026-05'); // mayo de 2026 termina en domingo

    expect(weeks.at(-1)!.at(-1)!.date).toBe('2026-05-31');
  });

  it('marks the salon days that have appointments, ignoring cancelled ones', () => {
    const days = busyDays(
      [
        appointment('2026-11-24T15:00:00Z'),
        appointment('2026-11-25T02:00:00Z'), // 21:00 del 24 en Ecuador
        appointment('2026-11-26T15:00:00Z', 'CANCELLED'),
        appointment('2026-11-27T15:00:00Z', 'COMPLETED'),
      ],
      ZONE,
    );

    expect([...days].sort()).toEqual(['2026-11-24', '2026-11-27']);
  });

  it('groups the appointments of a day by the hour they start', () => {
    const groups = groupByHour(
      [
        appointment('2026-11-24T15:30:00Z', 'CONFIRMED', 'b'), // 10:30
        appointment('2026-11-24T14:00:00Z', 'CONFIRMED', 'a'), // 09:00
        appointment('2026-11-24T15:00:00Z', 'CONFIRMED', 'c'), // 10:00
        appointment('2026-11-25T15:00:00Z', 'CONFIRMED', 'otro día'),
      ],
      '2026-11-24',
      ZONE,
    );

    expect(groups.map((group) => group.hour)).toEqual(['09:00', '10:00']);
    expect(groups[1].appointments.map((item) => item.id)).toEqual(['c', 'b']);
  });
});
