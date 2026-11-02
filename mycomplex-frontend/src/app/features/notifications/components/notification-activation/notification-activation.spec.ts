import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FcmNotificationService } from '../../data-access/fcm-notification.service';
import { NotificationActivation } from './notification-activation';

describe('NotificationActivation', () => {
  it('requests activation only after the explicit user action', async () => {
    const service = {
      state: signal('available'),
      activate: vi.fn(),
      deactivate: vi.fn()
    };
    await TestBed.configureTestingModule({
      imports: [NotificationActivation],
      providers: [{ provide: FcmNotificationService, useValue: service }]
    }).compileComponents();
    const fixture: ComponentFixture<NotificationActivation> =
      TestBed.createComponent(NotificationActivation);
    fixture.detectChanges();

    expect(service.activate).not.toHaveBeenCalled();
    const button: HTMLButtonElement =
      fixture.nativeElement.querySelector('button');
    button.click();

    expect(service.activate).toHaveBeenCalledOnce();
  });

  it('offers deactivation and explains blocked or disabled states', async () => {
    const state = signal('enabled');
    const service = {
      state,
      activate: vi.fn(),
      deactivate: vi.fn()
    };
    await TestBed.configureTestingModule({
      imports: [NotificationActivation],
      providers: [{ provide: FcmNotificationService, useValue: service }]
    }).compileComponents();
    const fixture = TestBed.createComponent(NotificationActivation);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Las notificaciones están activadas'
    );
    fixture.nativeElement.querySelector('button').click();
    expect(service.deactivate).toHaveBeenCalledOnce();

    state.set('blocked');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'El navegador bloqueó el permiso'
    );

    state.set('disabled');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'aún no están configuradas'
    );
  });
});
