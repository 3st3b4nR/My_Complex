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
  assignmentId: string;
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
  membershipId: string;
  userId: string;
  firstNames: string;
  lastNames: string;
  email: string;
  role: PropertyRole;
  userStatus: UserStatus;
  membershipStatus: MembershipStatus;
  version: number;
  units: PropertyMemberUnit[];
}

export interface AvailableResidentialUnit {
  unitId: string;
  structureId: string | null;
  structureName: string | null;
  structureType: PropertyStructureType | null;
  unitNumber: string;
  unitType: ResidentialUnitType;
}

export interface PropertyMemberDetails extends PropertyMember {
  availableUnits: AvailableResidentialUnit[];
}

export interface PropertyMemberUnitUpdate {
  unitId: string;
  relationshipType: ResidentialRelationshipType;
  primary: boolean;
}

export interface PropertyMemberUpdate {
  role: PropertyRole;
  membershipStatus: MembershipStatus;
  version: number;
  units: PropertyMemberUnitUpdate[];
}
