import { ChangeDetectionStrategy, Component, computed, input, model, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';

import { AppointmentView } from '../../core/api/models';
import { localDateIn } from '../../core/time/salon-time';
import { busyDays } from './agenda-calendar';
import { AgendaActionRequest, AgendaList } from './agenda-list';
import { MonthCalendar } from './month-calendar';

/**
 * Agenda mensual compartida por el administrador y el estilista: el calendario del mes a la
 * izquierda y las citas del día elegido, separadas por hora, a la derecha.
 */
@Component({
  selector: 'app-agenda-board',
  imports: [ButtonModule, SkeletonModule, AgendaList, MonthCalendar],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: 1.5rem;
      align-items: start;
    }
    @media (min-width: 56rem) {
      :host {
        grid-template-columns: minmax(18rem, 21rem) 1fr;
      }
      app-month-calendar {
        position: sticky;
        top: 1rem;
      }
    }
  `,
  template: `
    <app-month-calendar [(selected)]="date" [today]="today()" [busy]="busy()" [zone]="zone()" />

    <div aria-live="polite" [attr.aria-busy]="appointments() === null && !error()">
      @if (appointments(); as list) {
        <app-agenda-list [appointments]="list" [date]="date()" [zone]="zone()" [showStylist]="showStylist()"
                         [canCancel]="canCancel()" (action)="action.emit($event)" />
      } @else if (error()) {
        <div class="panel empty-state" role="alert">
          <p>{{ error() }}</p>
          <p-button label="Reintentar" severity="secondary" (onClick)="retry.emit()" />
        </div>
      } @else {
        <p-skeleton height="2rem" width="60%" [style]="{ marginBottom: '1rem' }" />
        <p-skeleton height="10rem" />
      }
    </div>
  `,
})
export class AgendaBoard {
  readonly date = model.required<string>();
  /** Citas del mes visible, o null mientras se cargan. */
  readonly appointments = input.required<AppointmentView[] | null>();
  readonly error = input<string | null>(null);
  readonly zone = input.required<string>();
  readonly showStylist = input(false);
  readonly canCancel = input(false);
  readonly action = output<AgendaActionRequest>();
  readonly retry = output<void>();

  protected readonly today = computed(() => localDateIn(new Date(), this.zone()));
  protected readonly busy = computed(() => busyDays(this.appointments() ?? [], this.zone()));
}
