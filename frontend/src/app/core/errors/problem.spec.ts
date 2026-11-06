import { HttpErrorResponse } from '@angular/common/http';

import { problemCode, problemMessage } from './problem';

function httpError(status: number, error: unknown = null): HttpErrorResponse {
  return new HttpErrorResponse({ status, error });
}

describe('problem', () => {
  it('usa el mensaje y el código que envía el backend', () => {
    const error = httpError(409, { code: 'RN-01', detail: 'El horario ya no está disponible' });

    expect(problemCode(error)).toBe('RN-01');
    expect(problemMessage(error)).toBe('El horario ya no está disponible');
  });

  it('no muestra detalles internos en errores del servidor', () => {
    expect(problemMessage(httpError(500, { detail: 'NullPointerException en línea 42' }))).not.toContain('NullPointer');
  });

  it('distingue un fallo de conexión', () => {
    expect(problemMessage(httpError(0))).toContain('conexión');
  });

  it('tolera errores que no vienen de la API', () => {
    expect(problemCode(new Error('x'))).toBeNull();
    expect(problemMessage(new Error('x'))).toContain('error inesperado');
  });
});
