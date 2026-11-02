import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { PropertyApiService }
  from '../../../property-selection/data-access/property-api.service';
import { AdministrativeTaskApiService }
  from '../../data-access/administrative-task-api.service';
import { TaskManagementPage } from './task-management-page';

describe('TaskManagementPage', () => {
  it('loads the selected property, workers and real task list', async () => {
    const api = {
      find: vi.fn().mockReturnValue(of([{
        id: 'task-1', propertyId: 'property-1', title: 'Revisar ascensor',
        description: 'Validar funcionamiento', priority: 'HIGH',
        status: 'PENDING', location: 'Torre 1',
        dueAt: '2026-12-01T12:00:00Z', overdue: false,
        administrativeNote: null, assignedMembershipId: 'member-1',
        assignedWorkerUserId: 'user-1', assignedWorkerName: 'Ana Trabajadora',
        createdAt: '2026-11-01T12:00:00Z',
        updatedAt: '2026-11-01T12:00:00Z', version: 0,
        notificationStatus: 'PENDING'
      }])),
      findWorkers: vi.fn().mockReturnValue(of([{
        membershipId: 'member-1', displayName: 'Ana Trabajadora'
      }]))
    };
    const properties = {
      getMyProperties: vi.fn().mockReturnValue(of([{
        propertyId: 'property-1', name: 'Los Cerezos', city: 'Rionegro',
        role: 'PROPERTY_ADMIN'
      }]))
    };
    await TestBed.configureTestingModule({
      imports: [TaskManagementPage],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: {
          snapshot: { paramMap: { get: () => 'property-1' } }
        } },
        { provide: AdministrativeTaskApiService, useValue: api },
        { provide: PropertyApiService, useValue: properties }
      ]
    }).compileComponents();

    const fixture: ComponentFixture<TaskManagementPage> =
      TestBed.createComponent(TaskManagementPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Los Cerezos');
    expect(fixture.nativeElement.textContent).toContain('Revisar ascensor');
    expect(fixture.nativeElement.textContent).toContain('Ana Trabajadora');
    expect(api.find).toHaveBeenCalledWith('property-1', expect.any(Object));
  });
});
