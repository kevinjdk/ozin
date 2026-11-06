import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Role } from '../api/models';
import { AuthService, homeFor } from './auth.service';

/**
 * Evita mostrar pantallas que el usuario no puede usar. Es solo comodidad:
 * quien protege de verdad los datos es el backend.
 */
export function roleGuard(...allowed: Role[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const role = auth.role();

    if (!role) {
      return router.createUrlTree(['/ingresar'], { queryParams: { returnUrl: state.url } });
    }
    return allowed.includes(role) ? true : router.parseUrl(homeFor(role));
  };
}
