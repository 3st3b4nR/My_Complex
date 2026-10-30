import { DatePipe, NgOptimizedImage } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
import { UserRegistration } from '../../components/user-registration/user-registration';
import {
  MembershipStatus,
  PropertyMember,
  PropertyMemberUnit,
  ResidentialRelationshipType,
  UserStatus,
  UserRegistrationStatus
} from '../../models/property-member';
import {
  USER_STATUS_LABELS,
  userStatusLabel,
  userRegistrationStatusLabel
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
  imports: [DatePipe, NgOptimizedImage, RouterLink, UserManagementEditor, UserRegistration],
  templateUrl: './user-management-page.html',
  styleUrl: './user-management-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserManagementPage {

  private readonly route = inject(ActivatedRoute);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly memberApi = inject(PropertyMemberApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly refreshMembers = new Subject<void>();

  protected readonly selectedMembershipId = signal<string | null>(null);
  protected readonly registrationOpen = signal(false);
  protected readonly unlinkingMembershipId = signal<string | null>(null);
  protected readonly invitingMembershipId = signal<string | null>(null);
  protected readonly notice = signal('');
  protected readonly actionError = signal('');

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

  protected handleRegistrationSaved(message: string): void {
    this.registrationOpen.set(false);
    this.notice.set(message);
    this.refreshMembers.next();
  }

  protected registrationStatusLabel = userRegistrationStatusLabel;

  protected invitationActionLabel(
    status: UserRegistrationStatus
  ): string | null {
    switch (status) {
      case 'PENDING':
        return 'Enviar invitación';
      case 'PROVISIONING':
        return 'Aprovisionando…';
      case 'PROVISIONED':
        return 'Reintentar envío';
      case 'INVITED':
        return 'Reenviar invitación';
      case 'LINKED':
        return null;
    }
  }

  protected sendInvitation(member: PropertyMember): void {
    const property = this.state().property;
    const label = this.invitationActionLabel(member.registrationStatus);
    if (!property || !label || member.registrationStatus === 'PROVISIONING'
      || this.invitingMembershipId() !== null) {
      return;
    }
    const resend = member.registrationStatus === 'INVITED';
    const confirmation = resend
      ? 'Se solicitará un nuevo correo de invitación. El usuario debe usar el correo más reciente. ¿Deseas continuar?'
      : 'Se creará o reconciliará la identidad y se solicitará un correo para que el usuario establezca su propia contraseña. ¿Deseas continuar?';
    if (!window.confirm(confirmation)) {
      return;
    }
    this.invitingMembershipId.set(member.membershipId);
    this.actionError.set('');
    const request = resend
      ? this.memberApi.resendPropertyMemberInvitation(
          property.propertyId, member.membershipId
        )
      : this.memberApi.invitePropertyMember(
          property.propertyId, member.membershipId
        );
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => {
        this.invitingMembershipId.set(null);
        this.notice.set(result.message);
        this.refreshMembers.next();
      },
      error: error => {
        this.invitingMembershipId.set(null);
        this.actionError.set(this.invitationErrorMessage(error));
      }
    });
  }

  protected unlinkMember(member: PropertyMember): void {
    const property = this.state().property;
    if (!property || !window.confirm(
      `${member.firstNames} ${member.lastNames} perderá su vinculación con ${property.name}. `
      + 'Sus asignaciones residenciales vigentes serán finalizadas y su cuenta global no será eliminada. ¿Deseas continuar?'
    )) return;
    this.unlinkingMembershipId.set(member.membershipId);
    this.actionError.set('');
    this.memberApi.unlinkPropertyMember(
      property.propertyId,
      member.membershipId,
      member.version
    ).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.unlinkingMembershipId.set(null);
        this.selectedMembershipId.set(null);
        this.notice.set('Usuario desvinculado correctamente.');
        this.refreshMembers.next();
      },
      error: error => {
        this.unlinkingMembershipId.set(null);
        this.actionError.set(this.unlinkErrorMessage(error));
      }
    });
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

  private unlinkErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) return 'No pudimos desvincular al usuario.';
    if (error.status === 403) return 'No tienes permiso para desvincular usuarios.';
    if (error.status === 404) return 'La vinculación ya no está disponible.';
    if (error.status === 409) return 'La vinculación fue modificada. Actualiza el listado e inténtalo nuevamente.';
    if (error.status === 400 && error.error?.code === 'LAST_PROPERTY_ADMINISTRATOR') return 'No puedes desvincular al último administrador activo.';
    if (error.status === 400) return error.error?.message ?? 'La desvinculación no es válida.';
    return 'No pudimos desvincular al usuario.';
  }

  private invitationErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'No pudimos procesar la invitación.';
    }
    const code = error.error?.code as string | undefined;
    if (error.status === 403) {
      return 'No tienes permiso para invitar usuarios.';
    }
    if (error.status === 404) {
      return 'La vinculación ya no está disponible.';
    }
    if (code === 'AUTH0_NOT_CONFIGURED'
      || code === 'AUTH0_CONFIGURATION_ERROR') {
      return 'La integración con Auth0 aún no está configurada correctamente.';
    }
    if (code === 'AUTH0_IDENTITY_CONFLICT') {
      return 'El correo ya pertenece a una identidad que requiere revisión manual.';
    }
    if (code === 'AUTH0_RATE_LIMITED') {
      return 'Auth0 limitó temporalmente las solicitudes. Inténtalo más tarde.';
    }
    if (code === 'AUTH0_UNAVAILABLE') {
      return 'Auth0 no está disponible temporalmente.';
    }
    if (code === 'AUTH0_INVITATION_UNAVAILABLE') {
      return 'La identidad fue creada, pero no fue posible solicitar el correo. Puedes reintentarlo.';
    }
    if (code === 'INVITATION_RESEND_TOO_SOON') {
      return 'Espera antes de reenviar la invitación.';
    }
    return error.error?.message ?? 'No pudimos procesar la invitación.';
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
