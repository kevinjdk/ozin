import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/pages/home.page').then((m) => m.HomePage),
    title: 'Ozin · Reserva tu cita',
  },
  { path: '**', redirectTo: '' },
];
