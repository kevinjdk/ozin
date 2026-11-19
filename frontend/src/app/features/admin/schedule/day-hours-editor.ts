import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';

import { DayHours } from './weekly-schedule';

/**
 * Edita una jornada: entrada, salida y la pausa de mediodía. Con la casilla desmarcada la pausa no
 * se aplica y sus horas quedan deshabilitadas, pero se conservan por si se vuelve a marcar.
 */
@Component({
  selector: 'app-day-hours-editor',
  imports: [FormsModule, CheckboxModule, InputTextModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: grid;
      gap: 0.5rem;
      padding: 0.75rem;
      background: var(--ozin-canvas);
      border-radius: 0.625rem;
    }
    .label {
      font-size: 0.9375rem;
      font-weight: 600;
    }
    .label::first-letter {
      text-transform: uppercase;
    }
    .range {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
      align-items: center;
      gap: 0.5rem;
    }
    .range input {
      min-width: 0;
      width: 100%;
    }
    /* En móvil se quita el separador y se reduce el relleno para que la hora se lea completa. */
    @media (max-width: 30rem) {
      :host {
        padding: 0.625rem;
      }
      .range {
        grid-template-columns: 1fr 1fr;
        gap: 0.375rem;
      }
      .range span {
        display: none;
      }
      .range input {
        padding-inline: 0.5rem;
      }
    }
    .break-toggle {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.875rem;
    }
    .range--disabled {
      opacity: 0.5;
    }
  `,
  template: `
    <span class="label">{{ label() }}</span>

    <div class="range" role="group" [attr.aria-label]="'Jornada · ' + label()">
      <input pInputText type="time" [value]="hours().start" [attr.aria-label]="'Entrada · ' + label()"
             (input)="patch({ start: value($event) })" />
      <span class="muted" aria-hidden="true">a</span>
      <input pInputText type="time" [value]="hours().end" [attr.aria-label]="'Salida · ' + label()"
             (input)="patch({ end: value($event) })" />
    </div>

    <label class="break-toggle">
      <p-checkbox [binary]="true" [ngModel]="hours().hasBreak" (ngModelChange)="patch({ hasBreak: $event })"
                  [ariaLabel]="'Pausa de mediodía · ' + label()" />
      Pausa de mediodía
    </label>

    <div class="range" [class.range--disabled]="!hours().hasBreak" role="group"
         [attr.aria-label]="'Horas de la pausa · ' + label()">
      <input pInputText type="time" [value]="hours().breakStart" [disabled]="!hours().hasBreak"
             [attr.aria-label]="'Inicio de la pausa · ' + label()" (input)="patch({ breakStart: value($event) })" />
      <span class="muted" aria-hidden="true">a</span>
      <input pInputText type="time" [value]="hours().breakEnd" [disabled]="!hours().hasBreak"
             [attr.aria-label]="'Fin de la pausa · ' + label()" (input)="patch({ breakEnd: value($event) })" />
    </div>
  `,
})
export class DayHoursEditor {
  readonly hours = model.required<DayHours>();
  readonly label = input.required<string>();

  protected value(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  protected patch(change: Partial<DayHours>): void {
    this.hours.update((hours) => ({ ...hours, ...change }));
  }
}
