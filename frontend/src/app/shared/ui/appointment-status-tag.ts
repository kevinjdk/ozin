import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TagModule } from 'primeng/tag';

import { AppointmentStatus } from '../../core/api/models';

type Severity = 'info' | 'success' | 'secondary' | 'danger' | 'warn';

const PRESENTATION: Record<AppointmentStatus, { label: string; severity: Severity }> = {
  PENDING: { label: 'Pendiente', severity: 'warn' },
  CONFIRMED: { label: 'Confirmada', severity: 'info' },
  COMPLETED: { label: 'Completada', severity: 'success' },
  CANCELLED: { label: 'Cancelada', severity: 'secondary' },
  NO_SHOW: { label: 'No asistió', severity: 'danger' },
};

@Component({
  selector: 'app-appointment-status-tag',
  imports: [TagModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<p-tag [value]="presentation().label" [severity]="presentation().severity" />`,
})
export class AppointmentStatusTag {
  readonly status = input.required<AppointmentStatus>();
  protected readonly presentation = computed(() => PRESENTATION[this.status()]);
}
