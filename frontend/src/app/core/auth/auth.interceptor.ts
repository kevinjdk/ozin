import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';

const API_PREFIX = '/api/';
const AUTH_PREFIX = '/api/v1/auth/';

function withToken(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

/**
 * Añade el token a las peticiones de la API. Si caduca, lo renueva una vez y repite la petición,
 * sin que el usuario lo note. Si ya no se puede renovar, lo lleva a iniciar sesión.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(API_PREFIX) || request.url.startsWith(AUTH_PREFIX)) {
    return next(request);
  }

  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.accessToken();

  return next(token ? withToken(request, token) : request).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !token) {
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        catchError((refreshError: unknown) => {
          void router.navigate(['/ingresar'], { queryParams: { returnUrl: router.url } });
          return throwError(() => refreshError);
        }),
        switchMap((newToken) => next(withToken(request, newToken))),
      );
    }),
  );
};
