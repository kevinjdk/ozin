import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DayOfWeek, SalonService, Stylist, WorkingHours } from '../../../core/api/models';
import { SalonApi } from '../../../core/api/salon.api';
import { Notifier } from '../../../core/errors/notifier.service';
import { problemMessage } from '../../../core/errors/problem';
import { SalonContext } from '../../../core/time/salon-context.service';
import { formatDay, formatTime, localToInstant } from '../../../core/time/salon-time';

const DAYS: { value: DayOfWeek; label: string }[] = [
  { value: 'MONDAY', label: 'Lunes' },
  { value: 'TUESDAY', label: 'Martes' },
  { value: 'WEDNESDAY', label: 'Miércoles' },
  { value: 'THURSDAY', label: 'Jueves' },
  { value: 'FRIDAY', label: 'Viernes' },
  { value: 'SATURDAY', label: 'Sábado' },
  { value: 'SUNDAY', label: 'Domingo' },
];
const PASSWORD_POLICY = /^(?=.*[A-ZÁÉÍÓÚÑ])(?=.*\d).{8,}$/;

/** HU-11 y HU-12: estilistas, sus servicios, su horario semanal y sus ausencias. */
@Component({
  selector: 'app-admin-stylists-page',
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    MultiSelectModule,
    PasswordModule,
    SelectModule,
    SkeletonModule,
    TableModule,
    TagModule,
    ToggleSwitchModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './admin-stylists.page.scss',
  templateUrl: './admin-stylists.page.html',
})
export class AdminStylistsPage implements OnInit {
  private readonly api = inject(SalonApi);
  private readonly notifier = inject(Notifier);
  private readonly salon = inject(SalonContext);
  private readonly fb = inject(FormBuilder);

  protected readonly days = DAYS;
  protected readonly stylists = signal<Stylist[] | null>(null);
  protected readonly services = signal<SalonService[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);

  protected readonly dialogOpen = signal(false);
  protected readonly editing = signal<Stylist | null>(null);
  protected readonly timeOffFor = signal<Stylist | null>(null);
  protected readonly timeOffOpen = computed(() => this.timeOffFor() !== null);

  protected readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.pattern(PASSWORD_POLICY)]],
    active: [true],
    serviceIds: [[] as string[]],
    workingHours: this.fb.array<ReturnType<AdminStylistsPage['hoursRow']>>([]),
  });

  protected readonly timeOffForm = this.fb.nonNullable.group({
    start: ['', Validators.required],
    end: ['', Validators.required],
    reason: ['', Validators.maxLength(255)],
  });

  protected get hours(): FormArray<ReturnType<AdminStylistsPage['hoursRow']>> {
    return this.form.controls.workingHours;
  }

  ngOnInit(): void {
    this.load();
    this.api.allServices().subscribe({ next: (services) => this.services.set(services) });
  }

  protected load(): void {
    this.error.set(null);
    this.api.allStylists().subscribe({
      next: (stylists) => this.stylists.set(stylists),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  private hoursRow(hours?: WorkingHours) {
    return this.fb.nonNullable.group({
      day: [hours?.day ?? ('MONDAY' as DayOfWeek), Validators.required],
      start: [hours?.start.slice(0, 5) ?? '09:00', Validators.required],
      end: [hours?.end.slice(0, 5) ?? '18:00', Validators.required],
    });
  }

  protected addHours(): void {
    this.hours.push(this.hoursRow());
  }

  protected removeHours(index: number): void {
    this.hours.removeAt(index);
  }

  protected serviceNames(stylist: Stylist): string {
    const names = stylist.serviceIds.map((id) => this.services().find((service) => service.id === id)?.name);
    return names.filter(Boolean).join(', ') || 'Sin servicios';
  }

  protected openCreate(): void {
    this.editing.set(null);
    this.form.controls.email.enable();
    this.form.controls.password.enable();
    this.form.reset({ fullName: '', email: '', password: '', active: true, serviceIds: [] });
    this.hours.clear();
    this.dialogOpen.set(true);
  }

  protected openEdit(stylist: Stylist): void {
    this.editing.set(stylist);
    // Las credenciales no se editan aquí: deshabilitarlas las saca de la validación.
    this.form.controls.email.disable();
    this.form.controls.password.disable();
    this.form.reset({
      fullName: stylist.fullName,
      email: '',
      password: '',
      active: stylist.active,
      serviceIds: stylist.serviceIds,
    });
    this.hours.clear();
    stylist.workingHours.forEach((hours) => this.hours.push(this.hoursRow(hours)));
    this.dialogOpen.set(true);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const editing = this.editing();
    const branchId = this.salon.branch()?.id;
    if (!editing && !branchId) {
      this.error.set('No se pudo cargar la sede.');
      return;
    }
    const request = editing
      ? this.api.updateStylist(editing.id, {
          fullName: value.fullName,
          active: value.active,
          serviceIds: value.serviceIds,
          workingHours: value.workingHours,
        })
      : this.api.createStylist({
          branchId: branchId!,
          fullName: value.fullName,
          email: value.email,
          password: value.password,
          serviceIds: value.serviceIds,
          workingHours: value.workingHours,
        });

    this.saving.set(true);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogOpen.set(false);
        this.notifier.success('Estilista guardado.');
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.notifier.error(error);
      },
    });
  }

  protected openTimeOff(stylist: Stylist): void {
    this.timeOffForm.reset({ start: '', end: '', reason: '' });
    this.timeOffFor.set(stylist);
  }

  protected timeOffRange(start: string, end: string): string {
    const zone = this.salon.zone();
    return `${formatDay(start, zone)} ${formatTime(start, zone)} a ${formatDay(end, zone)} ${formatTime(end, zone)}`;
  }

  protected saveTimeOff(): void {
    const stylist = this.timeOffFor();
    if (!stylist) {
      return;
    }
    if (this.timeOffForm.invalid) {
      this.timeOffForm.markAllAsTouched();
      return;
    }
    const { start, end, reason } = this.timeOffForm.getRawValue();
    const zone = this.salon.zone();
    this.saving.set(true);
    this.api
      .addTimeOff(stylist.id, { start: localToInstant(start, zone), end: localToInstant(end, zone), reason })
      .subscribe({
        next: (updated) => {
          this.saving.set(false);
          this.timeOffFor.set(updated);
          this.timeOffForm.reset({ start: '', end: '', reason: '' });
          this.notifier.success('Ausencia registrada.');
          this.load();
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.notifier.error(error);
        },
      });
  }
}
