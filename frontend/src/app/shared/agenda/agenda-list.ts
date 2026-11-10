import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';

import { AppointmentView } from '../../core/api/models';
import { AppointmentAction } from '../../core/api/salon.api';
import { formatDay, formatTime } from '../../core/time/salon-time';
import { AppointmentStatusTag } from '../ui/appointment-status-tag';

export interface AgendaActionRequest {
  appointment: AppointmentView;
  action: AppointmentAction;
}

/** Citas agrupadas por día, con las acciones que el estado de cada una permite (HU-08, HU-09, HU-13). */
@Component({
  selector: 'app-agenda-list',
  imports: [ButtonModule, AppointmentStatusTag],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './agenda-list.scss',
  template: `
    @for (group of groups(); track group.day) {
      <section class="day">
        <h2 class="day__title">{{ group.day }}</h2>
        <ul class="day__list">
          @for (appointment of group.appointments; track appointment.id) {
            <li class="item">
              <div class="item__time">{{ time(appointment.start) }}<span>{{ time(appointment.end) }}</span></div>
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
              <div class="item__actions">
                @if (appointment.status === 'PENDING') {
                  <p-button label="Confirmar" size="small" (onClick)="request(appointment, 'confirm')" />
                }
                @if (appointment.status === 'CONFIRMED' && hasStarted(appointment)) {
                  <p-button label="Completar" size="small" (onClick)="request(appointment, 'complete')" />
                  <p-button label="No asistió" size="small" severity="secondary" [outlined]="true"
                            (onClick)="request(appointment, 'no-show')" />
                }
                @if (canCancel() && isActive(appointment)) {
                  <p-button label="Cancelar" size="small" severity="danger" [text]="true"
                            (onClick)="request(appointment, 'cancel')" />
                }
              </div>
            </li>
          }
        </ul>
      </section>
    } @empty {
      <p class="empty-state">No hay citas en este periodo.</p>
    }
  `,
})
export class AgendaList {
  readonly appointments = input.required<AppointmentView[]>();
  readonly zone = input.required<string>();
  readonly showStylist = input(false);
  readonly canCancel = input(false);
  readonly action = output<AgendaActionRequest>();

  protected readonly groups = computed(() => {
    const groups = new Map<string, AppointmentView[]>();
    for (const appointment of this.appointments()) {
      const day = formatDay(appointment.start, this.zone());
      groups.set(day, [...(groups.get(day) ?? []), appointment]);
    }
    return [...groups.entries()].map(([day, appointments]) => ({ day, appointments }));
  });

  protected time(iso: string): string {
    return formatTime(iso, this.zone());
  }

  protected hasStarted(appointment: AppointmentView): boolean {
    return new Date(appointment.start).getTime() <= Date.now();
  }

  protected isActive(appointment: AppointmentView): boolean {
    return appointment.status === 'PENDING' || appointment.status === 'CONFIRMED';
  }

  protected request(appointment: AppointmentView, action: AppointmentAction): void {
    this.action.emit({ appointment, action });
  }
}
