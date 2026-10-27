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
  Subject,
  switchMap
} from 'rxjs';

import { PropertyApiService } from '../../../property-selection/data-access/property-api.service';
import {
  AssignedProperty,
  PropertyRole
} from '../../../property-selection/models/assigned-property';
import { PropertyMemberApiService } from '../../data-access/property-member-api.service';
import { UserManagementEditor } from '../../components/user-management-editor/user-management-editor';
import {
  MembershipStatus,
  PropertyMember,
  PropertyMemberUnit,
  ResidentialRelationshipType,
  UserStatus
} from '../../models/property-member';
import {
  USER_STATUS_LABELS,
  userStatusLabel
} from '../../models/property-member-labels';

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

const MEMBERSHIP_STATUS_LABELS: Record<MembershipStatus, string> = {
  ACTIVE: 'Activa',
  INACTIVE: 'Inactiva',
  SUSPENDED: 'Suspendida'
};

const RELATIONSHIP_LABELS: Record<
  ResidentialRelationshipType,
  string
> = {
  OWNER: 'Propietario',
  RESIDENT: 'Residente'
};

@Component({
  selector: 'app-user-management-page',
  imports: [NgOptimizedImage, RouterLink, UserManagementEditor],
  templateUrl: './user-management-page.html',
  styleUrl: './user-management-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserManagementPage {

  private readonly route = inject(ActivatedRoute);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly memberApi = inject(PropertyMemberApiService);
  private readonly refreshMembers = new Subject<void>();

  protected readonly selectedMembershipId = signal<string | null>(null);

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

  protected readonly structureOptions = computed(() => {
    const currentState = this.state();

    if (currentState.status !== 'success') {
      return [];
    }

    return Array.from(new Set(
      currentState.members.flatMap(member =>
        member.units
          .map(unit => unit.structureName)
          .filter((name): name is string => name !== null)
      )
    )).sort((first, second) =>
      first.localeCompare(second, 'es')
    );
  });

  protected readonly relationshipOptions = computed(() => {
    const currentState = this.state();

    if (currentState.status !== 'success') {
      return [];
    }

    const types = new Set(
      currentState.members.flatMap(member =>
        member.units.map(unit => unit.relationshipType)
      )
    );

    return Array.from(types).map(value => ({
      value,
      label: RELATIONSHIP_LABELS[value]
    }));
  });

  protected readonly searchTerm = signal('');
  protected readonly roleFilter = signal<PropertyRole | 'ALL'>('ALL');
  protected readonly userStatusFilter = signal<UserStatus | 'ALL'>(
    'ALL'
  );
  protected readonly membershipStatusFilter = signal<
    MembershipStatus | 'ALL'
  >('ALL');
  protected readonly structureFilter = signal<string | 'ALL'>('ALL');
  protected readonly relationshipFilter = signal<
    ResidentialRelationshipType | 'ALL'
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
    const structure = this.structureFilter();
    const relationship = this.relationshipFilter();

    return currentState.members.filter(member => {
      const residentialText = member.units
        .map(unit =>
          `${unit.structureName ?? ''} ${unit.unitNumber}`
        )
        .join(' ');
      const searchableText = this.normalize(
        `${member.firstNames} ${member.lastNames} `
        + `${member.email} ${residentialText}`
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
        )
        && (
          structure === 'ALL'
          || member.units.some(
            unit => unit.structureName === structure
          )
        )
        && (
          relationship === 'ALL'
          || member.units.some(
            unit => unit.relationshipType === relationship
          )
        );
    });
  });

  protected readonly hasActiveFilters = computed(() =>
    this.searchTerm().trim() !== ''
    || this.roleFilter() !== 'ALL'
    || this.userStatusFilter() !== 'ALL'
    || this.membershipStatusFilter() !== 'ALL'
    || this.structureFilter() !== 'ALL'
    || this.relationshipFilter() !== 'ALL'
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

  protected updateStructureFilter(event: Event): void {
    const value = this.selectValue(event);

    if (
      value === 'ALL'
      || this.structureOptions().includes(value)
    ) {
      this.structureFilter.set(value);
    }
  }

  protected updateRelationshipFilter(event: Event): void {
    const value = this.selectValue(event);

    if (
      value === 'ALL'
      || this.relationshipOptions().some(
        option => option.value === value
      )
    ) {
      this.relationshipFilter.set(
        value as ResidentialRelationshipType | 'ALL'
      );
    }
  }

  protected clearFilters(): void {
    this.searchTerm.set('');
    this.roleFilter.set('ALL');
    this.userStatusFilter.set('ALL');
    this.membershipStatusFilter.set('ALL');
    this.structureFilter.set('ALL');
    this.relationshipFilter.set('ALL');
  }

  protected editMember(member: PropertyMember): void {
    this.selectedMembershipId.set(member.membershipId);
  }

  protected closeEditor(): void {
    this.selectedMembershipId.set(null);
  }

  protected handleMemberSaved(): void {
    this.refreshMembers.next();
  }

  protected roleLabel(role: PropertyRole): string {
    return ROLE_LABELS[role];
  }

  protected userStatusLabel(status: UserStatus): string {
    return userStatusLabel(status);
  }

  protected membershipStatusLabel(
    status: MembershipStatus
  ): string {
    return MEMBERSHIP_STATUS_LABELS[status];
  }

  protected unitLabel(unit: PropertyMemberUnit): string {
    const structure = unit.structureName
      ? `${unit.structureName} · `
      : '';

    return `${structure}${this.unitTypeLabel(unit)} `
      + `· ${RELATIONSHIP_LABELS[unit.relationshipType]}`;
  }

  protected noUnitsLabel(role: PropertyRole): string {
    return role === 'OWNER' || role === 'RESIDENT'
      ? 'Sin unidad asignada'
      : 'No aplica';
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

            return this.refreshMembers.pipe(
              startWith(undefined),
              switchMap(() => this.memberApi
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
                )
              )
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

  private unitTypeLabel(unit: PropertyMemberUnit): string {
    return switchUnitType(unit);
  }

  private isForbidden(error: unknown): boolean {
    return error instanceof HttpErrorResponse
      && error.status === 403;
  }
}

function switchUnitType(unit: PropertyMemberUnit): string {
  switch (unit.unitType) {
    case 'APARTMENT':
      return `Apto. ${unit.unitNumber}`;
    case 'HOUSE':
      return `Casa ${unit.unitNumber}`;
    case 'COMMERCIAL_SPACE':
      return `Local ${unit.unitNumber}`;
    case 'OFFICE':
      return `Oficina ${unit.unitNumber}`;
    case 'OTHER':
      return `Unidad ${unit.unitNumber}`;
  }
}
