import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter
} from '@angular/router';
import { Observable, of, Subject, throwError } from 'rxjs';

import { PropertyApiService } from '../../../property-selection/data-access/property-api.service';
import { AssignedProperty } from '../../../property-selection/models/assigned-property';
import { PropertyMemberApiService } from '../../data-access/property-member-api.service';
import { PropertyMember } from '../../models/property-member';
import { UserManagementPage } from './user-management-page';

describe('UserManagementPage', () => {
  const propertyId = '10000000-0000-0000-0000-000000000001';
  const administratorProperty: AssignedProperty = {
    propertyId,
    name: 'Los Cerezos',
    city: 'Rionegro',
    role: 'PROPERTY_ADMIN'
  };
  const members: PropertyMember[] = [
    {
      userId: '20000000-0000-0000-0000-000000000001',
      firstNames: 'John Esteban',
      lastNames: 'Roldan Marin',
      email: 'admin@mycomplex.com',
      role: 'PROPERTY_ADMIN',
      userStatus: 'ACTIVE',
      membershipStatus: 'ACTIVE',
      units: []
    },
    {
      userId: '20000000-0000-0000-0000-000000000002',
      firstNames: 'Laura',
      lastNames: 'Gomez',
      email: 'laura@example.com',
      role: 'RESIDENT',
      userStatus: 'BLOCKED',
      membershipStatus: 'SUSPENDED',
      units: [
        {
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
      userId: '20000000-0000-0000-0000-000000000003',
      firstNames: 'Carolina',
      lastNames: 'Restrepo',
      email: 'carolina@loscerezos.test',
      role: 'OWNER',
      userStatus: 'ACTIVE',
      membershipStatus: 'ACTIVE',
      units: [
        {
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
      userId: '20000000-0000-0000-0000-000000000004',
      firstNames: 'Ana',
      lastNames: 'Sin Unidad',
      email: 'ana@loscerezos.test',
      role: 'OWNER',
      userStatus: 'ACTIVE',
      membershipStatus: 'ACTIVE',
      units: []
    }
  ];

  async function createComponent(
    assignedProperties: AssignedProperty[],
    membersResult: Observable<PropertyMember[]>
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
            getPropertyMembers: () => membersResult
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

  afterEach(() => TestBed.resetTestingModule());

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
});
