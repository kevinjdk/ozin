import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { SalonService } from '../../../core/api/models';
import { SalonApi } from '../../../core/api/salon.api';
import { Notifier } from '../../../core/errors/notifier.service';
import { problemMessage } from '../../../core/errors/problem';
import { formatMoney } from '../../../core/time/salon-time';

/** HU-10. Los servicios se desactivan en lugar de borrarse, para conservar el histórico. */
@Component({
  selector: 'app-admin-services-page',
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputNumberModule,
    InputTextModule,
    SkeletonModule,
    TableModule,
    TagModule,
    ToggleSwitchModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <header class="page__header">
        <div>
          <h1>Servicios</h1>
          <p>Cambiar un precio no afecta a las citas ya reservadas.</p>
        </div>
        <p-button label="Nuevo servicio" icon="pi pi-plus" (onClick)="openCreate()" />
      </header>

      @if (services(); as list) {
        <div class="panel table-stack">
          <p-table [value]="list" dataKey="id">
            <ng-template #header>
              <tr>
                <th>Nombre</th>
                <th>Duración</th>
                <th>Precio</th>
                <th>Estado</th>
                <th><span class="sr-only">Acciones</span></th>
              </tr>
            </ng-template>
            <ng-template #body let-service>
              <tr>
                <td>{{ service.name }}</td>
                <td>{{ service.durationMinutes }} min</td>
                <td>{{ money(service) }}</td>
                <td>
                  <p-tag [value]="service.active ? 'Activo' : 'Inactivo'" [severity]="service.active ? 'success' : 'secondary'" />
                </td>
                <td class="table-actions">
                  <p-button label="Editar" size="small" severity="secondary" [outlined]="true"
                            [ariaLabel]="'Editar ' + service.name" (onClick)="openEdit(service)" />
                </td>
              </tr>
            </ng-template>
            <ng-template #emptymessage>
              <tr><td colspan="5" class="muted">Crea el primer servicio del catálogo.</td></tr>
            </ng-template>
          </p-table>
        </div>
      } @else if (error()) {
        <div class="panel empty-state" role="alert">
          <p>{{ error() }}</p>
          <p-button label="Reintentar" severity="secondary" (onClick)="load()" />
        </div>
      } @else {
        <p-skeleton height="12rem" />
      }
    </div>

    <p-dialog [visible]="dialogOpen()" (visibleChange)="dialogOpen.set($event)" [modal]="true"
              [header]="editingId() ? 'Editar servicio' : 'Nuevo servicio'" [style]="{ width: 'min(26rem, 94vw)' }">
      <form class="form" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <div class="field">
          <label for="service-name">Nombre</label>
          <input pInputText id="service-name" formControlName="name" />
          @if (form.controls.name.touched && form.controls.name.invalid) {
            <small class="field__error">Escribe un nombre.</small>
          }
        </div>
        <div class="field">
          <label for="service-duration">Duración (minutos)</label>
          <p-inputnumber inputId="service-duration" formControlName="durationMinutes" [min]="1" [max]="480" [fluid]="true" />
          @if (form.controls.durationMinutes.touched && form.controls.durationMinutes.invalid) {
            <small class="field__error">Debe estar entre 1 y 480 minutos.</small>
          }
        </div>
        <div class="field">
          <label for="service-price">Precio (USD)</label>
          <p-inputnumber inputId="service-price" formControlName="price" mode="currency" currency="USD" locale="es-EC"
                         [min]="0" [fluid]="true" />
          @if (form.controls.price.touched && form.controls.price.invalid) {
            <small class="field__error">Escribe un precio.</small>
          }
        </div>
        @if (editingId()) {
          <div class="field">
            <label for="service-active">Disponible para reservar</label>
            <p-toggleswitch inputId="service-active" formControlName="active" />
          </div>
        }
        <div class="form__actions">
          <p-button label="Cancelar" severity="secondary" [text]="true" (onClick)="dialogOpen.set(false)" />
          <p-button type="submit" label="Guardar" [loading]="saving()" />
        </div>
      </form>
    </p-dialog>
  `,
})
export class AdminServicesPage implements OnInit {
  private readonly api = inject(SalonApi);
  private readonly notifier = inject(Notifier);

  protected readonly services = signal<SalonService[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly dialogOpen = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly saving = signal(false);

  protected readonly form = inject(FormBuilder).group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    durationMinutes: [30 as number | null, [Validators.required, Validators.min(1), Validators.max(480)]],
    price: [null as number | null, [Validators.required, Validators.min(0)]],
    active: [true],
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.error.set(null);
    this.api.allServices().subscribe({
      next: (services) => this.services.set(services),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  protected money(service: SalonService): string {
    return formatMoney(service.price, service.currency);
  }

  protected openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', durationMinutes: 30, price: null, active: true });
    this.dialogOpen.set(true);
  }

  protected openEdit(service: SalonService): void {
    this.editingId.set(service.id);
    this.form.reset({
      name: service.name,
      durationMinutes: service.durationMinutes,
      price: service.price,
      active: service.active,
    });
    this.dialogOpen.set(true);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const input = { name: value.name!, durationMinutes: value.durationMinutes!, price: value.price! };
    const id = this.editingId();
    const request = id
      ? this.api.updateService(id, { ...input, active: value.active! })
      : this.api.createService(input);

    this.saving.set(true);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogOpen.set(false);
        this.notifier.success('Servicio guardado.');
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.notifier.error(error);
      },
    });
  }
}
