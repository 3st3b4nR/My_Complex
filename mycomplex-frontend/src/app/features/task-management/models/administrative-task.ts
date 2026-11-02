export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TaskStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export interface AdministrativeTask {
  id: string;
  propertyId: string;
  title: string;
  description: string;
  priority: TaskPriority;
  status: TaskStatus;
  location: string | null;
  dueAt: string;
  overdue: boolean;
  administrativeNote: string | null;
  assignedMembershipId: string;
  assignedWorkerUserId: string;
  assignedWorkerName: string;
  createdAt: string;
  updatedAt: string;
  version: number;
  notificationStatus: string | null;
}

export interface TaskWorker {
  membershipId: string;
  displayName: string;
}

export interface TaskWriteRequest {
  title: string;
  description: string;
  priority: TaskPriority;
  location: string | null;
  dueAt: string;
  administrativeNote: string | null;
  assignedMembershipId: string;
  version?: number;
}

export interface TaskFilters {
  search?: string;
  status?: TaskStatus;
  priority?: TaskPriority;
  assignedMembershipId?: string;
  overdue?: boolean;
}
