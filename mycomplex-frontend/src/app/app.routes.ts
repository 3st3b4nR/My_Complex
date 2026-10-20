import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'acceso',
    loadComponent: () =>
      import('./features/acceso/pages/login-page/login-page').then(
        (component) => component.LoginPage,
      ),
    title: 'Acceso | MyComplex',
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'acceso',
  },
  {
    path: '**',
    redirectTo: 'acceso',
  },
];