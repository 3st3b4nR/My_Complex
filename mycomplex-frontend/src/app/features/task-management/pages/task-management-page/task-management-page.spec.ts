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

const IN_PROGRESS_TASK: AdministrativeTask = {
  ...TASK,
  id: 'task-2',
  title: 'Pintar portería',
  status: 'IN_PROGRESS',
  version: 1
};
const COMPLETED_TASK: AdministrativeTask = {
  ...TASK,
  id: 'task-3',
  title: 'Limpiar tanque',
  status: 'COMPLETED',
  version: 2
};
const CANCELLED_TASK: AdministrativeTask = {
  ...TASK,
  id: 'task-4',
  title: 'Cambiar luminaria',
  status: 'CANCELLED',
  version: 3
};
const ALL_TASKS = [
  TASK, IN_PROGRESS_TASK, COMPLETED_TASK, CANCELLED_TASK
];

describe('TaskManagementPage', () => {
  async function createPage(overrides: Record<string, unknown> = {}) {
    const api = {
      find: vi.fn().mockReturnValue(of(ALL_TASKS)),
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

  it('opens active tasks and excludes completed and cancelled tasks', async () => {
    const { fixture, api } = await createPage();
    const content = fixture.nativeElement.textContent;

    expect(content).toContain('Los Cerezos');
    expect(content).toContain('Revisar ascensor');
    expect(content).toContain('Pintar portería');
    expect(content).not.toContain('Limpiar tanque');
    expect(content).not.toContain('Cambiar luminaria');
    expect(fixture.nativeElement.querySelector('.editor')).not.toBeNull();
    const activeTab = fixture.nativeElement.querySelector(
      '.task-tabs button:first-child'
    );
    expect(activeTab.getAttribute('aria-pressed')).toBe('true');
    expect(api.find).toHaveBeenCalledWith('property-1', expect.any(Object));
  });

  it('switches to read-only history with only terminal tasks', async () => {
    const { fixture } = await createPage();
    const historyTab = fixture.nativeElement.querySelector(
      '.task-tabs button:last-child'
    ) as HTMLButtonElement;

    historyTab.click();
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(historyTab.getAttribute('aria-pressed')).toBe('true');
    expect(content).toContain('Limpiar tanque');
    expect(content).toContain('Cambiar luminaria');
    expect(content).not.toContain('Revisar ascensor');
    expect(content).not.toContain('Pintar portería');
    expect(fixture.nativeElement.querySelector('.editor')).toBeNull();
    const actions = Array.from(
      fixture.nativeElement.querySelectorAll('article button')
    ).map((button: any) => button.textContent.trim());
    expect(actions).not.toContain('Editar');
    expect(actions).not.toContain('Iniciar');
    expect(actions).not.toContain('Completar');
    expect(actions).not.toContain('Cancelar');
  });

  it('renders the empty and unavailable property states', async () => {
    const empty = await createPage({ find: vi.fn().mockReturnValue(of([])) });
    expect(empty.fixture.nativeElement.textContent)
      .toContain('No hay tareas activas para los filtros seleccionados.');
    empty.component.selectView('history');
    empty.fixture.detectChanges();
    expect(empty.fixture.nativeElement.textContent)
      .toContain('Todavía no hay tareas completadas o canceladas');

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

    component.selectView('history');
    expect(component.filters.controls.status.value).toBe('');
    expect(api.find).toHaveBeenLastCalledWith('property-1', {
      search: 'ascensor', status: undefined, priority: 'HIGH',
      assignedMembershipId: 'member-1', overdue: true
    });
    component.filters.controls.status.setValue('COMPLETED');
    component.load();
    expect(api.find).toHaveBeenLastCalledWith('property-1', {
      search: 'ascensor', status: 'COMPLETED', priority: 'HIGH',
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

  it('moves completed and cancelled tasks to history without duplicates', async () => {
    const changeStatus = vi.fn()
      .mockReturnValueOnce(of({
        ...TASK, status: 'CANCELLED', version: 1
      }))
      .mockReturnValueOnce(of({
        ...IN_PROGRESS_TASK, status: 'COMPLETED', version: 2
      }));
    const { component } = await createPage({
      find: vi.fn().mockReturnValue(of([TASK, IN_PROGRESS_TASK])),
      changeStatus
    });

    component.changeStatus(TASK, 'CANCELLED');
    expect(changeStatus).toHaveBeenCalledWith(
      'property-1', 'task-1', 'CANCELLED', 0
    );
    component.changeStatus(IN_PROGRESS_TASK, 'COMPLETED');

    expect(component.visibleTasks()).toEqual([]);
    expect(component.tasks()).toHaveLength(2);
    component.selectedView.set('history');
    expect(component.visibleTasks().map((task: AdministrativeTask) => task.id))
      .toEqual(['task-1', 'task-2']);
    expect(new Set(component.tasks().map((task: AdministrativeTask) => task.id)).size)
      .toBe(2);
  });

  it('keeps status update failures controlled', async () => {
    const changeStatus = vi.fn().mockReturnValue(
      throwError(() => ({}))
    );
    const { component } = await createPage({ changeStatus });

    component.changeStatus(TASK, 'CANCELLED');
    expect(component.actionError())
      .toBe('No fue posible actualizar el estado.');
  });
});
