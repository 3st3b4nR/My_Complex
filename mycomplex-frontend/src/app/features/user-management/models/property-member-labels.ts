import { UserRegistrationStatus, UserStatus } from './property-member';

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
