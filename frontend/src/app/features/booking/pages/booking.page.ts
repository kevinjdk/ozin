import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';

import { Appointment, PublicStylist, SalonService, Slot } from '../../../core/api/models';
import { SalonApi } from '../../../core/api/salon.api';
import { AuthService } from '../../../core/auth/auth.service';
import { Notifier } from '../../../core/errors/notifier.service';
import { problemCode, problemMessage } from '../../../core/errors/problem';
import { SalonContext } from '../../../core/time/salon-context.service';
import {
  addDays,
  formatDay,
  formatMoney,
  formatTime,
  localDateIn,
  localToPickerDate,
  pickerDateToLocal,
} from '../../../core/time/salon-time';
import { BookingDraft, BookingStep } from '../data-access/booking-draft.service';

const STEPS: { id: BookingStep; label: string }[] = [
  { id: 'service', label: 'Servicio' },
  { id: 'stylist', label: 'Estilista' },
  { id: 'slot', label: 'Horario' },
  { id: 'confirm', label: 'Confirmar' },
];
/** RN-05: el backend rechaza reservas a más de 30 días. */
const MAX_DAYS_AHEAD = 30;

/** HU-02 y HU-03: flujo de reserva. */
@Component({
  selector: 'app-booking-page',
  imports: [FormsModule, RouterLink, ButtonModule, DatePickerModule, MessageModule, SkeletonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './booking.page.scss',
  templateUrl: './booking.page.html',
})
export class BookingPage {
  private readonly api = inject(SalonApi);
  private readonly router = inject(Router);
  private readonly notifier = inject(Notifier);
  private readonly salon = inject(SalonContext);
  protected readonly auth = inject(AuthService);
  protected readonly draft = inject(BookingDraft);

  /** Servicio preseleccionado desde la portada (?servicio=id). */
  readonly servicio = input<string>();

  protected readonly steps = STEPS;
  protected readonly stepIndex = computed(() => STEPS.findIndex((step) => step.id === this.draft.step()));

  protected readonly services = signal<SalonService[] | null>(null);
  protected readonly stylists = signal<PublicStylist[] | null>(null);
  protected readonly slots = signal<Slot[] | null>(null);
  protected readonly loadError = signal<string | null>(null);
  protected readonly booking = signal(false);
  protected readonly booked = signal<Appointment | null>(null);

  private readonly today = localDateIn(new Date(), this.salon.zone());
  protected readonly date = signal(this.today);
  protected readonly pickerDate = computed(() => localToPickerDate(this.date()));
  protected readonly minDate = localToPickerDate(this.today);
  protected readonly maxDate = localToPickerDate(addDays(this.today, MAX_DAYS_AHEAD));

  protected readonly selectedDayLabel = computed(() => {
    const slot = this.draft.slot();
    return slot ? formatDay(slot.start, this.salon.zone()) : '';
  });

  constructor() {
    this.api.services().subscribe({
      next: (services) => {
        this.services.set(services);
        const preselected = services.find((service) => service.id === this.servicio());
        if (preselected) {
          this.draft.chooseService(preselected);
        }
      },
      error: (error: unknown) => this.loadError.set(problemMessage(error)),
    });

    effect(() => {
      const service = this.draft.service();
      if (service && !this.draft.stylist()) {
        this.loadStylists(service.id);
      }
    });

    effect(() => {
      const service = this.draft.service();
      const stylist = this.draft.stylist();
      const date = this.date();
      if (service && stylist && !this.draft.slot()) {
        this.loadSlots(stylist.id, service.id, date);
      }
    });
  }

  private loadStylists(serviceId: string): void {
    this.stylists.set(null);
    this.api.stylistsFor(serviceId).subscribe({
      next: (stylists) => this.stylists.set(stylists),
      error: (error: unknown) => this.loadError.set(problemMessage(error)),
    });
  }

  private loadSlots(stylistId: string, serviceId: string, date: string): void {
    this.slots.set(null);
    this.api.availability(stylistId, serviceId, date).subscribe({
      next: (slots) => this.slots.set(slots),
      error: (error: unknown) => this.loadError.set(problemMessage(error)),
    });
  }

  protected onDatePicked(date: Date | null): void {
    if (date) {
      this.date.set(pickerDateToLocal(date));
    }
  }

  protected time(iso: string): string {
    return formatTime(iso, this.salon.zone());
  }

  protected money(amount: number, currency: string): string {
    return formatMoney(amount, currency);
  }

  protected bookedDay(appointment: Appointment): string {
    return formatDay(appointment.start, this.salon.zone());
  }

  protected confirm(): void {
    const service = this.draft.service();
    const stylist = this.draft.stylist();
    const slot = this.draft.slot();
    if (!service || !stylist || !slot) {
      return;
    }
    if (!this.auth.isAuthenticated()) {
      void this.router.navigate(['/ingresar'], { queryParams: { returnUrl: '/reservar' } });
      return;
    }
    this.booking.set(true);
    this.api.book(stylist.id, service.id, slot.start).subscribe({
      next: (appointment) => {
        this.booking.set(false);
        this.booked.set(appointment);
        this.draft.reset();
      },
      error: (error: unknown) => {
        this.booking.set(false);
        this.notifier.error(error);
        // RN-01: otra persona ganó el horario. Se vuelve a la lista para ver los que siguen libres.
        if (problemCode(error) === 'RN-01') {
          this.draft.back();
        }
      },
    });
  }

  protected startOver(): void {
    this.booked.set(null);
    this.draft.reset();
  }
}
