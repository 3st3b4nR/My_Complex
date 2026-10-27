import {
  provideHttpClient
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { PropertyMember } from '../models/property-member';
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
});
