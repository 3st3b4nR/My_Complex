import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { of } from 'rxjs';

import { FcmNotificationService } from '../../../features/notifications/data-access/fcm-notification.service';
import { PropertyShell } from './property-shell';

describe('PropertyShell', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PropertyShell],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            user$: of({ email: 'admin@mycomplex.com' }),
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
        }
      ]
    }).compileComponents();
  });

  it('shows implemented property sections and marks the active one', () => {
    const fixture = TestBed.createComponent(PropertyShell);
    fixture.componentRef.setInput('propertyId', 'property-1');
    fixture.componentRef.setInput('propertyName', 'Los Cerezos');
    fixture.componentRef.setInput('propertyCity', 'Rionegro');
    fixture.componentRef.setInput('activeSection', 'meetings');
    fixture.componentRef.setInput('isAdministrator', true);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const navigation = element.querySelector('nav');

    expect(navigation?.textContent).toContain('Inicio');
    expect(navigation?.textContent).toContain('Noticias');
    expect(navigation?.textContent).toContain('Reuniones');
    expect(navigation?.textContent).toContain('Usuarios y residentes');
    expect(navigation?.textContent).toContain('Tareas');
    expect(navigation?.textContent).not.toContain('Comunicaciones');
    expect(navigation?.textContent).not.toContain('Copropiedad');
    expect(navigation?.textContent).not.toContain('Configuración');
    expect(navigation?.textContent).not.toContain('Visitantes');
    expect(navigation?.textContent).not.toContain('Reservas');
    expect(
      navigation?.querySelector(
        '[data-section="meetings"]'
      )?.classList.contains('navigation-item--active')
    ).toBe(true);
  });
});
