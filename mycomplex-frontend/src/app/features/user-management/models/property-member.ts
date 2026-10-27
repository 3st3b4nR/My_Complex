import { PropertyRole } from '../../property-selection/models/assigned-property';

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';

export type MembershipStatus =
  | 'ACTIVE'
  | 'INACTIVE'
  | 'SUSPENDED';

export interface PropertyMember {
  userId: string;
  firstNames: string;
  lastNames: string;
  email: string;
  role: PropertyRole;
  userStatus: UserStatus;
  membershipStatus: MembershipStatus;
}
