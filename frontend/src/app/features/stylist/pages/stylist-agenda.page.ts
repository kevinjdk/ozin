import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';

import { AppointmentView } from '../../../core/api/models';
import { SalonApi } from '../../../core/api/salon.api';
import { AuthService } from '../../../core/auth/auth.service';
import { problemMessage } from '../../../core/errors/problem';
import { SalonContext } from '../../../core/time/salon-context.service';
import { localDateIn } from '../../../core/time/salon-time';
import { AgendaActionRequest, AgendaList } from '../../../shared/agenda/agenda-list';
import { agendaRange, AgendaView } from '../../../shared/agenda/agenda-range';
import { AgendaToolbar } from '../../../shared/agenda/agenda-toolbar';
import { AppointmentActions } from '../../../shared/agenda/appointment-actions.service';

/** HU-08 y HU-09. */
@Component({
  selector: 'app-stylist-agenda-page',
  imports: [ButtonModule, SkeletonModule, AgendaToolbar, AgendaList],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <header class="page__header"><h1>Mi agenda</h1></header>

      <app-agenda-toolbar [(date)]="date" [(view)]="view" [zone]="salon.zone()" />

      @if (appointments(); as list) {
        <app-agenda-list [appointments]="list" [zone]="salon.zone()" (action)="run($event)" />
      } @else if (error()) {
        <div class="panel empty-state" role="alert">
          <p>{{ error() }}</p>
          <p-button label="Reintentar" severity="secondary" (onClick)="load()" />
        </div>
      } @else {
        <p-skeleton height="10rem" />
      }
    </div>
  `,
})
export class StylistAgendaPage {
  private readonly api = inject(SalonApi);
  private readonly auth = inject(AuthService);
  private readonly actions = inject(AppointmentActions);
  protected readonly salon = inject(SalonContext);

  protected readonly date = signal(localDateIn(new Date(), this.salon.zone()));
  protected readonly view = signal<AgendaView>('day');
  protected readonly appointments = signal<AppointmentView[] | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => this.load());
  }

  protected load(): void {
    const stylistId = this.auth.profileId();
    if (!stylistId) {
      return;
    }
    this.appointments.set(null);
    this.error.set(null);
    this.api.stylistAgenda(stylistId, agendaRange(this.date(), this.view(), this.salon.zone())).subscribe({
      next: (appointments) => this.appointments.set(appointments),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  protected run(request: AgendaActionRequest): void {
    this.actions.run(request.appointment.id, request.action, () => this.load());
  }
}
