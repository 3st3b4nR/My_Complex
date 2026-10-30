import { PropertyRole } from '../../property-selection/models/assigned-property';
import {
  MembershipStatus,
  PropertyMemberUnit,
  ResidentialRelationshipType,
  UserRegistrationStatus,
  UserStatus
} from './property-member';

export const PROPERTY_ROLE_LABELS: Readonly<Record<PropertyRole, string>> = {
  PROPERTY_ADMIN: 'Administrador de copropiedad',
  OWNER: 'Propietario',
  RESIDENT: 'Residente',
  SECURITY_GUARD: 'Vigilante',
  WORKER: 'Trabajador'
};

export const MEMBERSHIP_STATUS_LABELS: Readonly<
  Record<MembershipStatus, string>
> = {
  ACTIVE: 'Activa',
  INACTIVE: 'Inactiva',
  SUSPENDED: 'Suspendida'
};

export const RESIDENTIAL_RELATIONSHIP_LABELS: Readonly<
  Record<ResidentialRelationshipType, string>
> = {
  OWNER: 'Propietario',
  RESIDENT: 'Residente'
};

export const USER_STATUS_LABELS: Readonly<Record<UserStatus, string>> = {
  ACTIVE: 'Activo',
  INACTIVE: 'Inactivo',
  BLOCKED: 'Bloqueado'
};

export function userStatusLabel(status: UserStatus): string {
  return USER_STATUS_LABELS[status];
}

export const USER_REGISTRATION_STATUS_LABELS: Readonly<
  Record<UserRegistrationStatus, string>
> = {
  PENDING: 'Pendiente',
  PROVISIONING: 'Aprovisionando',
  PROVISIONED: 'Identidad creada',
  INVITED: 'Invitado',
  LINKED: 'Vinculado'
};

export function userRegistrationStatusLabel(
  status: UserRegistrationStatus
): string {
  return USER_REGISTRATION_STATUS_LABELS[status];
}

export function propertyMemberUnitLabel(unit: PropertyMemberUnit): string {
  const structure = unit.structureName ? `${unit.structureName} · ` : '';
  let unitLabel: string;
  switch (unit.unitType) {
    case 'APARTMENT': unitLabel = `Apto. ${unit.unitNumber}`; break;
    case 'HOUSE': unitLabel = `Casa ${unit.unitNumber}`; break;
    case 'COMMERCIAL_SPACE': unitLabel = `Local ${unit.unitNumber}`; break;
    case 'OFFICE': unitLabel = `Oficina ${unit.unitNumber}`; break;
    case 'OTHER': unitLabel = `Unidad ${unit.unitNumber}`; break;
  }
  return `${structure}${unitLabel} · `
    + RESIDENTIAL_RELATIONSHIP_LABELS[unit.relationshipType];
}
