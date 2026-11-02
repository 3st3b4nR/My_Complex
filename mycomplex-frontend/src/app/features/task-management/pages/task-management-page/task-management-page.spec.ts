import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { PropertyApiService }
  from '../../../property-selection/data-access/property-api.service';
import { AdministrativeTaskApiService }
  from '../../data-access/administrative-task-api.service';
import { AdministrativeTask } from '../../models/administrative-task';
import { TaskManagementPage } from './task-management-page';

const TASK: AdministrativeTask = {
  id: 'task-1', propertyId: 'property-1', title: 'Revisar ascensor',
  description: 'Validar funcionamiento', priority: 'HIGH',
  status: 'PENDING', location: 'Torre 1',
  dueAt: '2026-12-01T12:00:00Z', overdue: false,
  administrativeNote: null, assignedMembershipId: 'member-1',
  assignedWorkerUserId: 'user-1', assignedWorkerName: 'Ana Trabajadora',
  createdAt: '2026-11-01T12:00:00Z',
  updatedAt: '2026-11-01T12:00:00Z', version: 0,
  notificationStatus: 'PENDING'
};

describe('TaskManagementPage', () => {
  async function createPage(overrides: Record<string, unknown> = {}) {
    const api = {
      find: vi.fn().mockReturnValue(of([TASK])),
      findWorkers: vi.fn().mockReturnValue(of([{
        membershipId: 'member-1', displayName: 'Ana Trabajadora'
      }])),
      create: vi.fn().mockReturnValue(of(TASK)),
      update: vi.fn().mockReturnValue(of(TASK)),
      changeStatus: vi.fn().mockReturnValue(of(TASK)),
      ...overrides
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
    return { fixture, component: fixture.componentInstance as any, api,
      properties };
  }

  it('loads the selected property, workers and real task list', async () => {
    const { fixture, api } = await createPage();

    expect(fixture.nativeElement.textContent).toContain('Los Cerezos');
    expect(fixture.nativeElement.textContent).toContain('Revisar ascensor');
    expect(fixture.nativeElement.textContent).toContain('Ana Trabajadora');
    expect(api.find).toHaveBeenCalledWith('property-1', expect.any(Object));
  });

  it('renders the empty and unavailable property states', async () => {
    const empty = await createPage({ find: vi.fn().mockReturnValue(of([])) });
    expect(empty.fixture.nativeElement.textContent)
      .toContain('No hay tareas para los filtros seleccionados.');

    TestBed.resetTestingModule();
    const unavailable = await createPage();
    unavailable.properties.getMyProperties.mockReturnValue(of([]));
    unavailable.component.load();
    unavailable.fixture.detectChanges();
    expect(unavailable.fixture.nativeElement.textContent)
      .toContain('La copropiedad no está disponible');
  });

  it('distinguishes forbidden and general load errors', async () => {
    const forbidden = await createPage({
      find: vi.fn().mockReturnValue(throwError(() => ({ status: 403 })))
    });
    expect(forbidden.fixture.nativeElement.textContent)
      .toContain('No tienes permisos');

    TestBed.resetTestingModule();
    const failed = await createPage({
      find: vi.fn().mockReturnValue(throwError(() => ({ status: 500 })))
    });
    expect(failed.fixture.nativeElement.textContent)
      .toContain('No pudimos cargar las tareas');
  });

  it('applies and clears the main filters', async () => {
    const { component, api } = await createPage();
    component.filters.setValue({
      search: 'ascensor', status: 'PENDING', priority: 'HIGH',
      assignedMembershipId: 'member-1', overdue: true
    });

    component.load();

    expect(api.find).toHaveBeenLastCalledWith('property-1', {
      search: 'ascensor', status: 'PENDING', priority: 'HIGH',
      assignedMembershipId: 'member-1', overdue: true
    });

    component.clearFilters();
    expect(component.filters.getRawValue()).toEqual({
      search: '', status: '', priority: '', assignedMembershipId: '',
      overdue: false
    });
  });

  it('validates required data and creates a task', async () => {
    const { component, api } = await createPage();
    component.save();
    expect(api.create).not.toHaveBeenCalled();
    expect(component.form.controls.title.touched).toBe(true);

    component.form.setValue({
      title: 'Revisar bomba', description: 'Validar presión',
      priority: 'URGENT', location: '', dueAt: '2026-12-02T08:30',
      administrativeNote: '', assignedMembershipId: 'member-1'
    });
    component.save();

    expect(api.create).toHaveBeenCalledWith('property-1', expect.objectContaining({
      title: 'Revisar bomba', location: null, administrativeNote: null,
      assignedMembershipId: 'member-1'
    }));
    expect(component.saving()).toBe(false);
  });

  it('edits a task and reports save failures', async () => {
    const update = vi.fn().mockReturnValue(
      throwError(() => ({ error: { message: 'Conflicto de versión' } }))
    );
    const { component } = await createPage({ update });
    component.edit(TASK);

    expect(component.isEditing()).toBe(true);
    expect(component.form.controls.title.value).toBe('Revisar ascensor');
    component.save();

    expect(update).toHaveBeenCalledWith('property-1', 'task-1',
      expect.objectContaining({ version: 0 }));
    expect(component.actionError()).toBe('Conflicto de versión');
    expect(component.saving()).toBe(false);
  });

  it('changes and cancels status while preserving controlled errors', async () => {
    const changeStatus = vi.fn()
      .mockReturnValueOnce(of({ ...TASK, status: 'IN_PROGRESS' }))
      .mockReturnValueOnce(throwError(() => ({})));
    const { component, api } = await createPage({ changeStatus });

    component.changeStatus(TASK, 'IN_PROGRESS');
    expect(changeStatus).toHaveBeenCalledWith(
      'property-1', 'task-1', 'IN_PROGRESS', 0
    );
    expect(api.find).toHaveBeenCalledTimes(2);

    component.changeStatus(TASK, 'CANCELLED');
    expect(component.actionError())
      .toBe('No fue posible actualizar el estado.');
  });
});
