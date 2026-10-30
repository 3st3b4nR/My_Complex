import { DatePipe, NgOptimizedImage } from '@angular/common';
import {
  Component,
  computed,
  DestroyRef,
  inject,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import {
  PropertyRole
} from '../../../property-selection/models/assigned-property';
import { PropertyMemberApiService } from '../../data-access/property-member-api.service';
import { UserManagementFacade } from '../../data-access/user-management.facade';
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
  PROPERTY_ROLE_LABELS,
  MEMBERSHIP_STATUS_LABELS,
  RESIDENTIAL_RELATIONSHIP_LABELS,
  propertyMemberUnitLabel,
  userStatusLabel,
  userRegistrationStatusLabel
} from '../../models/property-member-labels';
import {
  invitationErrorMessage,
  unlinkErrorMessage
} from '../../models/property-member-error-messages';

@Component({
  selector: 'app-user-management-page',
  imports: [DatePipe, NgOptimizedImage, RouterLink, UserManagementEditor, UserRegistration],
  providers: [UserManagementFacade],
  templateUrl: './user-management-page.html',
  styleUrl: './user-management-page.scss'
})
export class UserManagementPage {

  private readonly facade = inject(UserManagementFacade);
  private readonly memberApi = inject(PropertyMemberApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly selectedMembershipId = signal<string | null>(null);
  protected readonly registrationOpen = signal(false);
  protected readonly unlinkingMembershipId = signal<string | null>(null);
  protected readonly invitingMembershipId = signal<string | null>(null);
  protected readonly notice = signal('');
  protected readonly actionError = signal('');

  protected readonly roleOptions = Object.entries(PROPERTY_ROLE_LABELS).map(
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
      label: RESIDENTIAL_RELATIONSHIP_LABELS[value]
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

  protected readonly state = this.facade.state;

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
    this.facade.reload();
  }

  protected handleRegistrationSaved(message: string): void {
    this.registrationOpen.set(false);
    this.notice.set(message);
    this.facade.reload();
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
        this.facade.reload();
      },
      error: error => {
        this.invitingMembershipId.set(null);
        this.actionError.set(invitationErrorMessage(error));
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
        this.facade.reload();
      },
      error: error => {
        this.unlinkingMembershipId.set(null);
        this.actionError.set(unlinkErrorMessage(error));
      }
    });
  }

  protected roleLabel(role: PropertyRole): string {
    return PROPERTY_ROLE_LABELS[role];
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
    return propertyMemberUnitLabel(unit);
  }

  protected noUnitsLabel(role: PropertyRole): string {
    return role === 'OWNER' || role === 'RESIDENT'
      ? 'Sin unidad asignada'
      : 'No aplica';
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

}
