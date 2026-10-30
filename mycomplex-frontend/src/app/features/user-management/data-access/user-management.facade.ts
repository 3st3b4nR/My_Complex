import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { catchError, map, of, startWith, Subject, switchMap } from 'rxjs';

import { PropertyApiService } from '../../property-selection/data-access/property-api.service';
import { AssignedProperty } from '../../property-selection/models/assigned-property';
import { PropertyMember } from '../models/property-member';
import { PropertyMemberApiService } from './property-member-api.service';

export type UserManagementState =
  | { status: 'loading'; property: AssignedProperty | null; members: PropertyMember[] }
  | { status: 'success'; property: AssignedProperty; members: PropertyMember[] }
  | { status: 'not-found' | 'error'; property: null; members: PropertyMember[] }
  | { status: 'unauthorized'; property: AssignedProperty; members: PropertyMember[] };

@Injectable()
export class UserManagementFacade {
  private readonly route = inject(ActivatedRoute);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly memberApi = inject(PropertyMemberApiService);
  private readonly refreshMembers = new Subject<void>();

  readonly state = toSignal(this.route.paramMap.pipe(
    switchMap(parameters => {
      const propertyId = parameters.get('propertyId');
      if (!propertyId) return of<UserManagementState>(this.notFound());

      return this.propertyApi.getMyProperties().pipe(
        switchMap(properties => {
          const property = properties.find(item => item.propertyId === propertyId);
          if (!property) return of<UserManagementState>(this.notFound());
          if (property.role !== 'PROPERTY_ADMIN') {
            return of<UserManagementState>({ status: 'unauthorized', property, members: [] });
          }
          return this.refreshMembers.pipe(
            startWith(undefined),
            switchMap(() => this.memberApi.getPropertyMembers(propertyId).pipe(
              map(members => ({ status: 'success' as const, property, members })),
              startWith({ status: 'loading' as const, property, members: [] }),
              catchError(error => of<UserManagementState>(
                error instanceof HttpErrorResponse && error.status === 403
                  ? { status: 'unauthorized', property, members: [] }
                  : { status: 'error', property: null, members: [] }
              ))
            ))
          );
        }),
        catchError(() => of<UserManagementState>({
          status: 'error', property: null, members: []
        }))
      );
    })
  ), {
    initialValue: {
      status: 'loading', property: null, members: []
    } as UserManagementState
  });

  reload(): void {
    this.refreshMembers.next();
  }

  private notFound(): UserManagementState {
    return { status: 'not-found', property: null, members: [] };
  }
}
