import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  AdministrativeTask,
  TaskFilters,
  TaskStatus,
  TaskWorker,
  TaskWriteRequest
} from '../models/administrative-task';

@Injectable({ providedIn: 'root' })
export class AdministrativeTaskApiService {
  private readonly http = inject(HttpClient);

  find(propertyId: string, filters: TaskFilters = {}):
  Observable<AdministrativeTask[]> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value) params = params.set(key, String(value));
    });
    return this.http.get<AdministrativeTask[]>(this.base(propertyId), {
      params
    });
  }

  findWorkers(propertyId: string): Observable<TaskWorker[]> {
    return this.http.get<TaskWorker[]>(
      `${environment.apiUrl}/api/properties/${propertyId}/task-workers`
    );
  }

  create(propertyId: string, request: TaskWriteRequest):
  Observable<AdministrativeTask> {
    return this.http.post<AdministrativeTask>(this.base(propertyId), request);
  }

  update(propertyId: string, taskId: string, request: TaskWriteRequest):
  Observable<AdministrativeTask> {
    return this.http.put<AdministrativeTask>(
      `${this.base(propertyId)}/${taskId}`, request
    );
  }

  changeStatus(propertyId: string, taskId: string, status: TaskStatus,
    version: number): Observable<AdministrativeTask> {
    return this.http.patch<AdministrativeTask>(
      `${this.base(propertyId)}/${taskId}/status`, { status, version }
    );
  }

  private base(propertyId: string): string {
    return `${environment.apiUrl}/api/properties/${propertyId}/tasks`;
  }
}
