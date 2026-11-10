import { Routes } from '@angular/router';

import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/home/pages/home.page').then((m) => m.HomePage),
    title: 'Ozin · Reserva tu cita',
  },
  {
    path: 'reservar',
    loadComponent: () => import('./features/booking/pages/booking.page').then((m) => m.BookingPage),
    title: 'Reservar cita · Ozin',
  },
  {
    path: 'ingresar',
    loadComponent: () => import('./features/auth/pages/login.page').then((m) => m.LoginPage),
    title: 'Iniciar sesión · Ozin',
  },
  {
    path: 'registro',
    loadComponent: () => import('./features/auth/pages/register.page').then((m) => m.RegisterPage),
    title: 'Crear cuenta · Ozin',
  },
  {
    path: 'mis-citas',
    canActivate: [roleGuard('CUSTOMER')],
    loadComponent: () =>
      import('./features/customer/pages/my-appointments.page').then((m) => m.MyAppointmentsPage),
    title: 'Mis citas · Ozin',
  },
  {
    path: 'agenda',
    canActivate: [roleGuard('STYLIST')],
    loadComponent: () =>
      import('./features/stylist/pages/stylist-agenda.page').then((m) => m.StylistAgendaPage),
    title: 'Mi agenda · Ozin',
  },
  {
    path: 'admin',
    canActivate: [roleGuard('ADMIN')],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'agenda' },
      {
        path: 'agenda',
        loadComponent: () =>
          import('./features/admin/pages/admin-agenda.page').then((m) => m.AdminAgendaPage),
        title: 'Agenda del salón · Ozin',
      },
      {
        path: 'servicios',
        loadComponent: () =>
          import('./features/admin/pages/admin-services.page').then((m) => m.AdminServicesPage),
        title: 'Servicios · Ozin',
      },
      {
        path: 'estilistas',
        loadComponent: () =>
          import('./features/admin/pages/admin-stylists.page').then((m) => m.AdminStylistsPage),
        title: 'Estilistas · Ozin',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
