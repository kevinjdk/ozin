import { inject, Injectable } from '@angular/core';
import { ConfirmationService } from 'primeng/api';

import { AppointmentAction, SalonApi } from '../../core/api/salon.api';
import { Notifier } from '../../core/errors/notifier.service';

const DONE: Record<AppointmentAction, string> = {
  confirm: 'Cita confirmada.',
  complete: 'Cita completada.',
  'no-show': 'Cita marcada como no asistida.',
  cancel: 'Cita cancelada.',
};

/** Las acciones que no se pueden deshacer piden confirmación. */
const CONFIRMATION: Partial<Record<AppointmentAction, { header: string; message: string }>> = {
  cancel: { header: 'Cancelar cita', message: '¿Seguro que quieres cancelar esta cita? El horario quedará libre.' },
  'no-show': { header: 'Marcar inasistencia', message: '¿Confirmas que el cliente no asistió a la cita?' },
};

@Injectable({ providedIn: 'root' })
export class AppointmentActions {
  private readonly api = inject(SalonApi);
  private readonly confirmation = inject(ConfirmationService);
  private readonly notifier = inject(Notifier);

  /** @param onChange se llama tanto si la acción tiene éxito como si falla, porque la cita pudo cambiar */
  run(appointmentId: string, action: AppointmentAction, onChange: () => void): void {
    const execute = () =>
      this.api.applyAction(appointmentId, action).subscribe({
        next: () => {
          this.notifier.success(DONE[action]);
          onChange();
        },
        error: (error: unknown) => {
          this.notifier.error(error);
          onChange();
        },
      });

    const prompt = CONFIRMATION[action];
    if (!prompt) {
      execute();
      return;
    }
    this.confirmation.confirm({
      ...prompt,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Sí, continuar',
      rejectLabel: 'Volver',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: execute,
    });
  }
}
