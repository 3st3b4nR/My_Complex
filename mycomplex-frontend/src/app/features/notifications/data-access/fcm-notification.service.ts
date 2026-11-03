import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { initializeApp, getApps } from 'firebase/app';
import {
  getMessaging,
  isSupported,
  onRegistered,
  onUnregistered,
  register,
  unregister
} from 'firebase/messaging';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../../environments/environment';

type NotificationState =
  | 'checking'
  | 'available'
  | 'activating'
  | 'enabled'
  | 'blocked'
  | 'unsupported'
  | 'disabled'
  | 'error';

interface FirebaseWebConfig {
  enabled: boolean;
  apiKey: string;
  authDomain: string;
  projectId: string;
  storageBucket: string;
  messagingSenderId: string;
  appId: string;
  vapidKey: string;
}

interface InstallationResponse { id: string; status: string; }

@Injectable({ providedIn: 'root' })
export class FcmNotificationService {
  private static readonly STORAGE_KEY = 'mycomplex.fcmInstallationId';
  private readonly http = inject(HttpClient);
  readonly state = signal<NotificationState>('checking');

  constructor() {
    this.checkAvailability();
  }

  async activate(): Promise<void> {
    if (this.state() === 'activating') return;
    this.state.set('activating');
    try {
      if (Notification.permission === 'default') {
        await Notification.requestPermission();
      }
      if (Notification.permission !== 'granted') {
        this.state.set('blocked');
        return;
      }
      const config = await this.configuration();
      if (!config.enabled) {
        this.state.set('disabled');
        return;
      }
      const app = getApps().find(item => item.name === 'mycomplex-fcm')
        ?? initializeApp(config, 'mycomplex-fcm');
      const messaging = getMessaging(app);
      const worker = await navigator.serviceWorker.register(
        '/firebase-messaging-sw.js'
      );
      const readyWorker = await navigator.serviceWorker.ready;
      readyWorker.active?.postMessage({
        type: 'MYCOMPLEX_FIREBASE_CONFIG_ENDPOINT',
        apiUrl: environment.apiUrl
      });
      onRegistered(messaging, async installationIdentifier => {
        try {
          const response = await firstValueFrom(
            this.http.post<InstallationResponse>(
              `${environment.apiUrl}/api/me/fcm-installations`,
              { installationId: installationIdentifier }
            )
          );
          localStorage.setItem(FcmNotificationService.STORAGE_KEY, response.id);
          this.state.set('enabled');
        } catch {
          this.state.set('error');
        }
      });
      onUnregistered(messaging, () => {
        localStorage.removeItem(FcmNotificationService.STORAGE_KEY);
        this.state.set('available');
      });
      await register(messaging, {
        vapidKey: config.vapidKey,
        serviceWorkerRegistration: worker
      });
    } catch {
      this.state.set('error');
    }
  }

  async deactivate(): Promise<void> {
    const id = localStorage.getItem(FcmNotificationService.STORAGE_KEY);
    let cleanupFailed = false;

    try {
      if (id) {
        await firstValueFrom(this.http.delete<void>(
          `${environment.apiUrl}/api/me/fcm-installations/${id}`
        ));
      }
    } catch {
      cleanupFailed = true;
    }

    try {
      const app = getApps().find(item => item.name === 'mycomplex-fcm');
      if (app) await unregister(getMessaging(app));
    } catch {
      cleanupFailed = true;
    } finally {
      localStorage.removeItem(FcmNotificationService.STORAGE_KEY);
      this.state.set(cleanupFailed ? 'error' : 'available');
    }
  }

  private async checkAvailability(): Promise<void> {
    if (!('Notification' in window) || !('serviceWorker' in navigator)
      || !(await isSupported())) {
      this.state.set('unsupported');
      return;
    }
    const config = await this.configuration().catch(() => null);
    if (!config?.enabled) {
      this.state.set('disabled');
      return;
    }
    if (Notification.permission === 'denied') {
      this.state.set('blocked');
    } else if (localStorage.getItem(FcmNotificationService.STORAGE_KEY)) {
      this.state.set('enabled');
    } else {
      this.state.set('available');
    }
  }

  private configuration(): Promise<FirebaseWebConfig> {
    return firstValueFrom(this.http.get<FirebaseWebConfig>(
      `${environment.apiUrl}/api/public/firebase-config`
    ));
  }
}
