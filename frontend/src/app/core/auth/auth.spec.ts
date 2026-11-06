import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { TokenResponse } from '../api/models';
import { authInterceptor } from './auth.interceptor';
import { AuthService, homeFor } from './auth.service';
import { roleGuard } from './role.guard';

const REFRESH = '/api/v1/auth/refresh';
const DATA = '/api/v1/customers/me/appointments';

function tokens(accessToken: string): TokenResponse {
  return { accessToken, expiresIn: 900, role: 'CUSTOMER', profileId: 'c1' };
}

describe('sesión', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => backend.verify());

  function logIn(token = 'viejo'): void {
    auth.login('a@example.com', 'x').subscribe();
    backend.expectOne('/api/v1/auth/login').flush(tokens(token));
  }

  describe('AuthService', () => {
    it('el token de renovación nunca llega a JavaScript ni al almacenamiento', () => {
      logIn('secreto');

      expect(auth.accessToken()).toBe('secreto');
      expect(JSON.stringify({ ...localStorage })).not.toContain('secreto');
    });

    it('refresh y logout envían la cabecera anti-CSRF', () => {
      logIn();
      auth.refresh().subscribe();
      const refresh = backend.expectOne(REFRESH);
      expect(refresh.request.headers.get('X-Ozin-Client')).toBe('web');
      refresh.flush(tokens('nuevo'));

      auth.logout().subscribe();
      const logout = backend.expectOne('/api/v1/auth/logout');
      expect(logout.request.headers.get('X-Ozin-Client')).toBe('web');
      logout.flush(null);

      expect(auth.isAuthenticated()).toBe(false);
    });

    it('sin indicio de sesión no intenta renovar al cargar la página', async () => {
      await firstValueFrom(auth.restoreSession());

      backend.expectNone(REFRESH);
      expect(auth.isAuthenticated()).toBe(false);
    });

    it('recupera la sesión al recargar si había una', async () => {
      logIn();
      auth.closeSession();
      localStorage.setItem('ozin.hasSession', '1');

      const restored = firstValueFrom(auth.restoreSession());
      backend.expectOne(REFRESH).flush(tokens('recuperado'));
      await restored;

      expect(auth.accessToken()).toBe('recuperado');
    });

    it('si la sesión expiró, sigue como visitante sin fallar', async () => {
      localStorage.setItem('ozin.hasSession', '1');

      const restored = firstValueFrom(auth.restoreSession());
      backend.expectOne(REFRESH).flush(null, { status: 401, statusText: 'Unauthorized' });
      await restored;

      expect(auth.isAuthenticated()).toBe(false);
      expect(localStorage.getItem('ozin.hasSession')).toBeNull();
    });
  });

  describe('interceptor', () => {
    it('añade el token a las peticiones de la API', () => {
      logIn('abc');

      http.get(DATA).subscribe();

      const request = backend.expectOne(DATA);
      expect(request.request.headers.get('Authorization')).toBe('Bearer abc');
      request.flush({});
    });

    it('no envía el token a otros dominios', () => {
      logIn('abc');

      http.get('https://otro-sitio.example/api/v1/x').subscribe();

      const request = backend.expectOne('https://otro-sitio.example/api/v1/x');
      expect(request.request.headers.has('Authorization')).toBe(false);
      request.flush({});
    });

    it('si el token caducó, lo renueva y repite la petición sin que se note', () => {
      logIn('viejo');
      let result: unknown;

      http.get(DATA).subscribe((body) => (result = body));
      backend.expectOne(DATA).flush(null, { status: 401, statusText: 'Unauthorized' });
      backend.expectOne(REFRESH).flush(tokens('nuevo'));

      const retry = backend.expectOne(DATA);
      expect(retry.request.headers.get('Authorization')).toBe('Bearer nuevo');
      retry.flush({ ok: true });
      expect(result).toEqual({ ok: true });
    });

    it('varias peticiones que caducan a la vez comparten una sola renovación', () => {
      logIn('viejo');

      http.get(`${DATA}?a`).subscribe();
      http.get(`${DATA}?b`).subscribe();
      backend.expectOne(`${DATA}?a`).flush(null, { status: 401, statusText: 'Unauthorized' });
      backend.expectOne(`${DATA}?b`).flush(null, { status: 401, statusText: 'Unauthorized' });

      backend.expectOne(REFRESH).flush(tokens('nuevo'));

      backend.expectOne(`${DATA}?a`).flush({});
      backend.expectOne(`${DATA}?b`).flush({});
    });

    it('si no se puede renovar, cierra la sesión y lleva a iniciar sesión', () => {
      logIn('viejo');
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
      let failed = false;

      http.get(DATA).subscribe({ error: () => (failed = true) });
      backend.expectOne(DATA).flush(null, { status: 401, statusText: 'Unauthorized' });
      backend.expectOne(REFRESH).flush(null, { status: 401, statusText: 'Unauthorized' });

      expect(failed).toBe(true);
      expect(auth.isAuthenticated()).toBe(false);
      expect(navigate).toHaveBeenCalledWith(['/ingresar'], expect.anything());
    });

    it('un visitante con 401 no intenta renovar', () => {
      let failed = false;

      http.get(DATA).subscribe({ error: () => (failed = true) });
      backend.expectOne(DATA).flush(null, { status: 401, statusText: 'Unauthorized' });

      expect(failed).toBe(true);
      backend.expectNone(REFRESH);
    });

    it('otros errores llegan tal cual a quien hizo la petición', () => {
      logIn();
      let status = 0;

      http.get(DATA).subscribe({ error: (error) => (status = error.status) });
      backend.expectOne(DATA).flush(null, { status: 403, statusText: 'Forbidden' });

      expect(status).toBe(403);
      backend.expectNone(REFRESH);
    });
  });

  describe('roleGuard', () => {
    function runGuard(url = '/admin/agenda') {
      return TestBed.runInInjectionContext(() => roleGuard('ADMIN')({} as never, { url } as never));
    }

    it('un visitante va a iniciar sesión y conserva a dónde iba', () => {
      const result = runGuard() as UrlTree;

      expect(result.toString()).toBe('/ingresar?returnUrl=%2Fadmin%2Fagenda');
    });

    it('un usuario sin el rol va a su propia página', () => {
      logIn();

      expect((runGuard() as UrlTree).toString()).toBe(homeFor('CUSTOMER'));
    });

    it('deja pasar al rol permitido', () => {
      auth.login('a@example.com', 'x').subscribe();
      backend.expectOne('/api/v1/auth/login').flush({ ...tokens('t'), role: 'ADMIN', profileId: null });

      expect(runGuard()).toBe(true);
    });
  });
});
