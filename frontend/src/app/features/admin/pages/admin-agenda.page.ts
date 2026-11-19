import { ChangeDetectionStrategy, Component, computed, effect, inject, OnInit, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SelectModule } from 'primeng/select';

import { AppointmentView, Stylist } from '../../../core/api/models';
import { SalonApi } from '../../../core/api/salon.api';
import { problemMessage } from '../../../core/errors/problem';
import { SalonContext } from '../../../core/time/salon-context.service';
import { localDateIn } from '../../../core/time/salon-time';
import { monthOf, monthRange } from '../../../shared/agenda/agenda-calendar';
import { AgendaBoard } from '../../../shared/agenda/agenda-board';
import { AgendaActionRequest } from '../../../shared/agenda/agenda-list';
import { AppointmentActions } from '../../../shared/agenda/appointment-actions.service';

/** HU-13: agenda de todo el salón, que se puede filtrar por estilista. */
@Component({
  selector: 'app-admin-agenda-page',
  imports: [FormsModule, SelectModule, AgendaBoard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page page--wide">
      <header class="page__header">
        <h1>Agenda del salón</h1>
        <p-select [options]="stylists()" optionLabel="fullName" optionValue="id" [showClear]="true"
                  placeholder="Todos los estilistas" ariaLabel="Filtrar por estilista"
                  [ngModel]="stylistId()" (ngModelChange)="stylistId.set($event)" />
      </header>

      <app-agenda-board [(date)]="date" [appointments]="appointments()" [error]="error()" [zone]="salon.zone()"
                        [showStylist]="!stylistId()" [canCancel]="true" (action)="run($event)" (retry)="load()" />
    </div>
  `,
})
export class AdminAgendaPage implements OnInit {
  private readonly api = inject(SalonApi);
  private readonly actions = inject(AppointmentActions);
  protected readonly salon = inject(SalonContext);

  protected readonly date = signal(localDateIn(new Date(), this.salon.zone()));
  /** Solo cambiar de mes o de estilista vuelve a pedir citas; elegir otro día del mismo mes no. */
  private readonly month = computed(() => monthOf(this.date()));
  protected readonly stylistId = signal<string | null>(null);
  protected readonly stylists = signal<Stylist[]>([]);
  protected readonly appointments = signal<AppointmentView[] | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.month();
      this.stylistId();
      this.salon.branch();
      untracked(() => this.load());
    });
  }

  ngOnInit(): void {
    this.api.allStylists().subscribe({ next: (stylists) => this.stylists.set(stylists) });
  }

  /** @param keepCurrent tras una acción se recarga sin vaciar la agenda, para que no parpadee */
  protected load(keepCurrent = false): void {
    const branch = this.salon.branch();
    if (!branch) {
      this.error.set('No se pudo cargar la sede.');
      return;
    }
    const stylistId = this.stylistId();
    const range = monthRange(this.month(), this.salon.zone());
    if (!keepCurrent) {
      this.appointments.set(null);
    }
    this.error.set(null);
    const request = stylistId ? this.api.stylistAgenda(stylistId, range) : this.api.branchAgenda(branch.id, range);
    request.subscribe({
      next: (appointments) => this.appointments.set(appointments),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  protected run(request: AgendaActionRequest): void {
    this.actions.run(request.appointment.id, request.action, () => this.load(true));
  }
}
