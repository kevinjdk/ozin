import {
  addDays,
  formatTime,
  instantToLocal,
  localDateIn,
  localToInstant,
  localToPickerDate,
  pickerDateToLocal,
  startOfWeek,
} from './salon-time';

const ECUADOR = 'America/Guayaquil';

describe('salon-time', () => {
  it('muestra la hora de la sede, no la del dispositivo', () => {
    expect(formatTime('2026-11-09T15:00:00Z', ECUADOR)).toBe('10:00');
    expect(formatTime('2026-11-09T15:00:00Z', 'Asia/Tokyo')).toBe('00:00');
  });

  it('convierte una hora local de la sede a UTC', () => {
    expect(localToInstant('2026-11-09T10:00', ECUADOR)).toBe('2026-11-09T15:00:00.000Z');
    expect(localToInstant('2026-11-09', ECUADOR)).toBe('2026-11-09T05:00:00.000Z');
  });

  it('respeta el cambio de horario en zonas que lo tienen', () => {
    expect(localToInstant('2026-07-01T10:00', 'Europe/Madrid')).toBe('2026-07-01T08:00:00.000Z');
    expect(localToInstant('2026-12-01T10:00', 'Europe/Madrid')).toBe('2026-12-01T09:00:00.000Z');
  });

  it('ida y vuelta entre instante y hora local', () => {
    expect(instantToLocal(localToInstant('2026-11-09T18:30', ECUADOR), ECUADOR)).toBe('2026-11-09T18:30');
  });

  it('la fecha local puede ser distinta de la fecha en UTC', () => {
    expect(localDateIn(new Date('2026-11-10T02:00:00Z'), ECUADOR)).toBe('2026-11-09');
  });

  it('suma días cruzando meses y años', () => {
    expect(addDays('2026-11-30', 1)).toBe('2026-12-01');
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
  });

  it('la semana empieza en lunes, también si la fecha es domingo', () => {
    expect(startOfWeek('2026-11-09')).toBe('2026-11-09');
    expect(startOfWeek('2026-11-12')).toBe('2026-11-09');
    expect(startOfWeek('2026-11-15')).toBe('2026-11-09');
  });

  it('la fecha del calendario no se desplaza por la zona del dispositivo', () => {
    expect(pickerDateToLocal(localToPickerDate('2026-11-09'))).toBe('2026-11-09');
  });
});
