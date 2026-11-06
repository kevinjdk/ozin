import { ChangeDetectionStrategy, Component, effect, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';

import { AppointmentView, Stylist } from '../../../core/api/models';
import { SalonApi } from '../../../core/api/salon.api';
import { problemMessage } from '../../../core/errors/problem';
import { SalonContext } from '../../../core/time/salon-context.service';
import { localDateIn } from '../../../core/time/salon-time';
import { AgendaActionRequest, AgendaList } from '../../../shared/agenda/agenda-list';
import { agendaRange, AgendaView } from '../../../shared/agenda/agenda-range';
import { AgendaToolbar } from '../../../shared/agenda/agenda-toolbar';
import { AppointmentActions } from '../../../shared/agenda/appointment-actions.service';

/** HU-13. */
@Component({
  selector: 'app-admin-agenda-page',
  imports: [FormsModule, ButtonModule, SelectModule, SkeletonModule, AgendaToolbar, AgendaList],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <header class="page__header">
        <h1>Agenda del salón</h1>
        <p-select [options]="stylists()" optionLabel="fullName" optionValue="id" [showClear]="true"
                  placeholder="Todos los estilistas" ariaLabel="Filtrar por estilista"
                  [ngModel]="stylistId()" (ngModelChange)="stylistId.set($event)" />
      </header>

      <app-agenda-toolbar [(date)]="date" [(view)]="view" [zone]="salon.zone()" />

      @if (appointments(); as list) {
        <app-agenda-list [appointments]="list" [zone]="salon.zone()" [showStylist]="true" [canCancel]="true"
                         (action)="run($event)" />
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
export class AdminAgendaPage implements OnInit {
  private readonly api = inject(SalonApi);
  private readonly actions = inject(AppointmentActions);
  protected readonly salon = inject(SalonContext);

  protected readonly date = signal(localDateIn(new Date(), this.salon.zone()));
  protected readonly view = signal<AgendaView>('day');
  protected readonly stylistId = signal<string | null>(null);
  protected readonly stylists = signal<Stylist[]>([]);
  protected readonly appointments = signal<AppointmentView[] | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => this.load());
  }

  ngOnInit(): void {
    this.api.allStylists().subscribe({ next: (stylists) => this.stylists.set(stylists) });
  }

  protected load(): void {
    const branch = this.salon.branch();
    const stylistId = this.stylistId();
    const range = agendaRange(this.date(), this.view(), this.salon.zone());
    if (!branch) {
      this.error.set('No se pudo cargar la sede.');
      return;
    }
    this.appointments.set(null);
    this.error.set(null);
    const request = stylistId ? this.api.stylistAgenda(stylistId, range) : this.api.branchAgenda(branch.id, range);
    request.subscribe({
      next: (appointments) => this.appointments.set(appointments),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  protected run(request: AgendaActionRequest): void {
    this.actions.run(request.appointment.id, request.action, () => this.load());
  }
}
