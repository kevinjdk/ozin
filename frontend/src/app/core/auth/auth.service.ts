import { HttpClient, HttpHeaders } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, finalize, map, Observable, of, shareReplay, tap, throwError } from 'rxjs';

import { RegisterInput, Role, TokenResponse } from '../api/models';

const AUTH_API = '/api/v1/auth';
/** Sin esta cabecera el backend rechaza refresh y logout: es la protección CSRF de la cookie (ADR-0008). */
const CSRF_HEADERS = new HttpHeaders({ 'X-Ozin-Client': 'web' });
/** Solo indica que puede haber sesión, para no pedir una renovación que seguro fallaría. No guarda ningún token. */
const SESSION_HINT_KEY = 'ozin.hasSession';

export interface Session {
  accessToken: string;
  role: Role;
  profileId: string | null;
}

/**
 * Estado de la sesión.
 *
 * El access token vive solo en memoria y el de renovación en una cookie HttpOnly que JavaScript no
 * puede leer, de modo que un script inyectado no puede llevarse una sesión de larga duración.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly sessionState = signal<Session | null>(null);
  private refreshInFlight: Observable<string> | null = null;

  readonly isAuthenticated = computed(() => this.sessionState() !== null);
  readonly role = computed(() => this.sessionState()?.role ?? null);
  readonly profileId = computed(() => this.sessionState()?.profileId ?? null);
  readonly accessToken = computed(() => this.sessionState()?.accessToken ?? null);

  login(email: string, password: string): Observable<Session> {
    return this.http
      .post<TokenResponse>(`${AUTH_API}/login`, { email, password })
      .pipe(map((tokens) => this.openSession(tokens)));
  }

  register(input: RegisterInput): Observable<void> {
    return this.http.post(`${AUTH_API}/register`, input).pipe(map(() => undefined));
  }

  /**
   * Renueva el access token. Si varias peticiones lo necesitan a la vez comparten una sola llamada,
   * porque cada token de renovación sirve una vez y reutilizarlo cerraría la sesión.
   */
  refresh(): Observable<string> {
    this.refreshInFlight ??= this.http
      .post<TokenResponse>(`${AUTH_API}/refresh`, null, { headers: CSRF_HEADERS })
      .pipe(
        map((tokens) => this.openSession(tokens).accessToken),
        catchError((error: unknown) => {
          this.closeSession();
          return throwError(() => error);
        }),
        finalize(() => (this.refreshInFlight = null)),
        shareReplay({ bufferSize: 1, refCount: true }),
      );
    return this.refreshInFlight;
  }

  /** Recupera la sesión al cargar la página. No falla: si no hay sesión, se sigue como visitante. */
  restoreSession(): Observable<void> {
    if (!readSessionHint()) {
      return of(undefined);
    }
    return this.refresh().pipe(
      map(() => undefined),
      catchError(() => of(undefined)),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${AUTH_API}/logout`, null, { headers: CSRF_HEADERS }).pipe(
      catchError(() => of(undefined)),
      tap(() => this.closeSession()),
      map(() => undefined),
    );
  }

  closeSession(): void {
    this.sessionState.set(null);
    writeSessionHint(false);
  }

  private openSession(tokens: TokenResponse): Session {
    const session: Session = {
      accessToken: tokens.accessToken,
      role: tokens.role,
      profileId: tokens.profileId,
    };
    this.sessionState.set(session);
    writeSessionHint(true);
    return session;
  }
}

/** Página de inicio de cada rol tras iniciar sesión. */
export function homeFor(role: Role | null): string {
  switch (role) {
    case 'ADMIN':
      return '/admin/agenda';
    case 'STYLIST':
      return '/agenda';
    case 'CUSTOMER':
      return '/mis-citas';
    default:
      return '/';
  }
}

function readSessionHint(): boolean {
  try {
    return localStorage.getItem(SESSION_HINT_KEY) === '1';
  } catch {
    return false;
  }
}

function writeSessionHint(value: boolean): void {
  try {
    if (value) {
      localStorage.setItem(SESSION_HINT_KEY, '1');
    } else {
      localStorage.removeItem(SESSION_HINT_KEY);
    }
  } catch {
    // Sin almacenamiento disponible solo se pierde la recuperación automática de la sesión.
  }
}
