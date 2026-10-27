import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { PropertyMemberApiService } from '../../data-access/property-member-api.service';
import { PropertyMemberCandidate, PropertyMemberDetails } from '../../models/property-member';
import { UserRegistration } from './user-registration';

describe('UserRegistration', () => {
  const propertyId = '10000000-0000-0000-0000-000000000001';
  const candidate: PropertyMemberCandidate = {
    state: 'NEW_USER', userId: null, membershipId: null,
    firstNames: null, lastNames: null, email: 'new@test.com',
    registrationStatus: null, availableUnits: []
  };

  async function create(
    current: PropertyMemberCandidate = candidate
  ): Promise<{ fixture: ComponentFixture<UserRegistration>; register: ReturnType<typeof vi.fn> }> {
    const register = vi.fn(() => of({} as PropertyMemberDetails));
    await TestBed.configureTestingModule({
      imports: [UserRegistration],
      providers: [{ provide: PropertyMemberApiService, useValue: {
        findPropertyMemberCandidate: () => of(current),
        registerPropertyMember: register
      }}]
    }).compileComponents();
    const fixture = TestBed.createComponent(UserRegistration);
    fixture.componentRef.setInput('propertyId', propertyId);
    fixture.detectChanges();
    return { fixture, register };
  }

  afterEach(() => TestBed.resetTestingModule());

  it('opens and verifies an exact email for a new user', async () => {
    const { fixture } = await create();
    const root = fixture.nativeElement as HTMLElement;
    const email = root.querySelector('input[type="email"]') as HTMLInputElement;
    email.value = 'new@test.com'; email.dispatchEvent(new Event('input'));
    findButton(root, 'Verificar correo').click(); fixture.detectChanges();
    expect(root.textContent).toContain('persona nueva pendiente');
  });

  it('shows existing and reactivation candidate states', async () => {
    const { fixture } = await create({ ...candidate, state: 'DELETED_MEMBERSHIP', membershipId: 'membership' });
    const component = fixture.componentInstance as any;
    component.form.controls.email.setValue('new@test.com'); component.verifyEmail(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('puede reactivarse');
  });

  it('validates required names before registering a new user', async () => {
    const { fixture, register } = await create();
    const component = fixture.componentInstance as any;
    component.form.controls.email.setValue('new@test.com'); component.verifyEmail(); fixture.detectChanges();
    component.submit(); fixture.detectChanges();
    expect(register).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Nombres y apellidos son obligatorios');
  });

  it('registers a pending user and emits the Spanish success message', async () => {
    const { fixture, register } = await create();
    const component = fixture.componentInstance as any;
    const saved = vi.fn(); fixture.componentInstance.saved.subscribe(saved);
    component.form.patchValue({ email: 'new@test.com', firstNames: 'Ana', lastNames: 'Nueva' });
    component.verifyEmail(); fixture.detectChanges(); component.submit();
    expect(register).toHaveBeenCalled();
    expect(saved).toHaveBeenCalledWith('Usuario registrado. Su cuenta está pendiente de vinculación.');
  });

  function findButton(root: HTMLElement, text: string): HTMLButtonElement {
    return Array.from(root.querySelectorAll('button')).find(
      button => button.textContent?.includes(text)
    ) as HTMLButtonElement;
  }
});
