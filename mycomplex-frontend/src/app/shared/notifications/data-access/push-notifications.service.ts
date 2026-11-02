import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';

import {
  deleteToken,
  getMessaging,
  getToken,
  isSupported,
  Messaging,
  onMessage
} from 'firebase/messaging';

import {
  getApp,
  getApps,
  initializeApp
} from 'firebase/app';

import { firstValueFrom } from 'rxjs';

import { environment } from '../../../../environments/environment';

export type PushNotificationState =
  | 'unavailable'
  | 'inactive'
  | 'enabling'
  | 'enabled'
  | 'denied'
  | 'error';

@Injectable({
  providedIn: 'root'
})
export class PushNotificationsService {

  private static readonly STORAGE_KEY =
    'mycomplex.firebase.installation-id';

  private readonly http = inject(HttpClient);
  private foregroundListenerRegistered = false;

  readonly state = signal<PushNotificationState>(
    this.initialState()
  );

  async synchronize(): Promise<void> {
    if (!this.isBrowserSupported()) {
      this.state.set('unavailable');
      return;
    }

    if (Notification.permission !== 'granted') {
      this.state.set(
        Notification.permission === 'denied'
          ? 'denied'
          : 'inactive'
      );
      return;
    }

    await this.registerCurrentBrowser();
  }

  async enable(): Promise<void> {
    if (!this.isBrowserSupported()) {
      this.state.set('unavailable');
      return;
    }

    this.state.set('enabling');

    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      this.state.set(
        permission === 'denied' ? 'denied' : 'inactive'
      );
      return;
    }

    await this.registerCurrentBrowser();
  }

  async deactivate(): Promise<void> {
    const installationId = localStorage.getItem(
      PushNotificationsService.STORAGE_KEY
    );

    if (!installationId) {
      return;
    }

    try {
      await firstValueFrom(this.http.delete<void>(
        `${environment.apiUrl}/api/me/push-installations`,
        { body: { installationId } }
      ));

      const messaging = await this.messaging();
      if (messaging) {
        await deleteToken(messaging);
      }
    } catch {
      // Closing the local session must not be blocked by push cleanup.
    } finally {
      localStorage.removeItem(PushNotificationsService.STORAGE_KEY);
      this.state.set('inactive');
    }
  }

  private async registerCurrentBrowser(): Promise<void> {
    this.state.set('enabling');

    try {
      const messaging = await this.messaging();
      if (!messaging) {
        this.state.set('unavailable');
        return;
      }

      const serviceWorkerRegistration =
        await navigator.serviceWorker.register(
          '/firebase-messaging-sw.js'
        );

      const installationId = await getToken(messaging, {
        vapidKey: environment.firebase.vapidKey,
        serviceWorkerRegistration
      });

      if (!installationId) {
        throw new Error('Firebase did not return an installation ID');
      }

      await firstValueFrom(this.http.post<void>(
        `${environment.apiUrl}/api/me/push-installations`,
        { installationId }
      ));

      localStorage.setItem(
        PushNotificationsService.STORAGE_KEY,
        installationId
      );

      this.registerForegroundListener(messaging);
      this.state.set('enabled');
    } catch {
      this.state.set('error');
    }
  }

  private async messaging(): Promise<Messaging | null> {
    if (!(await isSupported())) {
      return null;
    }

    const app = getApps().length > 0
      ? getApp()
      : initializeApp(environment.firebase);

    return getMessaging(app);
  }

  private registerForegroundListener(messaging: Messaging): void {
    if (this.foregroundListenerRegistered) {
      return;
    }

    onMessage(messaging, payload => {
      const title = payload.notification?.title ?? 'MyComplex';
      const body = payload.notification?.body ??
        'Tienes una nueva notificación.';

      if (Notification.permission === 'granted') {
        new Notification(title, { body });
      }
    });

    this.foregroundListenerRegistered = true;
  }

  private initialState(): PushNotificationState {
    if (!this.isBrowserSupported()) {
      return 'unavailable';
    }

    return Notification.permission === 'granted'
      ? 'inactive'
      : Notification.permission === 'denied'
        ? 'denied'
        : 'inactive';
  }

  private isBrowserSupported(): boolean {
    return typeof window !== 'undefined'
      && 'Notification' in window
      && 'serviceWorker' in navigator;
  }
}
