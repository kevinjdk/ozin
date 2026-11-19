import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';

import { AppointmentView } from '../../../core/api/models';
import { SalonApi } from '../../../core/api/salon.api';
import { AuthService } from '../../../core/auth/auth.service';
import { problemMessage } from '../../../core/errors/problem';
import { SalonContext } from '../../../core/time/salon-context.service';
import { localDateIn } from '../../../core/time/salon-time';
import { monthOf, monthRange } from '../../../shared/agenda/agenda-calendar';
import { AgendaBoard } from '../../../shared/agenda/agenda-board';
import { AgendaActionRequest } from '../../../shared/agenda/agenda-list';
import { AppointmentActions } from '../../../shared/agenda/appointment-actions.service';

/** HU-08 y HU-09: la agenda propia del estilista. */
@Component({
  selector: 'app-stylist-agenda-page',
  imports: [AgendaBoard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page page--wide">
      <header class="page__header"><h1>Mi agenda</h1></header>

      <app-agenda-board [(date)]="date" [appointments]="appointments()" [error]="error()" [zone]="salon.zone()"
                        (action)="run($event)" (retry)="load()" />
    </div>
  `,
})
export class StylistAgendaPage {
  private readonly api = inject(SalonApi);
  private readonly auth = inject(AuthService);
  private readonly actions = inject(AppointmentActions);
  protected readonly salon = inject(SalonContext);

  protected readonly date = signal(localDateIn(new Date(), this.salon.zone()));
  private readonly month = computed(() => monthOf(this.date()));
  protected readonly appointments = signal<AppointmentView[] | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.month();
      this.auth.profileId();
      untracked(() => this.load());
    });
  }

  /** @param keepCurrent tras una acción se recarga sin vaciar la agenda, para que no parpadee */
  protected load(keepCurrent = false): void {
    const stylistId = this.auth.profileId();
    if (!stylistId) {
      return;
    }
    if (!keepCurrent) {
      this.appointments.set(null);
    }
    this.error.set(null);
    this.api.stylistAgenda(stylistId, monthRange(this.month(), this.salon.zone())).subscribe({
      next: (appointments) => this.appointments.set(appointments),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  protected run(request: AgendaActionRequest): void {
    this.actions.run(request.appointment.id, request.action, () => this.load(true));
  }
}
