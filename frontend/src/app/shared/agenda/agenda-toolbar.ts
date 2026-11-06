import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { ButtonModule } from 'primeng/button';

import { addDays, formatDay, localDateIn, localToInstant, startOfWeek } from '../../core/time/salon-time';
import { AgendaView } from './agenda-range';

/** Navegación de una agenda por día o por semana. */
@Component({
  selector: 'app-agenda-toolbar',
  imports: [ButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.5rem 1rem;
      margin-bottom: 1rem;
    }
    .label {
      flex: 1 1 12rem;
      font-weight: 600;
      min-width: 0;
    }
    .label::first-letter {
      text-transform: uppercase;
    }
    .group {
      display: inline-flex;
      gap: 0.25rem;
    }
  `,
  template: `
    <div class="group">
      <p-button icon="pi pi-chevron-left" severity="secondary" [outlined]="true" ariaLabel="Anterior" (onClick)="move(-1)" />
      <p-button label="Hoy" severity="secondary" [outlined]="true" (onClick)="goToToday()" />
      <p-button icon="pi pi-chevron-right" severity="secondary" [outlined]="true" ariaLabel="Siguiente" (onClick)="move(1)" />
    </div>
    <span class="label" aria-live="polite">{{ label() }}</span>
    <div class="group" role="group" aria-label="Vista">
      <p-button label="Día" size="small" [severity]="view() === 'day' ? 'primary' : 'secondary'" [outlined]="view() !== 'day'"
                (onClick)="view.set('day')" />
      <p-button label="Semana" size="small" [severity]="view() === 'week' ? 'primary' : 'secondary'" [outlined]="view() !== 'week'"
                (onClick)="view.set('week')" />
    </div>
  `,
})
export class AgendaToolbar {
  readonly date = model.required<string>();
  readonly view = model.required<AgendaView>();
  readonly zone = input.required<string>();

  protected readonly label = computed(() => {
    const day = (localDate: string) => formatDay(localToInstant(localDate, this.zone()), this.zone());
    if (this.view() === 'day') {
      return day(this.date());
    }
    const monday = startOfWeek(this.date());
    return `${day(monday)} al ${day(addDays(monday, 6))}`;
  });

  protected move(direction: number): void {
    this.date.update((date) => addDays(date, direction * (this.view() === 'week' ? 7 : 1)));
  }

  protected goToToday(): void {
    this.date.set(localDateIn(new Date(), this.zone()));
  }
}
