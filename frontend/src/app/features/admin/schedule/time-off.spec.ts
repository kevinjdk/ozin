import { describeTimeOff, isCurrentOrUpcoming, timeOffInputError, toTimeOffPeriod } from './time-off';

const ZONE = 'America/Guayaquil'; // UTC-5, sin horario de verano

describe('time off', () => {
  it('turns whole days into a period from midnight to the midnight after the last day', () => {
    expect(toTimeOffPeriod({ kind: 'days', from: '2026-11-24', to: '2026-11-26' }, ZONE)).toEqual({
      start: '2026-11-24T05:00:00.000Z',
      end: '2026-11-27T05:00:00.000Z',
    });
  });

  it('turns a single whole day into a 24 hour period', () => {
    expect(toTimeOffPeriod({ kind: 'days', from: '2026-11-24', to: '2026-11-24' }, ZONE)).toEqual({
      start: '2026-11-24T05:00:00.000Z',
      end: '2026-11-25T05:00:00.000Z',
    });
  });

  it('turns some hours of a day into a period in the salon time zone', () => {
    expect(toTimeOffPeriod({ kind: 'hours', date: '2026-11-24', start: '10:00', end: '12:30' }, ZONE)).toEqual({
      start: '2026-11-24T15:00:00.000Z',
      end: '2026-11-24T17:30:00.000Z',
    });
  });

  it('validates the period before sending it', () => {
    expect(timeOffInputError({ kind: 'days', from: '2026-11-24', to: '2026-11-24' })).toBeNull();
    expect(timeOffInputError({ kind: 'days', from: '2026-11-24', to: '2026-11-23' })).toContain('anterior');
    expect(timeOffInputError({ kind: 'hours', date: '2026-11-24', start: '12:00', end: '12:00' })).toContain('posterior');
    expect(timeOffInputError({ kind: 'hours', date: '', start: '10:00', end: '12:00' })).not.toBeNull();
  });

  it('describes whole days and partial days in words', () => {
    const singleDay = toTimeOffPeriod({ kind: 'days', from: '2026-11-24', to: '2026-11-24' }, ZONE);
    const severalDays = toTimeOffPeriod({ kind: 'days', from: '2026-11-24', to: '2026-11-26' }, ZONE);
    const someHours = toTimeOffPeriod({ kind: 'hours', date: '2026-11-24', start: '10:00', end: '12:30' }, ZONE);

    expect(describeTimeOff({ ...singleDay, reason: '' }, ZONE)).toMatch(/^Martes, 24 de noviembre · día completo$/);
    expect(describeTimeOff({ ...severalDays, reason: '' }, ZONE)).toMatch(/^Del martes, 24 de noviembre al jueves, 26 de noviembre$/);
    expect(describeTimeOff({ ...someHours, reason: '' }, ZONE)).toMatch(/^Martes, 24 de noviembre · 10:00 a 12:30$/);
  });

  it('hides time off that has already ended', () => {
    const off = { start: '2026-11-24T15:00:00Z', end: '2026-11-24T17:00:00Z', reason: '' };

    expect(isCurrentOrUpcoming(off, new Date('2026-11-24T16:00:00Z'))).toBe(true);
    expect(isCurrentOrUpcoming(off, new Date('2026-11-24T17:00:00Z'))).toBe(false);
  });
});
