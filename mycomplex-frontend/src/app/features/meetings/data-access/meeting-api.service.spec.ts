import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { MeetingApiService } from './meeting-api.service';

describe('MeetingApiService', () => {
  let service: MeetingApiService;
  let http: HttpTestingController;
  const propertyId = 'property-1';
  const baseUrl = `${environment.apiUrl}/api/properties/${propertyId}/meetings`;
  const draft = {
    title: 'Asamblea', description: 'Reunión general',
    meetingUrl: 'https://meet.example/1',
    scheduledAt: '2026-12-01T15:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      MeetingApiService,
      provideHttpClient(),
      provideHttpClientTesting()
    ] });
    service = TestBed.inject(MeetingApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists and creates meetings within the property scope', () => {
    service.list(propertyId).subscribe(result => expect(result).toEqual([]));
    const list = http.expectOne(baseUrl);
    expect(list.request.method).toBe('GET');
    list.flush([]);

    service.create(propertyId, draft).subscribe();
    const create = http.expectOne(baseUrl);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(draft);
    create.flush({});
  });

  it('updates and deletes a meeting with optimistic locking', () => {
    service.update(propertyId, 'meeting-1', { ...draft, version: 2 })
      .subscribe();
    const update = http.expectOne(`${baseUrl}/meeting-1`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual({ ...draft, version: 2 });
    update.flush({});

    service.delete(propertyId, 'meeting-1', 2).subscribe();
    const deletion = http.expectOne(candidate =>
      candidate.url === `${baseUrl}/meeting-1`);
    expect(deletion.request.method).toBe('DELETE');
    expect(deletion.request.params.get('version')).toBe('2');
    deletion.flush(null);
  });
});
