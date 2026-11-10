import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';

import { AuthService, homeFor } from '../../../core/auth/auth.service';
import { problemMessage } from '../../../core/errors/problem';
import { safeReturnUrl } from '../safe-return-url';

/** HU-06. */
@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputTextModule, MessageModule, PasswordModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page page--narrow">
      <header class="page__header"><h1>Inicia sesión</h1></header>

      <form class="panel form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        @if (error()) {
          <p-message severity="error">{{ error()! }}</p-message>
        }
        <div class="field">
          <label for="email">Email</label>
          <input pInputText id="email" type="email" autocomplete="email" formControlName="email" />
          @if (form.controls.email.touched && form.controls.email.invalid) {
            <small class="field__error">Escribe un email válido.</small>
          }
        </div>
        <div class="field">
          <label for="password">Contraseña</label>
          <p-password inputId="password" formControlName="password" [feedback]="false" [toggleMask]="true"
                      autocomplete="current-password" [fluid]="true" />
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <small class="field__error">Escribe tu contraseña.</small>
          }
        </div>
        <p-button type="submit" label="Ingresar" [loading]="submitting()" [fluid]="true" />
      </form>

      <p class="form__alt">
        ¿Aún no tienes cuenta?
        <a routerLink="/registro" [queryParams]="returnUrl() ? { returnUrl: returnUrl() } : {}">Crea una</a>
      </p>
    </div>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly returnUrl = input<string>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
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
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: (session) => void this.router.navigateByUrl(safeReturnUrl(this.returnUrl()) ?? homeFor(session.role)),
      error: (error: unknown) => {
        this.error.set(problemMessage(error));
        this.submitting.set(false);
      },
    });
  }
}
