import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  PropertyMember,
  PropertyMemberDetails,
  PropertyMemberUpdate,
  PropertyMemberCandidate,
  PropertyMemberRegistration,
  PropertyMemberInvitation
} from '../models/property-member';

@Injectable({
  providedIn: 'root'
})
export class PropertyMemberApiService {

  private readonly http = inject(HttpClient);

  getPropertyMembers(
    propertyId: string
  ): Observable<PropertyMember[]> {
    return this.http.get<PropertyMember[]>(
      `${environment.apiUrl}/api/properties/${propertyId}/members`
    );
  }

  getPropertyMemberDetails(
    propertyId: string,
    membershipId: string
  ): Observable<PropertyMemberDetails> {
    return this.http.get<PropertyMemberDetails>(
      `${environment.apiUrl}/api/properties/${propertyId}`
      + `/members/${membershipId}`
    );
  }

  updatePropertyMember(
    propertyId: string,
    membershipId: string,
    update: PropertyMemberUpdate
  ): Observable<PropertyMemberDetails> {
    return this.http.put<PropertyMemberDetails>(
      `${environment.apiUrl}/api/properties/${propertyId}`
      + `/members/${membershipId}`,
      update
    );
  }

  findPropertyMemberCandidate(
    propertyId: string,
    email: string
  ): Observable<PropertyMemberCandidate> {
    return this.http.get<PropertyMemberCandidate>(
      `${environment.apiUrl}/api/properties/${propertyId}`
      + '/member-candidates',
      { params: { email } }
    );
  }

  registerPropertyMember(
    propertyId: string,
    registration: PropertyMemberRegistration
  ): Observable<PropertyMemberDetails> {
    return this.http.post<PropertyMemberDetails>(
      `${environment.apiUrl}/api/properties/${propertyId}/members`,
      registration
    );
  }

  unlinkPropertyMember(
    propertyId: string,
    membershipId: string,
    version: number
  ): Observable<void> {
    return this.http.delete<void>(
      `${environment.apiUrl}/api/properties/${propertyId}`
      + `/members/${membershipId}`,
      { params: { version } }
    );
  }

  invitePropertyMember(
    propertyId: string,
    membershipId: string
  ): Observable<PropertyMemberInvitation> {
    return this.http.post<PropertyMemberInvitation>(
      `${environment.apiUrl}/api/properties/${propertyId}`
      + `/members/${membershipId}/invitation`,
      null
    );
  }

  resendPropertyMemberInvitation(
    propertyId: string,
    membershipId: string
  ): Observable<PropertyMemberInvitation> {
    return this.http.post<PropertyMemberInvitation>(
      `${environment.apiUrl}/api/properties/${propertyId}`
      + `/members/${membershipId}/invitation/resend`,
      null
    );
  }
}
