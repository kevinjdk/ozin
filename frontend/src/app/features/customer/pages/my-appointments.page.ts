import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';

import { AppointmentView, Page } from '../../../core/api/models';
import { SalonApi } from '../../../core/api/salon.api';
import { problemMessage } from '../../../core/errors/problem';
import { SalonContext } from '../../../core/time/salon-context.service';
import { formatDay, formatMoney, formatTime } from '../../../core/time/salon-time';
import { AppointmentActions } from '../../../shared/agenda/appointment-actions.service';
import { AppointmentStatusTag } from '../../../shared/ui/appointment-status-tag';

const PAGE_SIZE = 10;
/** RN-07: el backend lo vuelve a comprobar; aquí evita ofrecer un botón que fallaría. */
const CANCELLATION_NOTICE_MS = 24 * 60 * 60 * 1000;

/** HU-04 y HU-05. */
@Component({
  selector: 'app-my-appointments-page',
  imports: [RouterLink, ButtonModule, PaginatorModule, SkeletonModule, AppointmentStatusTag],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './my-appointments.page.scss',
  template: `
    <div class="page">
      <header class="page__header">
        <h1>Mis citas</h1>
        <p-button label="Nueva reserva" icon="pi pi-plus" routerLink="/reservar" />
      </header>

      @if (page(); as result) {
        @if (result.items.length === 0) {
          <div class="panel empty-state">
            <p>Todavía no tienes citas.</p>
            <p-button label="Reservar la primera" routerLink="/reservar" />
          </div>
        } @else {
          <ul class="list">
            @for (appointment of result.items; track appointment.id) {
              <li class="panel appointment">
                <div class="appointment__main">
                  <h2>{{ appointment.serviceName }}</h2>
                  <p class="appointment__when">{{ day(appointment) }}, {{ time(appointment) }}</p>
                  <p class="muted">Con {{ appointment.stylistName }} · {{ money(appointment) }}</p>
                </div>
                <div class="appointment__side">
                  <app-appointment-status-tag [status]="appointment.status" />
                  @if (isActive(appointment)) {
                    @if (canCancel(appointment)) {
                      <p-button label="Cancelar" severity="danger" size="small" [outlined]="true" (onClick)="cancel(appointment)" />
                    } @else {
                      <small class="muted appointment__hint">Falta poco: para cancelar, comunícate con el salón.</small>
                    }
                  }
                </div>
              </li>
            }
          </ul>
          @if (result.totalItems > pageSize) {
            <p-paginator [rows]="pageSize" [totalRecords]="result.totalItems" [first]="result.page * pageSize"
                         (onPageChange)="onPage($event)" />
          }
        }
      } @else if (error()) {
        <div class="panel empty-state" role="alert">
          <p>{{ error() }}</p>
          <p-button label="Reintentar" severity="secondary" (onClick)="load(0)" />
        </div>
      } @else {
        <div class="list" aria-hidden="true">
          <p-skeleton height="6rem" />
          <p-skeleton height="6rem" />
        </div>
      }
    </div>
  `,
})
export class MyAppointmentsPage implements OnInit {
  private readonly api = inject(SalonApi);
  private readonly salon = inject(SalonContext);
  private readonly actions = inject(AppointmentActions);

  protected readonly pageSize = PAGE_SIZE;
  protected readonly page = signal<Page<AppointmentView> | null>(null);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load(0);
  }

  protected load(page: number): void {
    this.error.set(null);
    this.api.myAppointments(page, PAGE_SIZE).subscribe({
      next: (result) => this.page.set(result),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  protected onPage(state: PaginatorState): void {
    this.load(state.page ?? 0);
  }

  protected day(appointment: AppointmentView): string {
    return formatDay(appointment.start, this.salon.zone());
  }

  protected time(appointment: AppointmentView): string {
    return formatTime(appointment.start, this.salon.zone());
  }

  protected money(appointment: AppointmentView): string {
    return formatMoney(appointment.price, appointment.currency);
  }

  protected isActive(appointment: AppointmentView): boolean {
    return (
      (appointment.status === 'PENDING' || appointment.status === 'CONFIRMED') &&
      new Date(appointment.start).getTime() > Date.now()
    );
  }

  protected canCancel(appointment: AppointmentView): boolean {
    return new Date(appointment.start).getTime() - Date.now() >= CANCELLATION_NOTICE_MS;
  }

  protected cancel(appointment: AppointmentView): void {
    this.actions.run(appointment.id, 'cancel', () => this.load(this.page()?.page ?? 0));
  }
}
