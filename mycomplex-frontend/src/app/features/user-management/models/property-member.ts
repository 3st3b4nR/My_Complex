import { PropertyRole } from '../../property-selection/models/assigned-property';

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';

export type MembershipStatus =
  | 'ACTIVE'
  | 'INACTIVE'
  | 'SUSPENDED';

export type PropertyStructureType =
  | 'TOWER'
  | 'BLOCK'
  | 'SECTOR'
  | 'OTHER';

export type ResidentialUnitType =
  | 'APARTMENT'
  | 'HOUSE'
  | 'COMMERCIAL_SPACE'
  | 'OFFICE'
  | 'OTHER';

export type ResidentialRelationshipType = 'OWNER' | 'RESIDENT';

export type ResidentialRelationshipStatus =
  | 'ACTIVE'
  | 'INACTIVE'
  | 'ENDED';

export interface PropertyMemberUnit {
  unitId: string;
  structureName: string | null;
  structureType: PropertyStructureType | null;
  unitNumber: string;
  unitType: ResidentialUnitType;
  relationshipType: ResidentialRelationshipType;
  primary: boolean;
  status: ResidentialRelationshipStatus;
}

export interface PropertyMember {
  userId: string;
  firstNames: string;
  lastNames: string;
  email: string;
  role: PropertyRole;
  userStatus: UserStatus;
  membershipStatus: MembershipStatus;
  units: PropertyMemberUnit[];
}
