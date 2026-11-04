import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter
} from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { of } from 'rxjs';

import { PropertyApiService } from '../../../property-selection/data-access/property-api.service';
import { PropertyRole } from '../../../property-selection/models/assigned-property';
import { FcmNotificationService } from '../../../notifications/data-access/fcm-notification.service';
import { PropertyDashboardPage } from './property-dashboard-page';

describe('PropertyDashboardPage', () => {
  const propertyId = '10000000-0000-0000-0000-000000000001';

  async function createComponent(
    role: PropertyRole
  ): Promise<ComponentFixture<PropertyDashboardPage>> {
    await TestBed.configureTestingModule({
      imports: [PropertyDashboardPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ propertyId }))
          }
        },
        {
          provide: AuthService,
          useValue: {
            user$: of({ name: 'Test administrator' }),
            logout: () => of(undefined)
          }
        },
        {
          provide: FcmNotificationService,
          useValue: {
            state: () => 'enabled',
            activate: vi.fn(),
            deactivate: vi.fn()
          }
        },
        {
          provide: PropertyApiService,
          useValue: {
            getMyProperties: () => of([{
              propertyId,
              name: 'Los Cerezos',
              city: 'Rionegro',
              role
            }])
          }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(PropertyDashboardPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => TestBed.resetTestingModule());

  it('links both administrator controls using the current property id', async () => {
    const fixture = await createComponent('PROPERTY_ADMIN');
    const links = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('a')
    ).filter(link =>
      link.textContent?.includes('Usuarios y residentes')
    );

    expect(links.length).toBe(2);
    links.forEach(link => {
      expect(link.getAttribute('href')).toBe(
        `/properties/${propertyId}/users`
      );
    });

    const moduleTitles = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll(
        '.module-card h3'
      )
    ).map(title => title.textContent?.trim());

    expect(moduleTitles).toEqual([
      'Usuarios y residentes',
      'Tareas',
      'Noticias',
      'Reuniones'
    ]);
    expect(fixture.nativeElement.textContent).not.toContain('Visitantes');
    expect(fixture.nativeElement.textContent).not.toContain('Reservas');
    expect(moduleTitles).not.toContain('Copropiedad');
    expect(moduleTitles).not.toContain('Configuración');
  });

  it('hides user management for a non-administrator role', async () => {
    const fixture = await createComponent('RESIDENT');

    expect(fixture.nativeElement.textContent).not.toContain(
      'Usuarios y residentes'
    );
  });
});
