import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';

import { Branch } from '../api/models';
import { SalonApi } from '../api/salon.api';

const FALLBACK_ZONE = 'America/Guayaquil';

/** Sede en uso. El MVP trabaja con una sola, pero el modelo admite varias (ADR-0006). */
@Injectable({ providedIn: 'root' })
export class SalonContext {
  private readonly api = inject(SalonApi);
  readonly branch = signal<Branch | null>(null);
  readonly zone = computed(() => this.branch()?.timeZone ?? FALLBACK_ZONE);

  load(): Observable<void> {
    return this.api.branches().pipe(
      map((branches) => this.branch.set(branches[0] ?? null)),
      catchError(() => of(undefined)),
    );
  }
}
