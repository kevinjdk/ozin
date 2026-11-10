import { inject, Injectable } from '@angular/core';
import { MessageService } from 'primeng/api';

import { problemMessage } from './problem';

@Injectable({ providedIn: 'root' })
export class Notifier {
  private readonly messages = inject(MessageService);

  success(detail: string): void {
    this.messages.add({ severity: 'success', summary: 'Listo', detail, life: 4000 });
  }

  error(error: unknown): void {
    this.messages.add({ severity: 'error', summary: 'No se pudo completar', detail: problemMessage(error), life: 6000 });
  }
}
