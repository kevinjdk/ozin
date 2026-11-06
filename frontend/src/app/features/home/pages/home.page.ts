import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

/** Página de inicio provisional. Se reemplazará por la landing pública en la Fase 5. */
@Component({
  selector: 'app-home-page',
  imports: [ButtonModule, CardModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="home">
      <p-card>
        <h1>Ozin</h1>
        <p>Reserva tu cita en la peluquería en pocos pasos.</p>
        <p-button label="Reservar cita" icon="pi pi-calendar" [disabled]="true" />
      </p-card>
    </main>
  `,
  styles: `
    .home {
      display: grid;
      place-items: center;
      min-height: 100dvh;
      padding: 1rem;
    }
    h1 {
      margin: 0 0 0.5rem;
    }
  `,
})
export class HomePage {}
