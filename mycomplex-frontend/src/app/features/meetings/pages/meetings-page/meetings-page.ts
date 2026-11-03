import {
  DatePipe
} from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import {
  ActivatedRoute,
  Router
} from '@angular/router';
import {
  finalize,
  forkJoin,
  take
} from 'rxjs';

import { PropertyApiService } from '../../../property-selection/data-access/property-api.service';
import { AssignedProperty } from '../../../property-selection/models/assigned-property';
import { PropertyShell } from '../../../../shared/layout/property-shell/property-shell';
import { MeetingApiService } from '../../data-access/meeting-api.service';
import { MeetingItem } from '../../models/meeting';

@Component({
  selector: 'app-meetings-page',
  imports: [
    DatePipe,
    PropertyShell,
    ReactiveFormsModule,
  ],
  templateUrl: './meetings-page.html',
  styleUrl: './meetings-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MeetingsPage implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly meetingApi = inject(MeetingApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly propertyId =
    this.route.snapshot.paramMap.get('propertyId') ?? '';

  protected readonly property = signal<AssignedProperty | null>(null);
  protected readonly meetings = signal<MeetingItem[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly deletingMeetingId = signal<string | null>(null);
  protected readonly pageError = signal<string | null>(null);
  protected readonly formError = signal<string | null>(null);
  protected readonly formVisible = signal(false);
  protected readonly editingMeeting = signal<MeetingItem | null>(null);

  protected readonly isAdministrator = computed(
    () => this.property()?.role === 'PROPERTY_ADMIN'
  );

  protected readonly upcomingMeetings = computed(() => {
    const now = Date.now();
    return this.meetings().filter(
      meeting => Date.parse(meeting.scheduledAt) >= now
    );
  });

  protected readonly pastMeetings = computed(() => {
    const now = Date.now();
    return this.meetings().filter(
      meeting => Date.parse(meeting.scheduledAt) < now
    );
  });

  protected readonly meetingForm =
    this.formBuilder.nonNullable.group({
      title: ['', [
        Validators.required,
        Validators.maxLength(180)
      ]],
      description: ['', Validators.required],
      meetingUrl: ['', [
        Validators.required,
        Validators.pattern(/^https:\/\/.+/i)
      ]],
      scheduledAt: ['', Validators.required]
    });

  ngOnInit(): void {
    if (!this.propertyId) {
      this.router.navigate(['/select-property']);
      return;
    }

    this.loadInitialData();
  }

  protected openCreateForm(): void {
    this.editingMeeting.set(null);
    this.formError.set(null);
    this.meetingForm.reset({
      title: '',
      description: '',
      meetingUrl: '',
      scheduledAt: ''
    });
    this.formVisible.set(true);
  }

  protected openEditForm(meeting: MeetingItem): void {
    this.editingMeeting.set(meeting);
    this.formError.set(null);
    this.meetingForm.reset({
      title: meeting.title,
      description: meeting.description,
      meetingUrl: meeting.meetingUrl,
      scheduledAt: meeting.scheduledAt.slice(0, 16)
    });
    this.formVisible.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected closeForm(): void {
    if (!this.saving()) {
      this.formVisible.set(false);
      this.formError.set(null);
    }
  }

  protected submit(): void {
    if (!this.isAdministrator()) {
      return;
    }

    if (this.meetingForm.invalid) {
      this.meetingForm.markAllAsTouched();
      return;
    }

    const value = this.meetingForm.getRawValue();
    const scheduledAt = new Date(
      `${value.scheduledAt}:00Z`
    ).toISOString();
    const editing = this.editingMeeting();

    this.saving.set(true);
    this.formError.set(null);

    const draft = {
      title: value.title,
      description: value.description,
      meetingUrl: value.meetingUrl,
      scheduledAt
    };

    const request = editing
      ? this.meetingApi.update(
          this.propertyId,
          editing.meetingId,
          {
            ...draft,
            version: editing.version
          }
        )
      : this.meetingApi.create(this.propertyId, draft);

    request.pipe(
      take(1),
      finalize(() => this.saving.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: savedMeeting => {
        this.storeSavedMeeting(savedMeeting);
        this.formVisible.set(false);
        this.editingMeeting.set(null);
      },
      error: error => {
        this.formError.set(this.requestErrorMessage(error));
      }
    });
  }

  protected deleteMeeting(meeting: MeetingItem): void {
    if (!this.isAdministrator()) {
      return;
    }

    if (!window.confirm(
      `¿Deseas eliminar la reunión “${meeting.title}”?`
    )) {
      return;
    }

    this.deletingMeetingId.set(meeting.meetingId);
    this.pageError.set(null);

    this.meetingApi.delete(
      this.propertyId,
      meeting.meetingId,
      meeting.version
    ).pipe(
      take(1),
      finalize(() => this.deletingMeetingId.set(null)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.meetings.update(items => items.filter(
          item => item.meetingId !== meeting.meetingId
        ));
      },
      error: error => {
        this.pageError.set(this.requestErrorMessage(error));
      }
    });
  }

  private loadInitialData(): void {
    this.loading.set(true);
    this.pageError.set(null);

    forkJoin({
      properties: this.propertyApi.getMyProperties(),
      meetings: this.meetingApi.list(this.propertyId)
    }).pipe(
      take(1),
      finalize(() => this.loading.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: ({ properties, meetings }) => {
        const property = properties.find(
          current => current.propertyId === this.propertyId
        );

        if (!property) {
          this.pageError.set(
            'La copropiedad no está asociada con tu usuario.'
          );
          return;
        }

        this.property.set(property);
        this.meetings.set(meetings);
      },
      error: error => {
        this.pageError.set(this.requestErrorMessage(error));
      }
    });
  }

  private storeSavedMeeting(savedMeeting: MeetingItem): void {
    this.meetings.update(items => {
      const withoutCurrent = items.filter(
        item => item.meetingId !== savedMeeting.meetingId
      );
      return [...withoutCurrent, savedMeeting].sort(
        (left, right) =>
          Date.parse(left.scheduledAt)
          - Date.parse(right.scheduledAt)
      );
    });
  }

  private requestErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        return 'La sesión expiró. Inicia sesión nuevamente.';
      }
      if (error.status === 403) {
        return 'No tienes permisos para realizar esta acción.';
      }
      if (error.status === 409) {
        return 'La reunión cambió. Recarga la página e inténtalo otra vez.';
      }
      if (error.status === 400) {
        return 'Verifica la fecha UTC y el enlace HTTPS de la reunión.';
      }
    }

    return 'No fue posible completar la operación. Inténtalo nuevamente.';
  }
}
