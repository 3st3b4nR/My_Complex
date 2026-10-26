export type PropertyRole =
  | 'PROPERTY_ADMIN'
  | 'OWNER'
  | 'RESIDENT'
  | 'SECURITY_GUARD'
  | 'WORKER';

export interface AssignedProperty {
  propertyId: string;
  name: string;
  city: string;
  role: PropertyRole;
}