import { Routes } from '@angular/router';
import { authGuardFn } from '@auth0/auth0-angular';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import(
        './features/authentication/pages/login-page/login-page'
      ).then(component => component.LoginPage),
    title: 'Acceso | MyComplex'
  },
  {
    path: 'select-property',
    loadComponent: () =>
      import(
        './features/property-selection/pages/property-selection-page/property-selection-page'
      ).then(component => component.PropertySelectionPage),
    canActivate: [authGuardFn],
    title: 'Seleccionar copropiedad | MyComplex'
  },
  {
    path: 'properties/:propertyId/dashboard',
    loadComponent: () =>
      import(
        './features/property-dashboard/pages/property-dashboard-page/property-dashboard-page'
      ).then(component => component.PropertyDashboardPage),
    canActivate: [authGuardFn],
    title: 'Panel de administración | MyComplex'
  },
  {
    path: 'properties/:propertyId/users',
    loadComponent: () =>
      import(
        './features/user-management/pages/user-management-page/user-management-page'
      ).then(component => component.UserManagementPage),
    canActivate: [authGuardFn],
    title: 'Gestión de usuarios | MyComplex'
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login'
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
