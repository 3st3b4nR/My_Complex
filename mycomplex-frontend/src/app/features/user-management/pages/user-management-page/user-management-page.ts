import { NgOptimizedImage } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  catchError,
  map,
  Observable,
  of,
  startWith,
  switchMap
} from 'rxjs';

import { PropertyApiService } from '../../../property-selection/data-access/property-api.service';
import {
  AssignedProperty,
  PropertyRole
} from '../../../property-selection/models/assigned-property';
import { PropertyMemberApiService } from '../../data-access/property-member-api.service';
import {
  MembershipStatus,
  PropertyMember,
  UserStatus
} from '../../models/property-member';

type UserManagementState =
  | {
      status: 'loading';
      property: AssignedProperty | null;
      members: PropertyMember[];
    }
  | {
      status: 'success';
      property: AssignedProperty;
      members: PropertyMember[];
    }
  | {
      status: 'not-found' | 'error';
      property: null;
      members: PropertyMember[];
    }
  | {
      status: 'unauthorized';
      property: AssignedProperty;
      members: PropertyMember[];
    };

const ROLE_LABELS: Record<PropertyRole, string> = {
  PROPERTY_ADMIN: 'Administrador de copropiedad',
  OWNER: 'Propietario',
  RESIDENT: 'Residente',
  SECURITY_GUARD: 'Vigilante',
  WORKER: 'Trabajador'
};

const USER_STATUS_LABELS: Record<UserStatus, string> = {
  ACTIVE: 'Activo',
  INACTIVE: 'Inactivo',
  BLOCKED: 'Bloqueado'
};

const MEMBERSHIP_STATUS_LABELS: Record<MembershipStatus, string> = {
  ACTIVE: 'Activa',
  INACTIVE: 'Inactiva',
  SUSPENDED: 'Suspendida'
};

@Component({
  selector: 'app-user-management-page',
  imports: [NgOptimizedImage, RouterLink],
  templateUrl: './user-management-page.html',
  styleUrl: './user-management-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserManagementPage {

  private readonly route = inject(ActivatedRoute);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly memberApi = inject(PropertyMemberApiService);

  protected readonly roleOptions = Object.entries(ROLE_LABELS).map(
    ([value, label]) => ({
      value: value as PropertyRole,
      label
    })
  );
  protected readonly userStatusOptions = Object.entries(
    USER_STATUS_LABELS
  ).map(([value, label]) => ({
    value: value as UserStatus,
    label
  }));
  protected readonly membershipStatusOptions = Object.entries(
    MEMBERSHIP_STATUS_LABELS
  ).map(([value, label]) => ({
    value: value as MembershipStatus,
    label
  }));

  protected readonly searchTerm = signal('');
  protected readonly roleFilter = signal<PropertyRole | 'ALL'>('ALL');
  protected readonly userStatusFilter = signal<UserStatus | 'ALL'>(
    'ALL'
  );
  protected readonly membershipStatusFilter = signal<
    MembershipStatus | 'ALL'
  >('ALL');

  protected readonly state = toSignal(this.loadState(), {
    initialValue: {
      status: 'loading',
      property: null,
      members: []
    } as UserManagementState
  });

  protected readonly filteredMembers = computed(() => {
    const currentState = this.state();

    if (currentState.status !== 'success') {
      return [];
    }

    const search = this.normalize(this.searchTerm());
    const role = this.roleFilter();
    const userStatus = this.userStatusFilter();
    const membershipStatus = this.membershipStatusFilter();

    return currentState.members.filter(member => {
      const searchableText = this.normalize(
        `${member.firstNames} ${member.lastNames} ${member.email}`
      );

      return (!search || searchableText.includes(search))
        && (role === 'ALL' || member.role === role)
        && (
          userStatus === 'ALL'
          || member.userStatus === userStatus
        )
        && (
          membershipStatus === 'ALL'
          || member.membershipStatus === membershipStatus
        );
    });
  });

  protected readonly hasActiveFilters = computed(() =>
    this.searchTerm().trim() !== ''
    || this.roleFilter() !== 'ALL'
    || this.userStatusFilter() !== 'ALL'
    || this.membershipStatusFilter() !== 'ALL'
  );

  protected updateSearch(event: Event): void {
    const target = event.target;

    if (target instanceof HTMLInputElement) {
      this.searchTerm.set(target.value);
    }
  }

  protected updateRoleFilter(event: Event): void {
    const value = this.selectValue(event);

    if (
      value === 'ALL'
      || this.roleOptions.some(option => option.value === value)
    ) {
      this.roleFilter.set(value as PropertyRole | 'ALL');
    }
  }

  protected updateUserStatusFilter(event: Event): void {
    const value = this.selectValue(event);

    if (
      value === 'ALL'
      || this.userStatusOptions.some(option => option.value === value)
    ) {
      this.userStatusFilter.set(value as UserStatus | 'ALL');
    }
  }

  protected updateMembershipStatusFilter(event: Event): void {
    const value = this.selectValue(event);

    if (
      value === 'ALL'
      || this.membershipStatusOptions.some(
        option => option.value === value
      )
    ) {
      this.membershipStatusFilter.set(
        value as MembershipStatus | 'ALL'
      );
    }
  }

  protected clearFilters(): void {
    this.searchTerm.set('');
    this.roleFilter.set('ALL');
    this.userStatusFilter.set('ALL');
    this.membershipStatusFilter.set('ALL');
  }

  protected roleLabel(role: PropertyRole): string {
    return ROLE_LABELS[role];
  }

  protected userStatusLabel(status: UserStatus): string {
    return USER_STATUS_LABELS[status];
  }

  protected membershipStatusLabel(
    status: MembershipStatus
  ): string {
    return MEMBERSHIP_STATUS_LABELS[status];
  }

  private loadState(): Observable<UserManagementState> {
    return this.route.paramMap.pipe(
      switchMap(parameters => {
        const propertyId = parameters.get('propertyId');

        if (!propertyId) {
          return of<UserManagementState>({
            status: 'not-found',
            property: null,
            members: []
          });
        }

        return this.propertyApi.getMyProperties().pipe(
          switchMap(properties => {
            const property = properties.find(
              currentProperty =>
                currentProperty.propertyId === propertyId
            );

            if (!property) {
              return of<UserManagementState>({
                status: 'not-found',
                property: null,
                members: []
              });
            }

            if (property.role !== 'PROPERTY_ADMIN') {
              return of<UserManagementState>({
                status: 'unauthorized',
                property,
                members: []
              });
            }

            return this.memberApi
              .getPropertyMembers(propertyId)
              .pipe(
                map(members => ({
                  status: 'success' as const,
                  property,
                  members
                })),
                startWith({
                  status: 'loading' as const,
                  property,
                  members: []
                }),
                catchError(error => of<UserManagementState>({
                  status: this.isForbidden(error)
                    ? 'unauthorized'
                    : 'error',
                  property: this.isForbidden(error)
                    ? property
                    : null,
                  members: []
                } as UserManagementState))
              );
          }),
          catchError(() => of<UserManagementState>({
            status: 'error',
            property: null,
            members: []
          }))
        );
      })
    );
  }

  private selectValue(event: Event): string {
    const target = event.target;
    return target instanceof HTMLSelectElement
      ? target.value
      : 'ALL';
  }

  private normalize(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLocaleLowerCase('es');
  }

  private isForbidden(error: unknown): boolean {
    return error instanceof HttpErrorResponse
      && error.status === 403;
  }
}
