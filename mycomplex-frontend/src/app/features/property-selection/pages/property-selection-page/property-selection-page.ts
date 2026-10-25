import {
  AsyncPipe,
  NgOptimizedImage
} from '@angular/common';

import {
  ChangeDetectionStrategy,
  Component,
  inject
} from '@angular/core';

import { Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';

import {
  catchError,
  map,
  of,
  startWith
} from 'rxjs';

import { PropertyApiService } from '../../data-access/property-api.service';

import {
  AssignedProperty,
  PropertyRole
} from '../../models/assigned-property';

type PropertySelectionState =
  | {
      status: 'loading';
      properties: AssignedProperty[];
    }
  | {
      status: 'success';
      properties: AssignedProperty[];
    }
  | {
      status: 'error';
      properties: AssignedProperty[];
    };

@Component({
  selector: 'app-property-selection-page',
  imports: [
    AsyncPipe,
    NgOptimizedImage
  ],
  templateUrl: './property-selection-page.html',
  styleUrl: './property-selection-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PropertySelectionPage {

  private readonly authService = inject(AuthService);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly router = inject(Router);

  protected readonly user$ = this.authService.user$;

  protected readonly state$ = this.propertyApi
    .getMyProperties()
    .pipe(
      map(properties => ({
        status: 'success' as const,
        properties
      })),
      startWith({
        status: 'loading' as const,
        properties: []
      }),
      catchError(() => of({
        status: 'error' as const,
        properties: []
      }))
    );

  protected roleLabel(role: PropertyRole): string {
    const labels: Record<PropertyRole, string> = {
      PROPERTY_ADMIN: 'Administrador de copropiedad',
      OWNER: 'Propietario',
      RESIDENT: 'Residente',
      SECURITY_GUARD: 'Vigilante',
      WORKER: 'Trabajador'
    };

    return labels[role];
  }

  protected selectProperty(
    property: AssignedProperty
  ): void {
    this.router.navigate([
      '/properties',
      property.propertyId,
      'dashboard'
    ]);
  }

  protected logout(): void {
    this.authService.logout({
      logoutParams: {
        returnTo: window.location.origin
      }
    }).subscribe();
  }
}