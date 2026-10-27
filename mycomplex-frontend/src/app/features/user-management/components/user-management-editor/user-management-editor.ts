import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  HostListener,
  inject,
  input,
  OnInit,
  output,
  signal
} from '@angular/core';
import {
  FormArray,
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { PropertyRole } from '../../../property-selection/models/assigned-property';
import { PropertyMemberApiService } from '../../data-access/property-member-api.service';
import {
  AvailableResidentialUnit,
  MembershipStatus,
  PropertyMemberDetails,
  PropertyMemberUpdate,
  ResidentialRelationshipType
} from '../../models/property-member';
import { userStatusLabel } from '../../models/property-member-labels';

type EditorStatus = 'loading' | 'ready' | 'forbidden' | 'not-found' | 'error';
type SaveStatus = 'idle' | 'saving' | 'success' | 'conflict' | 'error';

type UnitForm = FormGroup<{
  structureId: FormControl<string>;
  unitId: FormControl<string>;
  relationshipType: FormControl<ResidentialRelationshipType>;
  primary: FormControl<boolean>;
}>;

const ROLE_OPTIONS: { value: PropertyRole; label: string }[] = [
  { value: 'PROPERTY_ADMIN', label: 'Administrador de copropiedad' },
  { value: 'OWNER', label: 'Propietario' },
  { value: 'RESIDENT', label: 'Residente' },
  { value: 'SECURITY_GUARD', label: 'Vigilante' },
  { value: 'WORKER', label: 'Trabajador' }
];

const MEMBERSHIP_OPTIONS: {
  value: MembershipStatus;
  label: string;
}[] = [
  { value: 'ACTIVE', label: 'Activa' },
  { value: 'INACTIVE', label: 'Inactiva' },
  { value: 'SUSPENDED', label: 'Suspendida' }
];

@Component({
  selector: 'app-user-management-editor',
  imports: [ReactiveFormsModule],
  templateUrl: './user-management-editor.html',
  styleUrl: './user-management-editor.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserManagementEditor implements OnInit {

  readonly propertyId = input.required<string>();
  readonly membershipId = input.required<string>();
  readonly closed = output<void>();
  readonly saved = output<void>();

  private readonly api = inject(PropertyMemberApiService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly roleOptions = ROLE_OPTIONS;
  protected readonly membershipOptions = MEMBERSHIP_OPTIONS;
  protected readonly userStatusLabel = userStatusLabel;
  protected readonly status = signal<EditorStatus>('loading');
  protected readonly saveStatus = signal<SaveStatus>('idle');
  protected readonly saveError = signal('');
  protected readonly assignmentErrors = signal<string[]>([]);
  protected readonly detail = signal<PropertyMemberDetails | null>(null);

  protected readonly form = this.formBuilder.group({
    role: this.formBuilder.control<PropertyRole>(
      'OWNER',
      Validators.required
    ),
    membershipStatus: this.formBuilder.control<MembershipStatus>(
      'ACTIVE',
      Validators.required
    ),
    version: this.formBuilder.control(0),
    units: this.formBuilder.array<UnitForm>([])
  });

  protected readonly structures = computed(() => {
    const units = this.detail()?.availableUnits ?? [];
    const structures = new Map<string, string>();
    for (const unit of units) {
      const id = unit.structureId ?? 'NO_STRUCTURE';
      structures.set(id, unit.structureName ?? 'Sin estructura');
    }
    return Array.from(structures, ([id, name]) => ({ id, name }));
  });

  protected get unitForms(): FormArray<UnitForm> {
    return this.form.controls.units;
  }

  ngOnInit(): void {
    this.loadDetails();
  }

  @HostListener('window:beforeunload', ['$event'])
  protected warnBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.form.dirty && this.saveStatus() !== 'saving') {
      event.preventDefault();
      event.returnValue = '';
    }
  }

  protected availableUnits(index: number): AvailableResidentialUnit[] {
    const structureId = this.unitForms.at(index).controls.structureId.value;
    return (this.detail()?.availableUnits ?? []).filter(unit =>
      (unit.structureId ?? 'NO_STRUCTURE') === structureId
    );
  }

  protected addUnit(): void {
    this.unitForms.push(this.createUnitForm('', '', 'RESIDENT', false));
    this.form.markAsDirty();
    this.clearAssignmentErrors();
  }

  protected structureChanged(index: number): void {
    this.unitForms.at(index).controls.unitId.setValue('');
    this.unitForms.at(index).controls.unitId.markAsDirty();
    this.clearAssignmentErrors();
  }

  protected assignmentChanged(): void {
    this.clearAssignmentErrors();
  }

  protected selectPrimary(index: number, event: Event): void {
    const target = event.target;
    if (!(target instanceof HTMLInputElement) || !target.checked) {
      return;
    }
    this.unitForms.controls.forEach((group, currentIndex) =>
      group.controls.primary.setValue(currentIndex === index)
    );
    this.form.markAsDirty();
    this.clearAssignmentErrors();
  }

  protected removeUnit(index: number): void {
    if (!window.confirm('¿Deseas retirar esta asignación residencial?')) {
      return;
    }
    this.unitForms.removeAt(index);
    this.form.markAsDirty();
    this.clearAssignmentErrors();
  }

  protected cancel(): void {
    if (
      this.form.dirty
      && !window.confirm('Hay cambios sin guardar. ¿Deseas salir?')
    ) {
      return;
    }
    this.closed.emit();
  }

  protected save(): void {
    this.form.markAllAsTouched();
    this.saveError.set('');
    if (!this.validateAssignments() || this.form.invalid) {
      return;
    }

    const value = this.form.getRawValue();
    const update: PropertyMemberUpdate = {
      role: value.role,
      membershipStatus: value.membershipStatus,
      version: value.version,
      units: value.units.map(unit => ({
        unitId: unit.unitId,
        relationshipType: unit.relationshipType,
        primary: unit.primary
      }))
    };

    this.saveStatus.set('saving');
    this.form.disable();
    this.api.updatePropertyMember(
      this.propertyId(),
      this.membershipId(),
      update
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: details => {
        this.detail.set(details);
        this.populateForm(details);
        this.saveStatus.set('success');
        this.saved.emit();
      },
      error: error => {
        this.form.enable();
        this.handleSaveError(error);
      }
    });
  }

  protected hasAssignmentError(error: string): boolean {
    return this.assignmentErrors().includes(error);
  }

  private loadDetails(): void {
    this.status.set('loading');
    this.api.getPropertyMemberDetails(
      this.propertyId(),
      this.membershipId()
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: details => {
        this.detail.set(details);
        this.populateForm(details);
        this.status.set('ready');
      },
      error: error => {
        this.status.set(this.loadErrorStatus(error));
      }
    });
  }

  private populateForm(details: PropertyMemberDetails): void {
    this.unitForms.clear();
    for (const assignment of details.units) {
      const unit = details.availableUnits.find(
        available => available.unitId === assignment.unitId
      );
      this.unitForms.push(this.createUnitForm(
        unit?.structureId ?? 'NO_STRUCTURE',
        assignment.unitId,
        assignment.relationshipType,
        assignment.primary
      ));
    }
    this.form.patchValue({
      role: details.role,
      membershipStatus: details.membershipStatus,
      version: details.version
    });
    this.form.enable();
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.unitForms.setErrors(null);
    this.assignmentErrors.set([]);
  }

  private createUnitForm(
    structureId: string,
    unitId: string,
    relationshipType: ResidentialRelationshipType,
    primary: boolean
  ): UnitForm {
    return this.formBuilder.group({
      structureId: this.formBuilder.control(
        structureId,
        Validators.required
      ),
      unitId: this.formBuilder.control(unitId, Validators.required),
      relationshipType: this.formBuilder.control(
        relationshipType,
        Validators.required
      ),
      primary: this.formBuilder.control(primary)
    });
  }

  private validateAssignments(): boolean {
    const units = this.unitForms.getRawValue();
    const ids = units.map(unit => unit.unitId).filter(Boolean);
    const duplicateUnits = new Set(ids).size !== ids.length;
    const multiplePrimary = units.filter(unit => unit.primary).length > 1;
    const errors: Record<string, boolean> = {};
    if (duplicateUnits) {
      errors['duplicateUnits'] = true;
    }
    if (multiplePrimary) {
      errors['multiplePrimary'] = true;
    }
    this.unitForms.setErrors(Object.keys(errors).length ? errors : null);
    this.assignmentErrors.set(Object.keys(errors));
    return !duplicateUnits && !multiplePrimary;
  }

  private clearAssignmentErrors(): void {
    this.unitForms.setErrors(null);
    this.assignmentErrors.set([]);
    this.saveStatus.set('idle');
    this.saveError.set('');
  }

  private loadErrorStatus(error: unknown): EditorStatus {
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'forbidden';
    }
    if (error instanceof HttpErrorResponse && error.status === 404) {
      return 'not-found';
    }
    return 'error';
  }

  private handleSaveError(error: unknown): void {
    if (error instanceof HttpErrorResponse && error.status === 409) {
      this.saveStatus.set('conflict');
      this.saveError.set(
        'Otro administrador modificó esta vinculación. Cierra el editor y vuelve a intentarlo.'
      );
      return;
    }
    if (error instanceof HttpErrorResponse && error.status === 403) {
      this.saveStatus.set('error');
      this.saveError.set('No tienes permiso para guardar estos cambios.');
      return;
    }
    if (error instanceof HttpErrorResponse && error.status === 404) {
      this.saveStatus.set('error');
      this.saveError.set('La vinculación ya no se encuentra disponible.');
      return;
    }
    const message = error instanceof HttpErrorResponse
      ? error.error?.message
      : null;
    this.saveStatus.set('error');
    this.saveError.set(
      typeof message === 'string'
        ? message
        : 'No pudimos guardar los cambios. Inténtalo nuevamente.'
    );
  }
}
