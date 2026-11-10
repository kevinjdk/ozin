import { HttpErrorResponse } from '@angular/common/http';

const GENERIC_MESSAGE = 'Ocurrió un error inesperado. Vuelve a intentarlo.';
const NETWORK_MESSAGE = 'No pudimos conectar con el servidor. Revisa tu conexión.';

/** Código estable de una respuesta RFC 9457 del backend, por ejemplo RN-01. */
export function problemCode(error: unknown): string | null {
  return error instanceof HttpErrorResponse ? (error.error?.code ?? null) : null;
}

/** Mensaje que se puede mostrar al usuario. El backend los redacta pensando en él. */
export function problemMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return GENERIC_MESSAGE;
  }
  if (error.status === 0) {
    return NETWORK_MESSAGE;
  }
  if (error.status >= 500) {
    return GENERIC_MESSAGE;
  }
  return error.error?.detail ?? GENERIC_MESSAGE;
}
