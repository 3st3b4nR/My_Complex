import { UserStatus } from './property-member';

export const USER_STATUS_LABELS: Readonly<Record<UserStatus, string>> = {
  ACTIVE: 'Activo',
  INACTIVE: 'Inactivo',
  BLOCKED: 'Bloqueado'
};

export function userStatusLabel(status: UserStatus): string {
  return USER_STATUS_LABELS[status];
}
