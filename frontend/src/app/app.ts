import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';

import { AuthService } from './core/auth/auth.service';
import { SalonContext } from './core/time/salon-context.service';

interface NavLink {
  label: string;
  path: string;
}

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ButtonModule, ToastModule, ConfirmDialogModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);
  protected readonly salon = inject(SalonContext);

  protected readonly links = computed<NavLink[]>(() => {
    switch (this.auth.role()) {
      case 'ADMIN':
        return [
          { label: 'Agenda', path: '/admin/agenda' },
          { label: 'Servicios', path: '/admin/servicios' },
          { label: 'Estilistas', path: '/admin/estilistas' },
        ];
      case 'STYLIST':
        return [{ label: 'Mi agenda', path: '/agenda' }];
      case 'CUSTOMER':
        return [
          { label: 'Reservar', path: '/reservar' },
          { label: 'Mis citas', path: '/mis-citas' },
        ];
      default:
        return [{ label: 'Reservar', path: '/reservar' }];
    }
  });

  protected logout(): void {
    this.auth.logout().subscribe(() => void this.router.navigateByUrl('/'));
  }
}
