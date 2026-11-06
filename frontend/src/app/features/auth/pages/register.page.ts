import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { switchMap } from 'rxjs';

import { AuthService, homeFor } from '../../../core/auth/auth.service';
import { problemMessage } from '../../../core/errors/problem';
import { safeReturnUrl } from '../safe-return-url';

/** HU-06: misma política de contraseña que el backend; aquí solo adelanta el aviso. */
const PASSWORD_POLICY = /^(?=.*[A-ZÁÉÍÓÚÑ])(?=.*\d).{8,}$/;
const PHONE_E164 = /^\+[1-9]\d{7,14}$/;

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputTextModule, MessageModule, PasswordModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page page--narrow">
      <header class="page__header">
        <div>
          <h1>Crea tu cuenta</h1>
          <p>La necesitas para reservar y gestionar tus citas.</p>
        </div>
      </header>

      <form class="panel form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        @if (error()) {
          <p-message severity="error">{{ error()! }}</p-message>
        }
        <div class="field">
          <label for="fullName">Nombre completo</label>
          <input pInputText id="fullName" autocomplete="name" formControlName="fullName" />
          @if (form.controls.fullName.touched && form.controls.fullName.invalid) {
            <small class="field__error">Escribe tu nombre.</small>
          }
        </div>
        <div class="field">
          <label for="email">Email</label>
          <input pInputText id="email" type="email" autocomplete="email" formControlName="email" />
          @if (form.controls.email.touched && form.controls.email.invalid) {
            <small class="field__error">Escribe un email válido.</small>
          }
        </div>
        <div class="field">
          <label for="phone">Teléfono</label>
          <input pInputText id="phone" type="tel" autocomplete="tel" formControlName="phone" />
          @if (form.controls.phone.touched && form.controls.phone.invalid) {
            <small class="field__error">Usa el formato internacional, por ejemplo +593991234567.</small>
          }
        </div>
        <div class="field">
          <label for="password">Contraseña</label>
          <p-password inputId="password" formControlName="password" [feedback]="false" [toggleMask]="true"
                      autocomplete="new-password" [fluid]="true" />
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <small class="field__error">Debe tener al menos 8 caracteres, una mayúscula y un número.</small>
          } @else {
            <small class="field__hint">Al menos 8 caracteres, una mayúscula y un número.</small>
          }
        </div>
        <p-button type="submit" label="Crear cuenta" [loading]="submitting()" [fluid]="true" />
      </form>

      <p class="form__alt">
        ¿Ya tienes cuenta?
        <a routerLink="/ingresar" [queryParams]="returnUrl() ? { returnUrl: returnUrl() } : {}">Inicia sesión</a>
      </p>
    </div>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly returnUrl = input<string>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: ['+593', [Validators.required, Validators.pattern(PHONE_E164)]],
    password: ['', [Validators.required, Validators.pattern(PASSWORD_POLICY)]],
  });
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const input = this.form.getRawValue();
    this.auth
      .register(input)
      .pipe(switchMap(() => this.auth.login(input.email, input.password)))
      .subscribe({
        next: (session) => void this.router.navigateByUrl(safeReturnUrl(this.returnUrl()) ?? homeFor(session.role)),
        error: (error: unknown) => {
          this.error.set(problemMessage(error));
          this.submitting.set(false);
        },
      });
  }
}
