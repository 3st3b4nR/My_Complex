import {
  provideHttpClient
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import {
  PropertyMember,
  PropertyMemberDetails,
  PropertyMemberUpdate,
  PropertyMemberCandidate,
  PropertyMemberRegistration
} from '../models/property-member';
import { PropertyMemberApiService } from './property-member-api.service';

describe('PropertyMemberApiService', () => {
  it('requests members for the selected property', () => {
    TestBed.configureTestingModule({
      providers: [
        PropertyMemberApiService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    const service = TestBed.inject(PropertyMemberApiService);
    const http = TestBed.inject(HttpTestingController);
    const propertyId = '10000000-0000-0000-0000-000000000001';
    const expected: PropertyMember[] = [];

    service.getPropertyMembers(propertyId).subscribe(result => {
      expect(result).toEqual(expected);
    });

    const request = http.expectOne(
      `${environment.apiUrl}/api/properties/${propertyId}/members`
    );
    expect(request.request.method).toBe('GET');
    request.flush(expected);
    http.verify();
  });

  it('requests and updates one membership by its identifier', () => {
    TestBed.configureTestingModule({
      providers: [
        PropertyMemberApiService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    const service = TestBed.inject(PropertyMemberApiService);
    const http = TestBed.inject(HttpTestingController);
    const propertyId = '10000000-0000-0000-0000-000000000001';
    const membershipId = '70000000-0000-0000-0000-000000000001';
    const details = {} as PropertyMemberDetails;
    const update: PropertyMemberUpdate = {
      role: 'OWNER',
      membershipStatus: 'ACTIVE',
      version: 0,
      units: []
    };

    service.getPropertyMemberDetails(propertyId, membershipId)
      .subscribe(result => expect(result).toBe(details));
    const getRequest = http.expectOne(
      `${environment.apiUrl}/api/properties/${propertyId}`
      + `/members/${membershipId}`
    );
    expect(getRequest.request.method).toBe('GET');
    getRequest.flush(details);

    service.updatePropertyMember(propertyId, membershipId, update)
      .subscribe(result => expect(result).toBe(details));
    const putRequest = http.expectOne(
      `${environment.apiUrl}/api/properties/${propertyId}`
      + `/members/${membershipId}`
    );
    expect(putRequest.request.method).toBe('PUT');
    expect(putRequest.request.body).toEqual(update);
    putRequest.flush(details);
    http.verify();
  });

  it('checks, registers and unlinks a property member', () => {
    TestBed.configureTestingModule({ providers: [
      PropertyMemberApiService, provideHttpClient(), provideHttpClientTesting()
    ] });
    const service = TestBed.inject(PropertyMemberApiService);
    const http = TestBed.inject(HttpTestingController);
    const propertyId = '10000000-0000-0000-0000-000000000001';
    const membershipId = '70000000-0000-0000-0000-000000000001';
    const candidate = { state: 'NEW_USER' } as PropertyMemberCandidate;
    const registration = { mode: 'CREATE_PENDING_USER' } as PropertyMemberRegistration;

    service.findPropertyMemberCandidate(propertyId, 'ana@test.com').subscribe();
    const candidateRequest = http.expectOne(request =>
      request.url.endsWith(`/api/properties/${propertyId}/member-candidates`)
    );
    expect(candidateRequest.request.params.get('email')).toBe('ana@test.com');
    candidateRequest.flush(candidate);

    service.registerPropertyMember(propertyId, registration).subscribe();
    const post = http.expectOne(
      `${environment.apiUrl}/api/properties/${propertyId}/members`
    );
    expect(post.request.method).toBe('POST');
    post.flush({});

    service.unlinkPropertyMember(propertyId, membershipId, 3).subscribe();
    const deletion = http.expectOne(request =>
      request.url.endsWith(`/members/${membershipId}`)
    );
    expect(deletion.request.method).toBe('DELETE');
    expect(deletion.request.params.get('version')).toBe('3');
    deletion.flush(null);
    http.verify();
  });

  it('requests an invitation and an explicit resend', () => {
    TestBed.configureTestingModule({ providers: [
      PropertyMemberApiService, provideHttpClient(), provideHttpClientTesting()
    ] });
    const service = TestBed.inject(PropertyMemberApiService);
    const http = TestBed.inject(HttpTestingController);
    const propertyId = '10000000-0000-0000-0000-000000000001';
    const membershipId = '70000000-0000-0000-0000-000000000001';

    service.invitePropertyMember(propertyId, membershipId).subscribe();
    const invitation = http.expectOne(
      `${environment.apiUrl}/api/properties/${propertyId}`
      + `/members/${membershipId}/invitation`
    );
    expect(invitation.request.method).toBe('POST');
    expect(invitation.request.body).toBeNull();
    invitation.flush({ registrationStatus: 'INVITED' });

    service.resendPropertyMemberInvitation(propertyId, membershipId)
      .subscribe();
    const resend = http.expectOne(
      `${environment.apiUrl}/api/properties/${propertyId}`
      + `/members/${membershipId}/invitation/resend`
    );
    expect(resend.request.method).toBe('POST');
    resend.flush({ registrationStatus: 'INVITED' });
    http.verify();
  });
});
