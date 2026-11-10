import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';

import { SalonService } from '../../../core/api/models';
import { SalonApi } from '../../../core/api/salon.api';
import { problemMessage } from '../../../core/errors/problem';
import { formatMoney } from '../../../core/time/salon-time';
import { SalonContext } from '../../../core/time/salon-context.service';

/** HU-01: portada pública con el catálogo de servicios. */
@Component({
  selector: 'app-home-page',
  imports: [RouterLink, ButtonModule, SkeletonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './home.page.scss',
  template: `
    <section class="hero">
      <div class="hero__inner">
        <p class="hero__eyebrow">{{ salon.branch()?.name ?? 'Peluquería' }}</p>
        <h1>Tu próximo corte, a dos minutos de distancia.</h1>
        <p class="hero__lead">
          Elige el servicio, la persona que te atiende y el horario que te queda bien. Sin llamadas ni esperas.
        </p>
        <p-button label="Reservar cita" icon="pi pi-calendar" size="large" routerLink="/reservar" />
      </div>
    </section>

    <section class="page catalog" aria-labelledby="catalogo">
      <h2 id="catalogo">Servicios</h2>

      @if (loading()) {
        <ul class="catalog__list" aria-hidden="true">
          @for (row of [1, 2, 3, 4]; track row) {
            <li class="catalog__row"><p-skeleton height="2.5rem" /></li>
          }
        </ul>
      } @else if (error()) {
        <div class="empty-state" role="alert">
          <p>{{ error() }}</p>
          <p-button label="Reintentar" severity="secondary" (onClick)="load()" />
        </div>
      } @else if (services().length === 0) {
        <p class="empty-state">Pronto publicaremos nuestros servicios.</p>
      } @else {
        <ul class="catalog__list">
          @for (service of services(); track service.id) {
            <li class="catalog__row">
              <div class="catalog__name">
                <h3>{{ service.name }}</h3>
                <span class="muted">{{ service.durationMinutes }} minutos</span>
              </div>
              <span class="catalog__price">{{ money(service) }}</span>
              <a class="catalog__link" routerLink="/reservar" [queryParams]="{ servicio: service.id }"
                 [attr.aria-label]="'Reservar ' + service.name">Reservar</a>
            </li>
          }
        </ul>
      }
    </section>

    <section class="steps" aria-labelledby="como-funciona">
      <div class="page">
        <h2 id="como-funciona">Cómo funciona</h2>
        <ol class="steps__list">
          <li><strong>Elige el servicio</strong><span>Con precio y duración claros desde el inicio.</span></li>
          <li><strong>Escoge quién te atiende</strong><span>Solo verás a quienes realizan ese servicio.</span></li>
          <li><strong>Reserva tu horario</strong><span>Ves los horarios libres reales. El precio queda fijado al reservar.</span></li>
        </ol>
      </div>
    </section>
  `,
})
export class HomePage implements OnInit {
  private readonly api = inject(SalonApi);
  protected readonly salon = inject(SalonContext);

  protected readonly services = signal<SalonService[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.services().subscribe({
      next: (services) => {
        this.services.set(services);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(problemMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected money(service: SalonService): string {
    return formatMoney(service.price, service.currency);
  }
}
