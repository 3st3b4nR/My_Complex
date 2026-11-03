import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { FcmNotificationService } from './fcm-notification.service';

const firebase = vi.hoisted(() => ({
  initializeApp: vi.fn(),
  getApps: vi.fn(),
  getMessaging: vi.fn(),
  isSupported: vi.fn(),
  onRegistered: vi.fn(),
  onUnregistered: vi.fn(),
  register: vi.fn(),
  unregister: vi.fn()
}));

vi.mock('firebase/app', () => ({
  initializeApp: firebase.initializeApp,
  getApps: firebase.getApps
}));

vi.mock('firebase/messaging', () => ({
  getMessaging: firebase.getMessaging,
  isSupported: firebase.isSupported,
  onRegistered: firebase.onRegistered,
  onUnregistered: firebase.onUnregistered,
  register: firebase.register,
  unregister: firebase.unregister
}));

describe('FcmNotificationService', () => {
  const config = {
    enabled: true,
    apiKey: 'public-key',
    authDomain: 'project.firebaseapp.com',
    projectId: 'project',
    storageBucket: 'project.appspot.com',
    messagingSenderId: 'sender',
    appId: 'app',
    vapidKey: 'public-vapid-key'
  };
  let permission: NotificationPermission;
  let registered: ((identifier: string) => Promise<void>) | undefined;
  let unregistered: (() => void) | undefined;
  let postMessage: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    permission = 'granted';
    registered = undefined;
    unregistered = undefined;
    postMessage = vi.fn();
    Object.defineProperty(globalThis, 'Notification', {
      configurable: true,
      value: {
        get permission() { return permission; },
        requestPermission: vi.fn().mockResolvedValue('granted')
      }
    });
    Object.defineProperty(navigator, 'serviceWorker', {
      configurable: true,
      value: {
        register: vi.fn().mockResolvedValue({ scope: '/' }),
        ready: Promise.resolve({ active: { postMessage } })
      }
    });
    firebase.initializeApp.mockReturnValue({ name: 'mycomplex-fcm' });
    firebase.getApps.mockReturnValue([]);
    firebase.getMessaging.mockReturnValue({});
    firebase.isSupported.mockResolvedValue(true);
    firebase.register.mockResolvedValue(undefined);
    firebase.unregister.mockResolvedValue(undefined);
    firebase.onRegistered.mockImplementation((_messaging, callback) => {
      registered = callback;
    });
    firebase.onUnregistered.mockImplementation((_messaging, callback) => {
      unregistered = callback;
    });
    TestBed.configureTestingModule({ providers: [
      FcmNotificationService,
      provideHttpClient(),
      provideHttpClientTesting()
    ] });
  });

  async function createService(webConfig = config) {
    const service = TestBed.inject(FcmNotificationService);
    const http = TestBed.inject(HttpTestingController);
    let configurationRequest: ReturnType<HttpTestingController['expectOne']>;
    await vi.waitFor(() => {
      configurationRequest = http.expectOne(
        `${environment.apiUrl}/api/public/firebase-config`
      );
    });
    configurationRequest!.flush(webConfig);
    await Promise.resolve();
    return { service, http };
  }

  it('registers a Firebase installation after explicit activation', async () => {
    const { service, http } = await createService();
    expect(service.state()).toBe('available');

    const activation = service.activate();
    await Promise.resolve();
    http.expectOne(`${environment.apiUrl}/api/public/firebase-config`)
      .flush(config);
    await activation;

    expect(firebase.register).toHaveBeenCalledWith({}, expect.objectContaining({
      vapidKey: 'public-vapid-key'
    }));
    expect(postMessage).toHaveBeenCalledWith({
      type: 'MYCOMPLEX_FIREBASE_CONFIG_ENDPOINT',
      apiUrl: environment.apiUrl
    });

    const registration = registered!('firebase-installation-id');
    const request = http.expectOne(
      `${environment.apiUrl}/api/me/fcm-installations`
    );
    expect(request.request.body).toEqual({
      installationId: 'firebase-installation-id'
    });
    request.flush({ id: 'local-installation-id', status: 'ACTIVE' });
    await registration;
    expect(service.state()).toBe('enabled');
    expect(localStorage.getItem('mycomplex.fcmInstallationId'))
      .toBe('local-installation-id');
    http.verify();
  });

  it('reports denied permission without registering Firebase', async () => {
    permission = 'denied';
    const { service, http } = await createService();

    expect(service.state()).toBe('blocked');
    await service.activate();
    expect(service.state()).toBe('blocked');
    expect(firebase.register).not.toHaveBeenCalled();
    http.verify();
  });

  it('handles disabled and unsupported Firebase configurations', async () => {
    const disabled = await createService({ ...config, enabled: false });
    expect(disabled.service.state()).toBe('disabled');
    disabled.http.verify();

    TestBed.resetTestingModule();
    firebase.isSupported.mockResolvedValue(false);
    TestBed.configureTestingModule({ providers: [
      FcmNotificationService,
      provideHttpClient(),
      provideHttpClientTesting()
    ] });
    const unsupported = TestBed.inject(FcmNotificationService);
    await Promise.resolve();
    expect(unsupported.state()).toBe('unsupported');
    TestBed.inject(HttpTestingController).verify();
  });

  it('deactivates both the local and Firebase installations', async () => {
    const { service, http } = await createService();
    localStorage.setItem('mycomplex.fcmInstallationId', 'local-id');
    firebase.getApps.mockReturnValue([{ name: 'mycomplex-fcm' }]);

    const deactivation = service.deactivate();
    http.expectOne(`${environment.apiUrl}/api/me/fcm-installations/local-id`)
      .flush(null);
    await deactivation;

    expect(firebase.unregister).toHaveBeenCalled();
    expect(localStorage.getItem('mycomplex.fcmInstallationId')).toBeNull();
    expect(service.state()).toBe('available');
    http.verify();
  });

  it('uses controlled error states for HTTP failures', async () => {
    const { service, http } = await createService();
    localStorage.setItem('mycomplex.fcmInstallationId', 'local-id');
    firebase.getApps.mockReturnValue([{ name: 'mycomplex-fcm' }]);

    const deactivation = service.deactivate();
    http.expectOne(`${environment.apiUrl}/api/me/fcm-installations/local-id`)
      .flush({}, { status: 500, statusText: 'Server Error' });
    await deactivation;

    expect(service.state()).toBe('error');
    expect(firebase.unregister).toHaveBeenCalled();
    expect(localStorage.getItem('mycomplex.fcmInstallationId')).toBeNull();
    http.verify();
  });

  it('handles registration persistence errors and unregister callbacks',
    async () => {
      const { service, http } = await createService();
      const activation = service.activate();
      await Promise.resolve();
      http.expectOne(`${environment.apiUrl}/api/public/firebase-config`)
        .flush(config);
      await activation;

      const registration = registered!('invalid-installation-id');
      http.expectOne(`${environment.apiUrl}/api/me/fcm-installations`)
        .flush({}, { status: 500, statusText: 'Server Error' });
      await registration;
      expect(service.state()).toBe('error');

      localStorage.setItem('mycomplex.fcmInstallationId', 'local-id');
      unregistered!();
      expect(service.state()).toBe('available');
      expect(localStorage.getItem('mycomplex.fcmInstallationId')).toBeNull();
      http.verify();
    });
});
