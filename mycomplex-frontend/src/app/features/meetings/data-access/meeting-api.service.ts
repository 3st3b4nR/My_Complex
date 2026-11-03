import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  MeetingDraft,
  MeetingItem,
  MeetingUpdate
} from '../models/meeting';

@Injectable({
  providedIn: 'root'
})
export class MeetingApiService {

  private readonly http = inject(HttpClient);

  list(propertyId: string): Observable<MeetingItem[]> {
    return this.http.get<MeetingItem[]>(this.baseUrl(propertyId));
  }

  create(
    propertyId: string,
    draft: MeetingDraft
  ): Observable<MeetingItem> {
    return this.http.post<MeetingItem>(
      this.baseUrl(propertyId),
      draft
    );
  }

  update(
    propertyId: string,
    meetingId: string,
    update: MeetingUpdate
  ): Observable<MeetingItem> {
    return this.http.put<MeetingItem>(
      `${this.baseUrl(propertyId)}/${meetingId}`,
      update
    );
  }

  delete(
    propertyId: string,
    meetingId: string,
    version: number
  ): Observable<void> {
    const params = new HttpParams().set('version', version);

    return this.http.delete<void>(
      `${this.baseUrl(propertyId)}/${meetingId}`,
      { params }
    );
  }

  private baseUrl(propertyId: string): string {
    return `${environment.apiUrl}/api/properties/${propertyId}/meetings`;
  }
}
