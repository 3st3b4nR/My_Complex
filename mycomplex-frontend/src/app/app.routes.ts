import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import(
        './features/authentication/pages/login-page/login-page'
      ).then((component) => component.LoginPage),
    title: 'Acceso | MyComplex',
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];