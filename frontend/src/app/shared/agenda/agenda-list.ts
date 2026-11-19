import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';

import { AppointmentView } from '../../core/api/models';
import { AppointmentAction } from '../../core/api/salon.api';
import { formatDay, formatTime, localToInstant } from '../../core/time/salon-time';
import { AppointmentStatusTag } from '../ui/appointment-status-tag';
import { groupByHour } from './agenda-calendar';

export interface AgendaActionRequest {
  appointment: AppointmentView;
  action: AppointmentAction;
}

/**
 * Citas de un día separadas por hora, con las acciones que permite cada una (HU-08, HU-09, HU-13).
 * Las citas nacen confirmadas: el estilista solo registra si el cliente asistió o no.
 */
@Component({
  selector: 'app-agenda-list',
  imports: [ButtonModule, AppointmentStatusTag],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './agenda-list.scss',
  template: `
    <header class="list__header">
      <h2 class="list__title">{{ title() }}</h2>
      <span class="muted" aria-live="polite">{{ summary() }}</span>
    </header>

    @for (group of groups(); track group.hour) {
      <section class="hour" [attr.aria-label]="'Citas de las ' + group.hour">
        <h3 class="hour__label">{{ group.hour }}</h3>
        <ul class="hour__list">
          @for (appointment of group.appointments; track appointment.id) {
            <li class="item" [class.item--cancelled]="appointment.status === 'CANCELLED'">
              <div class="item__time">{{ time(appointment.start) }}<span>a {{ time(appointment.end) }}</span></div>
              <div class="item__body">
                <strong>{{ appointment.serviceName }}</strong>
                <span class="muted">
                  {{ appointment.customerName }}
                  @if (showStylist()) {
                    · con {{ appointment.stylistName }}
                  }
                </span>
              </div>
              <app-appointment-status-tag [status]="appointment.status" />
              @if (appointment.status === 'CONFIRMED') {
                <div class="item__actions">
                  @if (hasStarted(appointment)) {
                    <p-button label="Asistió" icon="pi pi-check" size="small" (onClick)="request(appointment, 'complete')" />
                    <p-button label="No asistió" size="small" severity="secondary" [outlined]="true"
                              (onClick)="request(appointment, 'no-show')" />
                  }
                  @if (canCancel()) {
                    <p-button label="Cancelar" size="small" severity="danger" [text]="true"
                              (onClick)="request(appointment, 'cancel')" />
                  }
                </div>
              }
            </li>
          }
        </ul>
      </section>
    } @empty {
      <p class="empty-state">No hay citas este día.</p>
    }
  `,
})
export class AgendaList {
  /** Citas del periodo cargado; la lista muestra solo las del día elegido. */
  readonly appointments = input.required<AppointmentView[]>();
  readonly date = input.required<string>();
  readonly zone = input.required<string>();
  readonly showStylist = input(false);
  readonly canCancel = input(false);
  readonly action = output<AgendaActionRequest>();

  protected readonly groups = computed(() => groupByHour(this.appointments(), this.date(), this.zone()));

  protected readonly title = computed(() => {
    const text = formatDay(localToInstant(this.date(), this.zone()), this.zone());
    return text.charAt(0).toUpperCase() + text.slice(1);
  });

  protected readonly summary = computed(() => {
    const active = this.groups()
      .flatMap((group) => group.appointments)
      .filter((appointment) => appointment.status !== 'CANCELLED').length;
    return active === 1 ? '1 cita' : `${active} citas`;
  });

  protected time(iso: string): string {
    return formatTime(iso, this.zone());
  }

  protected hasStarted(appointment: AppointmentView): boolean {
    return new Date(appointment.start).getTime() <= Date.now();
  }

  protected request(appointment: AppointmentView, action: AppointmentAction): void {
    this.action.emit({ appointment, action });
  }
}
