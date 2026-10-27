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
  PropertyMemberUpdate
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
});
