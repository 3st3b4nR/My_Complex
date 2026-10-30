import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter
} from '@angular/router';
import { defer, Observable, of, Subject, throwError } from 'rxjs';

import { PropertyApiService } from '../../../property-selection/data-access/property-api.service';
import { AssignedProperty } from '../../../property-selection/models/assigned-property';
import { PropertyMemberApiService } from '../../data-access/property-member-api.service';
import {
  PropertyMember,
  PropertyMemberDetails
} from '../../models/property-member';
import { UserManagementPage } from './user-management-page';

describe('UserManagementPage', () => {
  const invitePropertyMember = vi.fn(() => of({
    membershipId: '30000000-0000-0000-0000-000000000003',
    userId: '20000000-0000-0000-0000-000000000003',
    registrationStatus: 'INVITED' as const,
    invitationSentAt: '2026-10-27T15:00:00Z',
    message: 'Auth0 aceptó la solicitud de invitación.'
  }));
  const resendPropertyMemberInvitation = vi.fn(() => of({
    membershipId: '30000000-0000-0000-0000-000000000003',
    userId: '20000000-0000-0000-0000-000000000003',
    registrationStatus: 'INVITED' as const,
    invitationSentAt: '2026-10-27T15:00:00Z',
    message: 'La invitación fue reenviada. Debe utilizarse el correo más reciente.'
  }));
  const propertyId = '10000000-0000-0000-0000-000000000001';
  const administratorProperty: AssignedProperty = {
    propertyId,
    name: 'Los Cerezos',
    city: 'Rionegro',
    role: 'PROPERTY_ADMIN'
  };
  const members: PropertyMember[] = [
    {
      membershipId: '30000000-0000-0000-0000-000000000001',
      userId: '20000000-0000-0000-0000-000000000001',
      firstNames: 'John Esteban',
      lastNames: 'Roldan Marin',
      email: 'admin@mycomplex.com',
      role: 'PROPERTY_ADMIN',
      userStatus: 'ACTIVE',
      registrationStatus: 'LINKED',
      membershipStatus: 'ACTIVE',
      version: 0,
      units: []
    },
    {
      membershipId: '30000000-0000-0000-0000-000000000002',
      userId: '20000000-0000-0000-0000-000000000002',
      firstNames: 'Laura',
      lastNames: 'Gomez',
      email: 'laura@example.com',
      role: 'RESIDENT',
      userStatus: 'BLOCKED',
      registrationStatus: 'LINKED',
      membershipStatus: 'SUSPENDED',
      version: 0,
      units: [
        {
          assignmentId: '80000000-0000-0000-0000-000000000001',
          unitId: '50000000-0000-0000-0000-000000000101',
          structureName: 'Torre 1',
          structureType: 'TOWER',
          unitNumber: '101',
          unitType: 'APARTMENT',
          relationshipType: 'RESIDENT',
          primary: true,
          status: 'ACTIVE'
        }
      ]
    },
    {
      membershipId: '30000000-0000-0000-0000-000000000003',
      userId: '20000000-0000-0000-0000-000000000003',
      firstNames: 'Carolina',
      lastNames: 'Restrepo',
      email: 'carolina@loscerezos.test',
      role: 'OWNER',
      userStatus: 'ACTIVE',
      registrationStatus: 'PENDING',
      membershipStatus: 'ACTIVE',
      version: 0,
      units: [
        {
          assignmentId: '80000000-0000-0000-0000-000000000002',
          unitId: '50000000-0000-0000-0000-000000000201',
          structureName: 'Torre 2',
          structureType: 'TOWER',
          unitNumber: '201',
          unitType: 'APARTMENT',
          relationshipType: 'OWNER',
          primary: true,
          status: 'ACTIVE'
        },
        {
          assignmentId: '80000000-0000-0000-0000-000000000003',
          unitId: '50000000-0000-0000-0000-000000000202',
          structureName: 'Torre 2',
          structureType: 'TOWER',
          unitNumber: '202',
          unitType: 'APARTMENT',
          relationshipType: 'OWNER',
          primary: false,
          status: 'ACTIVE'
        }
      ]
    },
    {
      membershipId: '30000000-0000-0000-0000-000000000004',
      userId: '20000000-0000-0000-0000-000000000004',
      firstNames: 'Ana',
      lastNames: 'Sin Unidad',
      email: 'ana@loscerezos.test',
      role: 'OWNER',
      userStatus: 'ACTIVE',
      registrationStatus: 'PENDING',
      membershipStatus: 'ACTIVE',
      version: 0,
      units: []
    }
  ];

  async function createComponent(
    assignedProperties: AssignedProperty[],
    membersResult: Observable<PropertyMember[]>,
    detailsResult: Observable<PropertyMemberDetails> = of({
      ...members[1],
      availableUnits: [
        {
          unitId: '50000000-0000-0000-0000-000000000101',
          structureId: '40000000-0000-0000-0000-000000000001',
          structureName: 'Torre 1',
          structureType: 'TOWER',
          unitNumber: '101',
          unitType: 'APARTMENT'
        }
      ]
    })
  ): Promise<ComponentFixture<UserManagementPage>> {
    await TestBed.configureTestingModule({
      imports: [UserManagementPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ propertyId }))
          }
        },
        {
          provide: PropertyApiService,
          useValue: {
            getMyProperties: () => of(assignedProperties)
          }
        },
        {
          provide: PropertyMemberApiService,
          useValue: {
            getPropertyMembers: () => membersResult,
            getPropertyMemberDetails: () => detailsResult,
            updatePropertyMember: () => detailsResult,
            findPropertyMemberCandidate: () => of({}),
            registerPropertyMember: () => detailsResult,
            unlinkPropertyMember: () => of(undefined),
            invitePropertyMember,
            resendPropertyMemberInvitation
          }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(UserManagementPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => {
    invitePropertyMember.mockClear();
    resendPropertyMemberInvitation.mockClear();
    TestBed.resetTestingModule();
  });

  it('renders the loading state', async () => {
    const pendingMembers = new Subject<PropertyMember[]>();
    const fixture = await createComponent(
      [administratorProperty],
      pendingMembers
    );

    expect(fixture.nativeElement.textContent).toContain(
      'Cargando usuarios'
    );
  });

  it('renders members returned by the backend', async () => {
    const fixture = await createComponent(
      [administratorProperty],
      of(members)
    );

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('John Esteban Roldan Marin');
    expect(text).toContain('laura@example.com');
    expect(text).toContain('Torre 1 · Apto. 101 · Residente');
    expect(text).toContain('No aplica');
    expect(text).toContain('Sin unidad asignada');
    expect(text).toContain('Torre 2 · Apto. 201 · Propietario');
    expect(text).toContain('Torre 2 · Apto. 202 · Propietario');
    expect(text).toContain('Vinculado');
    expect(text).toContain('Pendiente');
    expect(text).not.toContain('LINKED');
    expect(text).not.toContain('PENDING');
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length)
      .toBe(4);
  });

  it('renders the empty state', async () => {
    const fixture = await createComponent(
      [administratorProperty],
      of([])
    );

    expect(fixture.nativeElement.textContent).toContain(
      'No hay usuarios vinculados'
    );
  });

  it('renders an error returned by the backend', async () => {
    const fixture = await createComponent(
      [administratorProperty],
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );

    expect(fixture.nativeElement.textContent).toContain(
      'No pudimos cargar los usuarios'
    );
  });

  it('renders access denied for a non-administrator role', async () => {
    const fixture = await createComponent(
      [{
        ...administratorProperty,
        role: 'RESIDENT'
      }],
      of(members)
    );

    expect(fixture.nativeElement.textContent).toContain(
      'Acceso no autorizado'
    );
  });

  it('renders property unavailable when it is not assigned', async () => {
    const fixture = await createComponent([], of(members));

    expect(fixture.nativeElement.textContent).toContain(
      'Copropiedad no disponible'
    );
  });

  it('filters by search, role and both independent statuses', async () => {
    const fixture = await createComponent(
      [administratorProperty],
      of(members)
    );

    const element = fixture.nativeElement as HTMLElement;
    const search = element.querySelector(
      'input[type="search"]'
    ) as HTMLInputElement;
    const selects = element.querySelectorAll('select');

    search.value = 'Laura';
    search.dispatchEvent(new Event('input'));
    selects[0].value = 'RESIDENT';
    selects[0].dispatchEvent(new Event('change'));
    selects[1].value = 'BLOCKED';
    selects[1].dispatchEvent(new Event('change'));
    selects[2].value = 'SUSPENDED';
    selects[2].dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const rows = element.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('Laura Gomez');

    const clearButton = Array.from(
      element.querySelectorAll('button')
    ).find(button => button.textContent?.includes('Limpiar filtros'));
    clearButton?.click();
    fixture.detectChanges();

    expect(element.querySelectorAll('tbody tr').length).toBe(4);
  });

  it('filters by structure and residential relationship type', async () => {
    const fixture = await createComponent(
      [administratorProperty],
      of(members)
    );

    const element = fixture.nativeElement as HTMLElement;
    const selects = element.querySelectorAll('select');

    selects[3].value = 'Torre 2';
    selects[3].dispatchEvent(new Event('change'));
    selects[4].value = 'OWNER';
    selects[4].dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const rows = element.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('Carolina Restrepo');
  });

  it('searches by unit number without duplicating member rows', async () => {
    const fixture = await createComponent(
      [administratorProperty],
      of(members)
    );

    const element = fixture.nativeElement as HTMLElement;
    const search = element.querySelector(
      'input[type="search"]'
    ) as HTMLInputElement;

    search.value = '202';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const rows = element.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('Carolina Restrepo');
  });

  it('opens the editor from a member row and keeps active filters', async () => {
    const fixture = await createComponent(
      [administratorProperty],
      of(members)
    );
    const element = fixture.nativeElement as HTMLElement;
    const search = element.querySelector(
      'input[type="search"]'
    ) as HTMLInputElement;
    search.value = 'Laura';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const editButton = element.querySelector(
      'tbody tr .edit-button'
    ) as HTMLButtonElement;
    editButton.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('app-user-management-editor'))
      .not.toBeNull();
    expect(search.value).toBe('Laura');
  });

  it('opens registration and confirms unlinking without losing filters', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = await createComponent([administratorProperty], of(members));
    const element = fixture.nativeElement as HTMLElement;
    const search = element.querySelector('input[type="search"]') as HTMLInputElement;
    search.value = 'Laura'; search.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const register = Array.from(element.querySelectorAll('button')).find(
      button => button.textContent?.includes('Registrar usuario')
    ) as HTMLButtonElement;
    register.click(); fixture.detectChanges();
    expect(element.querySelector('app-user-registration')).not.toBeNull();
    const close = element.querySelector(
      'app-user-registration button[aria-label="Cerrar registro"]'
    ) as HTMLButtonElement;
    close.click(); fixture.detectChanges();

    const unlink = element.querySelector('tbody tr .unlink-button') as HTMLButtonElement;
    unlink.click(); fixture.detectChanges();
    expect(window.confirm).toHaveBeenCalled();
    expect(search.value).toBe('Laura');
  });

  it('renders translated invitation states and their permitted actions', async () => {
    const invitationMembers: PropertyMember[] = [
      { ...members[2], registrationStatus: 'PENDING' },
      { ...members[2], membershipId: 'provisioning', registrationStatus: 'PROVISIONING' },
      { ...members[2], membershipId: 'provisioned', registrationStatus: 'PROVISIONED' },
      {
        ...members[2],
        membershipId: 'invited',
        registrationStatus: 'INVITED',
        invitationSentAt: '2026-10-27T15:00:00Z'
      },
      { ...members[2], membershipId: 'linked', registrationStatus: 'LINKED' }
    ];
    const fixture = await createComponent(
      [administratorProperty], of(invitationMembers)
    );
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Pendiente');
    expect(text).toContain('Aprovisionando');
    expect(text).toContain('Identidad creada');
    expect(text).toContain('Invitado');
    expect(text).toContain('Vinculado');
    expect(text).toContain('Enviar invitación');
    expect(text).toContain('Reintentar envío');
    expect(text).toContain('Reenviar invitación');
    expect(text).toContain('Último envío:');
    expect(text).not.toContain('PROVISIONED');
  });

  it('confirms invitation, prevents duplicate clicks and keeps filters', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const pendingInvitation = new Subject<{
      membershipId: string;
      userId: string;
      registrationStatus: 'INVITED';
      invitationSentAt: string;
      message: string;
    }>();
    invitePropertyMember.mockReturnValueOnce(pendingInvitation);
    const fixture = await createComponent(
      [administratorProperty], of([members[2]])
    );
    const element = fixture.nativeElement as HTMLElement;
    const search = element.querySelector('input[type="search"]') as HTMLInputElement;
    search.value = 'Carolina';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    const button = element.querySelector(
      '.invitation-button'
    ) as HTMLButtonElement;

    button.click();
    fixture.detectChanges();
    button.click();

    expect(window.confirm).toHaveBeenCalledTimes(1);
    expect(invitePropertyMember).toHaveBeenCalledTimes(1);
    expect(button.disabled).toBe(true);
    expect(search.value).toBe('Carolina');
    pendingInvitation.complete();
  });

  it('uses the explicit resend operation after confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = await createComponent(
      [administratorProperty],
      of([{ ...members[2], registrationStatus: 'INVITED' }])
    );
    const button = fixture.nativeElement.querySelector(
      '.invitation-button'
    ) as HTMLButtonElement;

    button.click();

    expect(resendPropertyMemberInvitation).toHaveBeenCalledTimes(1);
    expect(invitePropertyMember).not.toHaveBeenCalled();
  });

  it('keeps the current list when the invitation action fails', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    let listSubscriptions = 0;
    const membersResult = defer(() => {
      listSubscriptions += 1;
      return listSubscriptions === 1
        ? of([members[2]])
        : throwError(() => new HttpErrorResponse({ status: 500 }));
    });
    invitePropertyMember.mockReturnValueOnce(throwError(() =>
      new HttpErrorResponse({
        status: 503,
        error: { code: 'AUTH0_UNAVAILABLE' }
      })
    ));
    const fixture = await createComponent(
      [administratorProperty], membersResult
    );
    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector(
      '.invitation-button'
    ) as HTMLButtonElement;

    button.click();
    fixture.detectChanges();

    expect(listSubscriptions).toBe(1);
    expect(element.querySelectorAll('tbody tr').length).toBe(1);
    expect(element.textContent).toContain('Carolina Restrepo');
    expect(element.textContent).toContain(
      'Auth0 no está disponible temporalmente.'
    );
    expect(element.textContent).not.toContain(
      'No pudimos cargar los usuarios'
    );
  });
});
