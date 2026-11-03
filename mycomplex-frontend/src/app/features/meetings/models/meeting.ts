export interface MeetingItem {
  meetingId: string;
  propertyId: string;
  title: string;
  description: string;
  meetingUrl: string;
  scheduledAt: string;
  updatedAt: string;
  version: number;
}

export interface MeetingDraft {
  title: string;
  description: string;
  meetingUrl: string;
  scheduledAt: string;
}

export interface MeetingUpdate extends MeetingDraft {
  version: number;
}
