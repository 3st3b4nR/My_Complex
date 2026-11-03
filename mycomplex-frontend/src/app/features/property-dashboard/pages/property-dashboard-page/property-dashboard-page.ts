import {
  AsyncPipe
} from '@angular/common';

import {
  ChangeDetectionStrategy,
  Component,
  inject
} from '@angular/core';

import {
  ActivatedRoute,
  RouterLink
} from '@angular/router';

import {
  catchError,
  combineLatest,
  map,
  Observable,
  of,
  startWith
} from 'rxjs';

import { PropertyApiService } from '../../../property-selection/data-access/property-api.service';
import { PropertyShell } from '../../../../shared/layout/property-shell/property-shell';

import {
  AssignedProperty,
  PropertyRole
} from '../../../property-selection/models/assigned-property';

type PropertyDashboardState =
  | {
      status: 'loading';
      property: null;
    }
  | {
      status: 'success';
      property: AssignedProperty;
    }
  | {
      status: 'not-found';
      property: null;
    }
  | {
      status: 'error';
      property: null;
    };

@Component({
  selector: 'app-property-dashboard-page',
  imports: [
    AsyncPipe,
    PropertyShell,
    RouterLink
  ],
  templateUrl: './property-dashboard-page.html',
  styleUrl: './property-dashboard-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PropertyDashboardPage {

  private readonly route = inject(ActivatedRoute);
  private readonly propertyApi = inject(PropertyApiService);

  protected readonly state$: Observable<PropertyDashboardState> =
    combineLatest([
      this.route.paramMap,
      this.propertyApi.getMyProperties()
    ]).pipe(
      map(([parameters, properties]): PropertyDashboardState => {
        const propertyId = parameters.get('propertyId');

        const property = properties.find(
          currentProperty =>
            currentProperty.propertyId === propertyId
        );

        if (!property) {
          return {
            status: 'not-found',
            property: null
          };
        }

        return {
          status: 'success',
          property
        };
      }),
      startWith({
        status: 'loading',
        property: null
      } as PropertyDashboardState),
      catchError(() => of<PropertyDashboardState>({
        status: 'error',
        property: null
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

}
