import { PropertyRole } from '../../property-selection/models/assigned-property';

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';
export type UserRegistrationStatus =
  | 'PENDING'
  | 'PROVISIONING'
  | 'PROVISIONED'
  | 'INVITED'
  | 'LINKED';

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
  registrationStatus: UserRegistrationStatus;
  membershipStatus: MembershipStatus;
  version: number;
  invitationSentAt?: string | null;
  units: PropertyMemberUnit[];
}

export interface PropertyMemberInvitation {
  membershipId: string;
  userId: string;
  registrationStatus: UserRegistrationStatus;
  invitationSentAt: string | null;
  message: string;
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

export type PropertyMemberCandidateState =
  | 'NEW_USER'
  | 'AVAILABLE_EXISTING_USER'
  | 'EXISTING_MEMBERSHIP'
  | 'DELETED_MEMBERSHIP'
  | 'DELETED_USER';

export interface PropertyMemberCandidate {
  state: PropertyMemberCandidateState;
  userId: string | null;
  membershipId: string | null;
  firstNames: string | null;
  lastNames: string | null;
  email: string;
  registrationStatus: UserRegistrationStatus | null;
  availableUnits: AvailableResidentialUnit[];
}

export type PropertyMemberRegistrationMode =
  | 'CREATE_PENDING_USER'
  | 'LINK_EXISTING_USER'
  | 'REACTIVATE_MEMBERSHIP';

export interface PropertyMemberRegistration {
  mode: PropertyMemberRegistrationMode;
  userId: string | null;
  membershipId: string | null;
  firstNames: string | null;
  lastNames: string | null;
  email: string;
  documentType: string | null;
  documentNumber: string | null;
  role: PropertyRole;
  membershipStatus: MembershipStatus;
  units: PropertyMemberUnitUpdate[];
}
