import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { AssignedProperty } from '../models/assigned-property';

@Injectable({
  providedIn: 'root'
})
export class PropertyApiService {

  private readonly http = inject(HttpClient);

  getMyProperties(): Observable<AssignedProperty[]> {
    return this.http.get<AssignedProperty[]>(
      `${environment.apiUrl}/api/me/properties`
    );
  }
}