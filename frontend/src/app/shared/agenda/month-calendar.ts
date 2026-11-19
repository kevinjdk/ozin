import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { ButtonModule } from 'primeng/button';

import { formatDay, localToInstant } from '../../core/time/salon-time';
import { monthGrid, monthOf, shiftMonth } from './agenda-calendar';

const WEEKDAYS = ['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá', 'Do'];

/** Calendario del mes con los días que tienen citas pintados en rojo. Elegir un día lo selecciona. */
@Component({
  selector: 'app-month-calendar',
  imports: [ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './month-calendar.scss',
  template: `
    <div class="calendar__header">
      <p-button icon="pi pi-chevron-left" severity="secondary" [text]="true" [rounded]="true" ariaLabel="Mes anterior"
                (onClick)="move(-1)" />
      <h2 class="calendar__title" aria-live="polite">{{ title() }}</h2>
      <p-button icon="pi pi-chevron-right" severity="secondary" [text]="true" [rounded]="true" ariaLabel="Mes siguiente"
                (onClick)="move(1)" />
    </div>

    <table class="calendar__grid" [attr.aria-label]="title()">
      <thead>
        <tr>
          @for (weekday of weekdays; track weekday) {
            <th scope="col" class="calendar__weekday">{{ weekday }}</th>
          }
        </tr>
      </thead>
      <tbody>
        @for (week of weeks(); track week[0].date) {
          <tr>
            @for (day of week; track day.date) {
              <td>
                <button type="button" class="calendar__day"
                        [class.calendar__day--outside]="!day.inMonth"
                        [class.calendar__day--busy]="busy().has(day.date)"
                        [class.calendar__day--selected]="day.date === selected()"
                        [class.calendar__day--today]="day.date === today()"
                        [attr.aria-label]="label(day.date)"
                        [attr.aria-pressed]="day.date === selected()"
                        [attr.aria-current]="day.date === today() ? 'date' : null"
                        (click)="selected.set(day.date)">
                  {{ day.day }}
                </button>
              </td>
            }
          </tr>
        }
      </tbody>
    </table>

    <div class="calendar__footer">
      <span class="calendar__legend"><span class="calendar__swatch" aria-hidden="true"></span>Día con citas</span>
      <p-button label="Hoy" size="small" severity="secondary" [outlined]="true" (onClick)="selected.set(today())" />
    </div>
  `,
})
export class MonthCalendar {
  /** Día elegido; el mes que se muestra es el suyo. */
  readonly selected = model.required<string>();
  readonly today = input.required<string>();
  readonly busy = input.required<Set<string>>();
  readonly zone = input.required<string>();

  protected readonly weekdays = WEEKDAYS;
  protected readonly month = computed(() => monthOf(this.selected()));
  protected readonly weeks = computed(() => monthGrid(this.month()));
  protected readonly title = computed(() => {
    const [year, month] = this.month().split('-').map(Number);
    const text = new Intl.DateTimeFormat('es-EC', { month: 'long', year: 'numeric', timeZone: 'UTC' }).format(
      Date.UTC(year, month - 1, 1),
    );
    return text.charAt(0).toUpperCase() + text.slice(1);
  });

  protected label(date: string): string {
    const day = formatDay(localToInstant(date, this.zone()), this.zone());
    return this.busy().has(date) ? `${day}, con citas` : day;
  }

  /** Al cambiar de mes se elige hoy si cae en ese mes; si no, su primer día. */
  protected move(delta: number): void {
    const target = shiftMonth(this.month(), delta);
    this.selected.set(monthOf(this.today()) === target ? this.today() : `${target}-01`);
  }
}
