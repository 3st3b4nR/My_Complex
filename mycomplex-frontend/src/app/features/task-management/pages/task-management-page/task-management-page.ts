import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators }
  from '@angular/forms';
import {
  ActivatedRoute,
  RouterLink
} from '@angular/router';
import { forkJoin } from 'rxjs';

import { AdministrativeTaskApiService }
  from '../../data-access/administrative-task-api.service';
import {
  AdministrativeTask,
  TaskPriority,
  TaskStatus,
  TaskWorker,
  TaskWriteRequest
} from '../../models/administrative-task';
import { PropertyApiService }
  from '../../../property-selection/data-access/property-api.service';
import { PropertyShell }
  from '../../../../shared/layout/property-shell/property-shell';

type PageStatus = 'loading' | 'ready' | 'forbidden' | 'not-found' | 'error';
type TaskView = 'active' | 'history';

const ACTIVE_TASK_STATUSES: readonly TaskStatus[] = [
  'PENDING', 'IN_PROGRESS'
];
const HISTORY_TASK_STATUSES: readonly TaskStatus[] = [
  'COMPLETED', 'CANCELLED'
];

@Component({
  selector: 'app-task-management-page',
  imports: [DatePipe, PropertyShell, ReactiveFormsModule, RouterLink],
  templateUrl: './task-management-page.html',
  styleUrl: './task-management-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaskManagementPage {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(AdministrativeTaskApiService);
  private readonly propertiesApi = inject(PropertyApiService);

  protected readonly propertyId =
    this.route.snapshot.paramMap.get('propertyId') ?? '';
  protected readonly status = signal<PageStatus>('loading');
  protected readonly tasks = signal<AdministrativeTask[]>([]);
  protected readonly selectedView = signal<TaskView>('active');
  protected readonly propertyName = signal('');
  protected readonly propertyCity = signal('');
  protected readonly workers = signal<TaskWorker[]>([]);
  protected readonly selected = signal<AdministrativeTask | null>(null);
  protected readonly saving = signal(false);
  protected readonly actionError = signal('');
  protected readonly isEditing = computed(() => this.selected() !== null);
  protected readonly statusOptions = computed(() =>
    this.selectedView() === 'active'
      ? ACTIVE_TASK_STATUSES
      : HISTORY_TASK_STATUSES
  );
  protected readonly visibleTasks = computed(() => {
    const statuses = this.statusOptions();
    return this.tasks().filter(task => statuses.includes(task.status));
  });

  protected readonly filters = new FormGroup({
    search: new FormControl('', { nonNullable: true }),
    status: new FormControl('', { nonNullable: true }),
    priority: new FormControl('', { nonNullable: true }),
    assignedMembershipId: new FormControl('', { nonNullable: true }),
    overdue: new FormControl(false, { nonNullable: true })
  });

  protected readonly form = new FormGroup({
    title: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(180)]
    }),
    description: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(2000)]
    }),
    priority: new FormControl<TaskPriority>('MEDIUM', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    location: new FormControl('', { nonNullable: true }),
    dueAt: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    administrativeNote: new FormControl('', { nonNullable: true }),
    assignedMembershipId: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required]
    })
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.status.set('loading');
    const raw = this.filters.getRawValue();
    forkJoin({
      tasks: this.api.find(this.propertyId, {
        search: raw.search || undefined,
        status: (raw.status || undefined) as TaskStatus | undefined,
        priority: (raw.priority || undefined) as TaskPriority | undefined,
        assignedMembershipId: raw.assignedMembershipId || undefined,
        overdue: raw.overdue || undefined
      }),
      workers: this.api.findWorkers(this.propertyId),
      properties: this.propertiesApi.getMyProperties()
    }).subscribe({
      next: result => {
        const property = result.properties.find(item =>
          item.propertyId === this.propertyId
        );
        if (!property) {
          this.status.set('not-found');
          return;
        }
        this.propertyName.set(property.name);
        this.propertyCity.set(property.city);
        this.tasks.set(result.tasks);
        this.workers.set(result.workers);
        this.status.set('ready');
      },
      error: error => this.status.set(error.status === 403
        ? 'forbidden' : 'error')
    });
  }

  protected clearFilters(): void {
    this.filters.reset({
      search: '', status: '', priority: '', assignedMembershipId: '',
      overdue: false
    });
    this.load();
  }

  protected selectView(view: TaskView): void {
    if (view === this.selectedView()) return;
    this.selectedView.set(view);
    this.selected.set(null);
    this.actionError.set('');

    if (this.filters.controls.status.value) {
      this.filters.controls.status.setValue('');
      this.load();
    }
  }

  protected edit(task: AdministrativeTask): void {
    this.selected.set(task);
    this.actionError.set('');
    this.form.reset({
      title: task.title,
      description: task.description,
      priority: task.priority,
      location: task.location ?? '',
      dueAt: task.dueAt.slice(0, 16),
      administrativeNote: task.administrativeNote ?? '',
      assignedMembershipId: task.assignedMembershipId
    });
  }

  protected newTask(): void {
    this.selected.set(null);
    this.actionError.set('');
    this.form.reset({
      title: '', description: '', priority: 'MEDIUM', location: '',
      dueAt: '', administrativeNote: '', assignedMembershipId: ''
    });
  }

  protected save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request: TaskWriteRequest = {
      ...value,
      location: value.location || null,
      administrativeNote: value.administrativeNote || null,
      dueAt: new Date(value.dueAt).toISOString(),
      version: this.selected()?.version
    };
    this.saving.set(true);
    this.actionError.set('');
    const operation = this.selected()
      ? this.api.update(
          this.propertyId, this.selected()!.id, request
        )
      : this.api.create(this.propertyId, request);
    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.newTask();
        this.load();
      },
      error: error => {
        this.saving.set(false);
        this.actionError.set(error.error?.message
          ?? 'No fue posible guardar la tarea.');
      }
    });
  }

  protected changeStatus(task: AdministrativeTask, status: TaskStatus): void {
    this.actionError.set('');
    this.api.changeStatus(
      this.propertyId, task.id, status, task.version
    ).subscribe({
      next: updatedTask => {
        this.tasks.update(tasks => tasks.map(current =>
          current.id === updatedTask.id ? updatedTask : current
        ));
        if (this.selected()?.id === updatedTask.id
          && HISTORY_TASK_STATUSES.includes(updatedTask.status)) {
          this.selected.set(null);
        }
      },
      error: error => this.actionError.set(error.error?.message
        ?? 'No fue posible actualizar el estado.')
    });
  }

  protected priorityLabel(priority: TaskPriority): string {
    return { LOW: 'Baja', MEDIUM: 'Media', HIGH: 'Alta', URGENT: 'Urgente' }
      [priority];
  }

  protected statusLabel(status: TaskStatus): string {
    return {
      PENDING: 'Pendiente', IN_PROGRESS: 'En progreso',
      COMPLETED: 'Completada', CANCELLED: 'Cancelada'
    }[status];
  }
}
