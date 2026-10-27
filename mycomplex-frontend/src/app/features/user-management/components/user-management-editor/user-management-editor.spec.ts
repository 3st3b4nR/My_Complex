import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';

import { PropertyMemberApiService } from '../../data-access/property-member-api.service';
import {
  PropertyMemberDetails,
  PropertyMemberUpdate
} from '../../models/property-member';
import { UserManagementEditor } from './user-management-editor';

describe('UserManagementEditor', () => {
  const propertyId = '10000000-0000-0000-0000-000000000001';
  const membershipId = '70000000-0000-0000-0000-000000000001';
  const details: PropertyMemberDetails = {
    membershipId,
    userId: '60000000-0000-0000-0000-000000000001',
    firstNames: 'Ana',
    lastNames: 'Gómez',
    email: 'ana.gomez@loscerezos.test',
    role: 'OWNER',
    userStatus: 'ACTIVE',
    registrationStatus: 'LINKED',
    membershipStatus: 'ACTIVE',
    version: 0,
    units: [
      {
        assignmentId: '80000000-0000-0000-0000-000000000001',
        unitId: '50000000-0000-0000-0000-000000000101',
        structureName: 'Torre 1',
        structureType: 'TOWER',
        unitNumber: '101',
        unitType: 'APARTMENT',
        relationshipType: 'OWNER',
        primary: true,
        status: 'ACTIVE'
      }
    ],
    availableUnits: [
      {
        unitId: '50000000-0000-0000-0000-000000000101',
        structureId: '40000000-0000-0000-0000-000000000001',
        structureName: 'Torre 1',
        structureType: 'TOWER',
        unitNumber: '101',
        unitType: 'APARTMENT'
      },
      {
        unitId: '50000000-0000-0000-0000-000000000102',
        structureId: '40000000-0000-0000-0000-000000000001',
        structureName: 'Torre 1',
        structureType: 'TOWER',
        unitNumber: '102',
        unitType: 'APARTMENT'
      }
    ]
  };

  async function createComponent(
    detailsResult: Observable<PropertyMemberDetails> = of(details),
    updateResult: Observable<PropertyMemberDetails> = of({
      ...details,
      version: 1
    })
  ): Promise<{
    fixture: ComponentFixture<UserManagementEditor>;
    updatePropertyMember: ReturnType<typeof vi.fn>;
  }> {
    const updatePropertyMember = vi.fn(() => updateResult);
    await TestBed.configureTestingModule({
      imports: [UserManagementEditor],
      providers: [
        {
          provide: PropertyMemberApiService,
          useValue: {
            getPropertyMemberDetails: () => detailsResult,
            updatePropertyMember
          }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(UserManagementEditor);
    fixture.componentRef.setInput('propertyId', propertyId);
    fixture.componentRef.setInput('membershipId', membershipId);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, updatePropertyMember };
  }

  afterEach(() => {
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('renders global user data as read-only information', async () => {
    const { fixture } = await createComponent();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Ana');
    expect(element.textContent).toContain('Gómez');
    expect(element.textContent).toContain('ana.gomez@loscerezos.test');
    expect(element.textContent).toContain('Activo');
    expect(element.textContent).not.toContain('ACTIVE');
    expect(element.querySelector('input[type="email"]')).toBeNull();
  });

  it.each([
    ['ACTIVE', 'Activo'],
    ['INACTIVE', 'Inactivo'],
    ['BLOCKED', 'Bloqueado']
  ] as const)(
    'translates global user status %s as %s',
    async (userStatus, label) => {
      const { fixture } = await createComponent(of({
        ...details,
        userStatus
      }));
      const text = fixture.nativeElement.textContent as string;

      expect(text).toContain(label);
      expect(text).not.toContain(userStatus);
    }
  );

  it('changes role, membership status and relationship before saving', async () => {
    const { fixture, updatePropertyMember } = await createComponent();
    const element = fixture.nativeElement as HTMLElement;
    changeSelect(element, 'select[formControlName="role"]', 'RESIDENT');
    changeSelect(
      element,
      'select[formControlName="membershipStatus"]',
      'INACTIVE'
    );
    changeSelect(
      element,
      'select[formControlName="relationshipType"]',
      'RESIDENT'
    );
    clickButton(element, 'Guardar cambios');
    fixture.detectChanges();

    const update = (
      updatePropertyMember.mock.calls[0][2]
    ) as PropertyMemberUpdate;
    expect(update.role).toBe('RESIDENT');
    expect(update.membershipStatus).toBe('INACTIVE');
    expect(update.units[0].relationshipType).toBe('RESIDENT');
  });

  it('adds a unit and keeps only one primary assignment', async () => {
    const { fixture, updatePropertyMember } = await createComponent();
    const element = fixture.nativeElement as HTMLElement;
    clickButton(element, 'Agregar unidad');
    fixture.detectChanges();

    const cards = element.querySelectorAll('.assignment-card');
    const secondCard = cards[1] as HTMLElement;
    changeSelect(
      secondCard,
      'select[formControlName="structureId"]',
      '40000000-0000-0000-0000-000000000001'
    );
    fixture.detectChanges();
    changeSelect(
      secondCard,
      'select[formControlName="unitId"]',
      '50000000-0000-0000-0000-000000000102'
    );
    const primary = secondCard.querySelector(
      'input[formControlName="primary"]'
    ) as HTMLInputElement;
    primary.click();
    fixture.detectChanges();
    clickButton(element, 'Guardar cambios');
    fixture.detectChanges();

    const update = (
      updatePropertyMember.mock.calls[0][2]
    ) as PropertyMemberUpdate;
    expect(update.units).toHaveLength(2);
    expect(update.units.filter(unit => unit.primary)).toHaveLength(1);
    expect(update.units[1].unitId).toContain('102');
  });

  it('prevents duplicate units before sending', async () => {
    const { fixture, updatePropertyMember } = await createComponent();
    const component = fixture.componentInstance as any;
    component.addUnit();
    component.unitForms.at(1).patchValue({
      structureId: '40000000-0000-0000-0000-000000000001',
      unitId: '50000000-0000-0000-0000-000000000101'
    });
    component.save();
    fixture.detectChanges();

    expect(updatePropertyMember).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'No puedes asignar la misma unidad más de una vez.'
    );
  });

  it('validates that no more than one unit is primary', async () => {
    const { fixture, updatePropertyMember } = await createComponent();
    const component = fixture.componentInstance as any;
    component.addUnit();
    component.unitForms.at(1).patchValue({
      structureId: '40000000-0000-0000-0000-000000000001',
      unitId: '50000000-0000-0000-0000-000000000102',
      primary: true
    });
    component.save();
    fixture.detectChanges();

    expect(updatePropertyMember).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Solo una unidad puede ser principal.'
    );
  });

  it('confirms and removes an assignment', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const { fixture } = await createComponent();
    const element = fixture.nativeElement as HTMLElement;
    clickButton(element, 'Retirar asignación');
    fixture.detectChanges();

    expect(element.textContent).toContain('No hay unidades asignadas.');
  });

  it('warns before cancelling dirty changes', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const { fixture } = await createComponent();
    const closed = vi.fn();
    fixture.componentInstance.closed.subscribe(closed);
    const element = fixture.nativeElement as HTMLElement;
    changeSelect(element, 'select[formControlName="role"]', 'RESIDENT');
    clickButton(element, 'Cancelar');

    expect(confirm).toHaveBeenCalled();
    expect(closed).not.toHaveBeenCalled();
  });

  it('emits saved and disables buttons while saving', async () => {
    const pending = new Subject<PropertyMemberDetails>();
    const { fixture } = await createComponent(of(details), pending);
    const saved = vi.fn();
    fixture.componentInstance.saved.subscribe(saved);
    const element = fixture.nativeElement as HTMLElement;
    clickButton(element, 'Guardar cambios');
    fixture.detectChanges();

    const saveButton = findButton(element, 'Guardando…');
    expect(saveButton.disabled).toBe(true);
    pending.next({ ...details, version: 1 });
    pending.complete();
    fixture.detectChanges();

    expect(saved).toHaveBeenCalled();
    expect(element.textContent).toContain(
      'Los cambios se guardaron correctamente.'
    );
  });

  it.each([
    [403, 'Acceso no autorizado'],
    [404, 'Vinculación no disponible'],
    [500, 'No pudimos cargar el detalle']
  ])('renders load error %s', async (status, message) => {
    const { fixture } = await createComponent(
      throwError(() => new HttpErrorResponse({ status }))
    );
    expect(fixture.nativeElement.textContent).toContain(message);
  });

  it('renders a specific optimistic concurrency error', async () => {
    const { fixture } = await createComponent(
      of(details),
      throwError(() => new HttpErrorResponse({ status: 409 }))
    );
    const element = fixture.nativeElement as HTMLElement;
    clickButton(element, 'Guardar cambios');
    fixture.detectChanges();

    expect(element.textContent).toContain(
      'Otro administrador modificó esta vinculación.'
    );
  });

  it.each([
    [403, 'No tienes permiso para guardar estos cambios.'],
    [404, 'La vinculación ya no se encuentra disponible.'],
    [500, 'No pudimos guardar los cambios.']
  ])('renders save error %s', async (status, message) => {
    const { fixture } = await createComponent(
      of(details),
      throwError(() => new HttpErrorResponse({ status }))
    );
    const element = fixture.nativeElement as HTMLElement;
    clickButton(element, 'Guardar cambios');
    fixture.detectChanges();

    expect(element.textContent).toContain(message);
  });

  function changeSelect(
    root: HTMLElement,
    selector: string,
    value: string
  ): void {
    const select = root.querySelector(selector) as HTMLSelectElement;
    select.value = value;
    select.dispatchEvent(new Event('change'));
  }

  function clickButton(root: HTMLElement, text: string): void {
    findButton(root, text).click();
  }

  function findButton(root: HTMLElement, text: string): HTMLButtonElement {
    const button = Array.from(root.querySelectorAll('button')).find(
      current => current.textContent?.includes(text)
    );
    if (!(button instanceof HTMLButtonElement)) {
      throw new Error(`Button not found: ${text}`);
    }
    return button;
  }
});
