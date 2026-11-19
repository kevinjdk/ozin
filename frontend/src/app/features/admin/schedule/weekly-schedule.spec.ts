import { WorkingHours } from '../../../core/api/models';
import {
  breakSummary,
  defaultSchedule,
  describeSchedule,
  fromWorkingHours,
  scheduleErrors,
  toCustom,
  toWorkingHours,
  WeeklySchedule,
} from './weekly-schedule';

const ranges = (hours: WorkingHours[], day: string) =>
  hours.filter((range) => range.day === day).map(({ start, end }) => `${start}-${end}`);

describe('weekly schedule', () => {
  it('defaults to Monday to Saturday, 09:00 to 18:00 with a 13:00 to 14:00 break', () => {
    const hours = toWorkingHours(defaultSchedule());

    expect(ranges(hours, 'MONDAY')).toEqual(['09:00-13:00', '14:00-18:00']);
    expect(ranges(hours, 'SATURDAY')).toEqual(['09:00-13:00', '14:00-18:00']);
    expect(ranges(hours, 'SUNDAY')).toEqual([]);
  });

  it('uses one schedule for weekdays and another for the weekend in fixed mode', () => {
    const schedule = defaultSchedule();
    schedule.weekend = { start: '10:00', end: '14:00', hasBreak: false, breakStart: '13:00', breakEnd: '14:00' };

    const hours = toWorkingHours(schedule);

    expect(ranges(hours, 'FRIDAY')).toEqual(['09:00-13:00', '14:00-18:00']);
    expect(ranges(hours, 'SATURDAY')).toEqual(['10:00-14:00']);
  });

  it('switching to custom starts from the fixed schedule, so only the changed day needs editing', () => {
    const schedule = defaultSchedule();
    schedule.weekend = { start: '10:00', end: '14:00', hasBreak: false, breakStart: '13:00', breakEnd: '14:00' };

    const custom = toCustom(schedule);
    custom.days.WEDNESDAY = { ...custom.days.WEDNESDAY, end: '16:00' };
    const hours = toWorkingHours(custom);

    expect(custom.mode).toBe('custom');
    expect(ranges(hours, 'TUESDAY')).toEqual(['09:00-13:00', '14:00-18:00']);
    expect(ranges(hours, 'WEDNESDAY')).toEqual(['09:00-13:00', '14:00-16:00']);
    expect(ranges(hours, 'SATURDAY')).toEqual(['10:00-14:00']);
  });

  it('reads a saved schedule with a lunch break back as fixed with that break', () => {
    const saved = toWorkingHours(defaultSchedule()).map((range) => ({
      ...range,
      start: `${range.start}:00`,
      end: `${range.end}:00`,
    }));

    const schedule = fromWorkingHours(saved);

    expect(schedule.mode).toBe('fixed');
    expect(schedule.weekdays).toEqual({ start: '09:00', end: '18:00', hasBreak: true, breakStart: '13:00', breakEnd: '14:00' });
    expect(schedule.workDays).toHaveLength(6);
  });

  it('reads a schedule whose weekdays differ as custom', () => {
    const custom = toCustom(defaultSchedule());
    custom.days.MONDAY = { ...custom.days.MONDAY, hasBreak: false };

    expect(fromWorkingHours(toWorkingHours(custom)).mode).toBe('custom');
  });

  it('describes the lunch break as it is configured', () => {
    const schedule = defaultSchedule();
    expect(breakSummary(schedule)).toBe('Este horario tiene definida la pausa al mediodía de 13:00 a 14:00.');

    schedule.weekend = { ...schedule.weekend, hasBreak: false };
    expect(breakSummary(schedule)).toBe(
      'Este horario tiene definida la pausa al mediodía de 13:00 a 14:00, solo en algunos días.',
    );

    schedule.weekdays = { ...schedule.weekdays, hasBreak: false };
    expect(breakSummary(schedule)).toBe('Este horario no tiene pausa al mediodía: la jornada es continua.');
  });

  it('reports invalid hours only for the schedules in use', () => {
    const schedule: WeeklySchedule = { ...defaultSchedule(), workDays: ['MONDAY'] };
    schedule.weekend = { ...schedule.weekend, start: '18:00', end: '09:00' };
    expect(scheduleErrors(schedule)).toEqual([]);

    schedule.weekdays = { ...schedule.weekdays, breakStart: '08:00', breakEnd: '09:30' };
    expect(scheduleErrors(schedule)).toEqual(['Entre semana: la pausa debe quedar dentro de la jornada.']);

    const custom = toCustom(defaultSchedule());
    custom.days.TUESDAY = { ...custom.days.TUESDAY, end: '08:00' };
    expect(scheduleErrors(custom)).toEqual(['Martes: la salida debe ser después de la entrada.']);
  });

  it('summarises a schedule for the stylist table', () => {
    expect(describeSchedule(toWorkingHours(defaultSchedule()))).toBe('Lun a Sáb · 09:00–18:00, pausa 13:00–14:00');
    expect(describeSchedule([])).toBe('Sin horario');
    expect(
      describeSchedule([
        { day: 'MONDAY', start: '09:00', end: '18:00' },
        { day: 'WEDNESDAY', start: '10:00', end: '14:00' },
      ]),
    ).toBe('Lun, Mié · horario variable');
  });
});
