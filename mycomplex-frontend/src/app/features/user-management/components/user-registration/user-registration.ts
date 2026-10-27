import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { PropertyRole } from '../../../property-selection/models/assigned-property';
import { PropertyMemberApiService } from '../../data-access/property-member-api.service';
import { AvailableResidentialUnit, MembershipStatus, PropertyMemberCandidate, PropertyMemberRegistration, ResidentialRelationshipType } from '../../models/property-member';

type UnitForm = FormGroup<{
  structureId: FormControl<string>;
  unitId: FormControl<string>;
  relationshipType: FormControl<ResidentialRelationshipType>;
  primary: FormControl<boolean>;
}>;

@Component({
  selector: 'app-user-registration',
  imports: [ReactiveFormsModule],
  templateUrl: './user-registration.html',
  styleUrl: './user-registration.scss'
})
export class UserRegistration {
  readonly propertyId = input.required<string>();
  readonly closed = output<void>();
  readonly saved = output<string>();

  private readonly api = inject(PropertyMemberApiService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly status = signal<'email' | 'checking' | 'ready' | 'saving'>('email');
  protected readonly candidate = signal<PropertyMemberCandidate | null>(null);
  protected readonly error = signal('');
  protected readonly form = this.fb.group({
    email: this.fb.control('', [Validators.required, Validators.email, Validators.maxLength(254)]),
    firstNames: this.fb.control('', Validators.maxLength(120)),
    lastNames: this.fb.control('', Validators.maxLength(120)),
    documentType: this.fb.control('', Validators.maxLength(20)),
    documentNumber: this.fb.control('', Validators.maxLength(50)),
    role: this.fb.control<PropertyRole>('RESIDENT', Validators.required),
    membershipStatus: this.fb.control<MembershipStatus>('ACTIVE', Validators.required),
    units: this.fb.array<UnitForm>([])
  });

  protected readonly structures = computed(() => {
    const values = new Map<string, string>();
    for (const unit of this.candidate()?.availableUnits ?? []) {
      values.set(unit.structureId ?? 'NO_STRUCTURE', unit.structureName ?? 'Sin estructura');
    }
    return Array.from(values, ([id, name]) => ({ id, name }));
  });

  protected get unitForms(): FormArray<UnitForm> {
    return this.form.controls.units;
  }

  protected verifyEmail(): void {
    this.form.controls.email.markAsTouched();
    if (this.form.controls.email.invalid) return;
    this.status.set('checking');
    this.error.set('');
    this.api.findPropertyMemberCandidate(this.propertyId(), this.form.controls.email.value.trim())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: candidate => {
          this.candidate.set(candidate);
          if (candidate.firstNames) this.form.controls.firstNames.setValue(candidate.firstNames);
          if (candidate.lastNames) this.form.controls.lastNames.setValue(candidate.lastNames);
          this.status.set('ready');
        },
        error: error => {
          this.status.set('email');
          this.error.set(this.messageFor(error, 'No pudimos verificar el correo.'));
        }
      });
  }

  protected addUnit(): void {
    this.unitForms.push(this.fb.group({
      structureId: this.fb.control('', Validators.required),
      unitId: this.fb.control('', Validators.required),
      relationshipType: this.fb.control<ResidentialRelationshipType>('RESIDENT'),
      primary: this.fb.control(false)
    }));
  }

  protected removeUnit(index: number): void {
    this.unitForms.removeAt(index);
  }

  protected unitsFor(index: number): AvailableResidentialUnit[] {
    const structureId = this.unitForms.at(index).controls.structureId.value;
    return (this.candidate()?.availableUnits ?? []).filter(
      unit => (unit.structureId ?? 'NO_STRUCTURE') === structureId
    );
  }

  protected structureChanged(index: number): void {
    this.unitForms.at(index).controls.unitId.setValue('');
  }

  protected selectPrimary(index: number, event: Event): void {
    const input = event.target;
    if (input instanceof HTMLInputElement && input.checked) {
      this.unitForms.controls.forEach((unit, current) =>
        unit.controls.primary.setValue(current === index)
      );
    }
  }

  protected submit(): void {
    const candidate = this.candidate();
    this.form.markAllAsTouched();
    if (!candidate || this.form.invalid || !this.validateUnits()) return;
    if (candidate.state === 'EXISTING_MEMBERSHIP' || candidate.state === 'DELETED_USER') {
      this.error.set(candidate.state === 'EXISTING_MEMBERSHIP'
        ? 'La persona ya tiene una vinculación. Debes editarla desde el listado.'
        : 'La persona fue eliminada y requiere intervención de un administrador del sistema.');
      return;
    }
    if (candidate.state === 'DELETED_MEMBERSHIP'
        && !window.confirm('La vinculación anterior está eliminada. ¿Deseas reactivarla?')) return;

    const value = this.form.getRawValue();
    const request: PropertyMemberRegistration = {
      mode: candidate.state === 'NEW_USER' ? 'CREATE_PENDING_USER'
        : candidate.state === 'DELETED_MEMBERSHIP' ? 'REACTIVATE_MEMBERSHIP'
        : 'LINK_EXISTING_USER',
      userId: candidate.userId,
      membershipId: candidate.membershipId,
      firstNames: candidate.state === 'NEW_USER' ? value.firstNames.trim() : null,
      lastNames: candidate.state === 'NEW_USER' ? value.lastNames.trim() : null,
      email: value.email.trim(),
      documentType: value.documentType.trim() || null,
      documentNumber: value.documentNumber.trim() || null,
      role: value.role,
      membershipStatus: value.membershipStatus,
      units: value.units.map(unit => ({
        unitId: unit.unitId,
        relationshipType: unit.relationshipType,
        primary: unit.primary
      }))
    };
    if (candidate.state === 'NEW_USER'
        && (!request.firstNames || !request.lastNames)) {
      this.error.set('Nombres y apellidos son obligatorios.');
      return;
    }
    if ((request.documentType === null) !== (request.documentNumber === null)) {
      this.error.set('El tipo y el número de documento deben diligenciarse juntos.');
      return;
    }
    this.status.set('saving');
    this.form.disable();
    this.api.registerPropertyMember(this.propertyId(), request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.saved.emit(candidate.state === 'NEW_USER'
          ? 'Usuario registrado. Su cuenta está pendiente de vinculación.'
          : 'Usuario vinculado correctamente.'),
        error: error => {
          this.form.enable();
          this.status.set('ready');
          this.error.set(this.messageFor(error, 'No pudimos registrar la vinculación.'));
        }
      });
  }

  protected candidateMessage(): string {
    switch (this.candidate()?.state) {
      case 'NEW_USER': return 'El correo no existe. Se registrará una persona nueva pendiente de vinculación.';
      case 'AVAILABLE_EXISTING_USER': return 'Usuario existente disponible para vincular.';
      case 'EXISTING_MEMBERSHIP': return 'Esta persona ya tiene una vinculación con la copropiedad.';
      case 'DELETED_MEMBERSHIP': return 'Existe una vinculación eliminada que puede reactivarse.';
      case 'DELETED_USER': return 'El usuario global fue eliminado y requiere intervención.';
      default: return '';
    }
  }

  private validateUnits(): boolean {
    const units = this.unitForms.getRawValue();
    const ids = units.map(unit => unit.unitId).filter(Boolean);
    if (new Set(ids).size !== ids.length) {
      this.error.set('No puedes asignar la misma unidad más de una vez.');
      return false;
    }
    if (units.filter(unit => unit.primary).length > 1) {
      this.error.set('Solo una unidad puede ser principal.');
      return false;
    }
    return true;
  }

  private messageFor(error: unknown, fallback: string): string {
    if (!(error instanceof HttpErrorResponse)) return fallback;
    if (error.status === 403) return 'No tienes permiso para realizar esta operación.';
    if (error.status === 404) return 'La información solicitada ya no está disponible.';
    if (error.status === 409) return error.error?.message ?? 'La información cambió. Verifica nuevamente el correo.';
    if (error.status === 400) return error.error?.message ?? 'Revisa los datos ingresados.';
    return fallback;
  }
}
