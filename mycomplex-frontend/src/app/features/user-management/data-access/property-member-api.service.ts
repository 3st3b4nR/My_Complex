import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PropertyMember } from '../models/property-member';

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
}
