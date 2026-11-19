import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TagModule } from 'primeng/tag';

import { AppointmentStatus } from '../../core/api/models';

type Severity = 'info' | 'success' | 'secondary' | 'danger' | 'warn';

/**
 * Solo se etiquetan los estados que cambian algo para quien mira la tarjeta. Una cita confirmada es
 * el caso normal (toda cita nace así), por eso no lleva etiqueta.
 */
const PRESENTATION: Partial<Record<AppointmentStatus, { label: string; severity: Severity }>> = {
  COMPLETED: { label: 'Completada', severity: 'success' },
  CANCELLED: { label: 'Cancelada', severity: 'secondary' },
  NO_SHOW: { label: 'No asistió', severity: 'danger' },
};

@Component({
  selector: 'app-appointment-status-tag',
  imports: [TagModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (presentation(); as tag) {
      <p-tag [value]="tag.label" [severity]="tag.severity" />
    }
  `,
})
export class AppointmentStatusTag {
  readonly status = input.required<AppointmentStatus>();
  protected readonly presentation = computed(() => PRESENTATION[this.status()]);
}
