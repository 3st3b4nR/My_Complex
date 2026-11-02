import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { AdministrativeTaskApiService }
  from './administrative-task-api.service';

describe('AdministrativeTaskApiService', () => {
  it('keeps property scope and sends task filters', () => {
    TestBed.configureTestingModule({ providers: [
      AdministrativeTaskApiService,
      provideHttpClient(),
      provideHttpClientTesting()
    ] });
    const service = TestBed.inject(AdministrativeTaskApiService);
    const http = TestBed.inject(HttpTestingController);
    const propertyId = '10000000-0000-0000-0000-000000000001';

    service.find(propertyId, {
      search: 'ascensor', status: 'PENDING', priority: 'HIGH'
    }).subscribe(result => expect(result).toEqual([]));

    const request = http.expectOne(candidate =>
      candidate.url === `${environment.apiUrl}/api/properties/${propertyId}/tasks`
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('search')).toBe('ascensor');
    expect(request.request.params.get('status')).toBe('PENDING');
    expect(request.request.params.get('priority')).toBe('HIGH');
    request.flush([]);
    http.verify();
  });

  it('creates and changes status with optimistic version', () => {
    TestBed.configureTestingModule({ providers: [
      AdministrativeTaskApiService,
      provideHttpClient(),
      provideHttpClientTesting()
    ] });
    const service = TestBed.inject(AdministrativeTaskApiService);
    const http = TestBed.inject(HttpTestingController);
    const propertyId = '10000000-0000-0000-0000-000000000001';
    const taskId = '90000000-0000-0000-0000-000000000001';
    const request = {
      title: 'Revisar ascensor', description: 'Validar funcionamiento',
      priority: 'HIGH' as const, location: null,
      dueAt: '2026-12-01T12:00:00Z', administrativeNote: null,
      assignedMembershipId: '70000000-0000-0000-0000-000000000001'
    };

    service.create(propertyId, request).subscribe();
    const post = http.expectOne(
      `${environment.apiUrl}/api/properties/${propertyId}/tasks`
    );
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(request);
    post.flush({});

    service.changeStatus(propertyId, taskId, 'IN_PROGRESS', 2).subscribe();
    const patch = http.expectOne(
      `${environment.apiUrl}/api/properties/${propertyId}/tasks/${taskId}/status`
    );
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ status: 'IN_PROGRESS', version: 2 });
    patch.flush({});
    http.verify();
  });

  it('loads workers and updates a task in the selected property', () => {
    TestBed.configureTestingModule({ providers: [
      AdministrativeTaskApiService,
      provideHttpClient(),
      provideHttpClientTesting()
    ] });
    const service = TestBed.inject(AdministrativeTaskApiService);
    const http = TestBed.inject(HttpTestingController);
    const propertyId = '10000000-0000-0000-0000-000000000001';
    const taskId = '90000000-0000-0000-0000-000000000001';
    const request = {
      title: 'Actualizar ascensor', description: 'Revisión completa',
      priority: 'URGENT' as const, location: 'Torre 1',
      dueAt: '2026-12-02T12:00:00Z', administrativeNote: 'Prioritaria',
      assignedMembershipId: '70000000-0000-0000-0000-000000000001',
      version: 3
    };

    service.findWorkers(propertyId).subscribe(workers =>
      expect(workers[0].displayName).toBe('Ana Trabajadora'));
    const workers = http.expectOne(
      `${environment.apiUrl}/api/properties/${propertyId}/task-workers`
    );
    expect(workers.request.method).toBe('GET');
    workers.flush([{ membershipId: 'member-1', displayName: 'Ana Trabajadora' }]);

    service.update(propertyId, taskId, request).subscribe();
    const update = http.expectOne(
      `${environment.apiUrl}/api/properties/${propertyId}/tasks/${taskId}`
    );
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(request);
    update.flush({});
    http.verify();
  });

  it('omits empty filters from the request', () => {
    TestBed.configureTestingModule({ providers: [
      AdministrativeTaskApiService,
      provideHttpClient(),
      provideHttpClientTesting()
    ] });
    const service = TestBed.inject(AdministrativeTaskApiService);
    const http = TestBed.inject(HttpTestingController);

    service.find('property-1', {
      search: '', assignedMembershipId: '', overdue: false
    }).subscribe();

    const request = http.expectOne(candidate =>
      candidate.url.endsWith('/api/properties/property-1/tasks'));
    expect(request.request.params.keys()).toEqual([]);
    request.flush([]);
    http.verify();
  });
});
