import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { MultiSelectModule } from 'primeng/multiselect';
import { PasswordModule } from 'primeng/password';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DayOfWeek, SalonService, Stylist, TimeOff } from '../../../core/api/models';
import { SalonApi } from '../../../core/api/salon.api';
import { Notifier } from '../../../core/errors/notifier.service';
import { problemMessage } from '../../../core/errors/problem';
import { SalonContext } from '../../../core/time/salon-context.service';
import { localDateIn } from '../../../core/time/salon-time';
import { describeTimeOff, isCurrentOrUpcoming, timeOffInputError, TimeOffInput, toTimeOffPeriod } from '../schedule/time-off';
import { DayHoursEditor } from '../schedule/day-hours-editor';
import {
  breakSummary,
  DayHours,
  defaultSchedule,
  describeSchedule,
  fromWorkingHours,
  isWeekend,
  ScheduleMode,
  scheduleErrors,
  toCustom,
  toWorkingHours,
  WEEK,
  WeeklySchedule,
} from '../schedule/weekly-schedule';

const PASSWORD_POLICY = /^(?=.*[A-ZÁÉÍÓÚÑ])(?=.*\d).{8,}$/;

/** HU-11 y HU-12: estilistas, sus servicios, su horario semanal y sus ausencias. */
@Component({
  selector: 'app-admin-stylists-page',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    DayHoursEditor,
    ButtonModule,
    DialogModule,
    InputTextModule,
    MessageModule,
    MultiSelectModule,
    PasswordModule,
    SelectButtonModule,
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
  private readonly confirmation = inject(ConfirmationService);
  private readonly salon = inject(SalonContext);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly dayOptions = WEEK.map(({ day, short, long }) => ({ value: day, label: short, long }));
  protected readonly modeOptions: { value: ScheduleMode; label: string }[] = [
    { value: 'fixed', label: 'Fijo' },
    { value: 'custom', label: 'Personalizado' },
  ];
  protected readonly timeOffKinds: { value: TimeOffInput['kind']; label: string }[] = [
    { value: 'days', label: 'Días completos' },
    { value: 'hours', label: 'Unas horas' },
  ];

  protected readonly stylists = signal<Stylist[] | null>(null);
  protected readonly services = signal<SalonService[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);

  protected readonly dialogOpen = signal(false);
  protected readonly editing = signal<Stylist | null>(null);
  /** Horario semanal en edición; se convierte en franjas para la API al guardar. */
  protected readonly schedule = signal<WeeklySchedule>(defaultSchedule());
  protected readonly scheduleErrors = signal<string[]>([]);
  protected readonly workDays = computed(() => this.schedule().workDays);
  protected readonly worksWeekdays = computed(() => this.workDays().some((day) => !isWeekend(day)));
  protected readonly worksWeekend = computed(() => this.workDays().some(isWeekend));
  /** Días de trabajo en el orden de la semana, para el modo personalizado. */
  protected readonly orderedWorkDays = computed(() => WEEK.filter(({ day }) => this.workDays().includes(day)));
  protected readonly daysOff = computed(() =>
    WEEK.filter(({ day }) => !this.workDays().includes(day)).map(({ long }) => long),
  );
  /** Se recalcula con cada cambio, así el aviso siempre describe la pausa tal como está configurada. */
  protected readonly breakMessage = computed(() => breakSummary(this.schedule()));

  protected readonly timeOffFor = signal<Stylist | null>(null);
  protected readonly timeOffOpen = computed(() => this.timeOffFor() !== null);
  protected readonly timeOffError = signal<string | null>(null);
  protected readonly upcomingTimeOffs = computed(() =>
    (this.timeOffFor()?.timeOffs ?? []).filter((off) => isCurrentOrUpcoming(off, new Date())),
  );

  protected readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.pattern(PASSWORD_POLICY)]],
    active: [true],
    serviceIds: [[] as string[]],
  });

  protected readonly timeOffForm = this.fb.group({
    kind: ['days' as TimeOffInput['kind']],
    from: [''],
    to: [''],
    date: [''],
    start: ['09:00'],
    end: ['13:00'],
    reason: ['', Validators.maxLength(255)],
  });
  protected readonly timeOffKind = toSignal(this.timeOffForm.controls.kind.valueChanges, {
    initialValue: this.timeOffForm.controls.kind.value,
  });

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

  protected serviceNames(stylist: Stylist): string {
    const names = stylist.serviceIds.map((id) => this.services().find((service) => service.id === id)?.name);
    return names.filter(Boolean).join(', ') || 'Sin servicios';
  }

  protected scheduleSummary(stylist: Stylist): string {
    return describeSchedule(stylist.workingHours);
  }

  protected openCreate(): void {
    this.editing.set(null);
    this.form.controls.email.enable();
    this.form.controls.password.enable();
    this.form.reset({ fullName: '', email: '', password: '', active: true, serviceIds: [] });
    this.applySchedule(defaultSchedule());
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
    this.applySchedule(fromWorkingHours(stylist.workingHours));
    this.dialogOpen.set(true);
  }

  private applySchedule(schedule: WeeklySchedule): void {
    this.scheduleErrors.set([]);
    this.schedule.set(schedule);
  }

  /** Marca o desmarca un día de trabajo; los desmarcados son los días libres. */
  protected toggleDay(day: DayOfWeek): void {
    this.schedule.update((schedule) => ({
      ...schedule,
      workDays: schedule.workDays.includes(day)
        ? schedule.workDays.filter((current) => current !== day)
        : [...schedule.workDays, day],
    }));
  }

  /** Al pasar a personalizado cada día arranca con el horario fijo, para cambiar solo el que haga falta. */
  protected setMode(mode: ScheduleMode): void {
    this.schedule.update((schedule) =>
      mode === schedule.mode ? schedule : mode === 'custom' ? toCustom(schedule) : { ...schedule, mode },
    );
  }

  protected setFixedHours(group: 'weekdays' | 'weekend', hours: DayHours): void {
    this.schedule.update((schedule) => ({ ...schedule, [group]: hours }));
  }

  protected setDayHours(day: DayOfWeek, hours: DayHours): void {
    this.schedule.update((schedule) => ({ ...schedule, days: { ...schedule.days, [day]: hours } }));
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const schedule = this.schedule();
    const errors = scheduleErrors(schedule);
    this.scheduleErrors.set(errors);
    if (errors.length > 0) {
      return;
    }

    const value = this.form.getRawValue();
    const workingHours = toWorkingHours(schedule);
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
          workingHours,
        })
      : this.api.createStylist({
          branchId: branchId!,
          fullName: value.fullName,
          email: value.email,
          password: value.password,
          serviceIds: value.serviceIds,
          workingHours,
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

  // ---- Ausencias ----

  protected openTimeOff(stylist: Stylist): void {
    this.resetTimeOffForm();
    this.timeOffFor.set(stylist);
  }

  private resetTimeOffForm(): void {
    const today = localDateIn(new Date(), this.salon.zone());
    this.timeOffError.set(null);
    this.timeOffForm.reset({ kind: 'days', from: today, to: today, date: today, start: '09:00', end: '13:00', reason: '' });
  }

  /** Al mover el primer día por detrás del último, el último lo acompaña: la ausencia de un solo día es la más común. */
  protected syncLastDay(): void {
    const { from, to } = this.timeOffForm.getRawValue();
    if (from && (!to || to < from)) {
      this.timeOffForm.controls.to.setValue(from);
    }
  }

  protected describe(off: TimeOff): string {
    return describeTimeOff(off, this.salon.zone());
  }

  protected saveTimeOff(): void {
    const stylist = this.timeOffFor();
    if (!stylist) {
      return;
    }
    const value = this.timeOffForm.getRawValue();
    const input: TimeOffInput =
      value.kind === 'days'
        ? { kind: 'days', from: value.from, to: value.to }
        : { kind: 'hours', date: value.date, start: value.start, end: value.end };
    const problem = timeOffInputError(input);
    this.timeOffError.set(problem);
    if (problem || this.timeOffForm.invalid) {
      return;
    }

    this.saving.set(true);
    this.api.addTimeOff(stylist.id, { ...toTimeOffPeriod(input, this.salon.zone()), reason: value.reason }).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.timeOffFor.set(updated);
        this.resetTimeOffForm();
        this.notifier.success('Ausencia registrada.');
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.notifier.error(error);
      },
    });
  }

  protected removeTimeOff(off: TimeOff): void {
    const stylist = this.timeOffFor();
    if (!stylist) {
      return;
    }
    this.confirmation.confirm({
      header: 'Eliminar ausencia',
      message: `${this.describe(off)}. ${stylist.fullName} volverá a recibir reservas en ese periodo.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Volver',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.api.removeTimeOff(stylist.id, off.start).subscribe({
          next: (updated) => {
            this.timeOffFor.set(updated);
            this.notifier.success('Ausencia eliminada.');
            this.load();
          },
          error: (error: unknown) => this.notifier.error(error),
        }),
    });
  }
}
