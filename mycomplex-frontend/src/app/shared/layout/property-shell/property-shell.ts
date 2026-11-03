import {
  AsyncPipe,
  NgOptimizedImage
} from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input
} from '@angular/core';
import {
  Router,
  RouterLink
} from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';

import { FcmNotificationService } from '../../../features/notifications/data-access/fcm-notification.service';

export type PropertySection =
  | 'dashboard'
  | 'news'
  | 'meetings'
  | 'users'
  | 'tasks';

@Component({
  selector: 'app-property-shell',
  imports: [
    AsyncPipe,
    NgOptimizedImage,
    RouterLink
  ],
  templateUrl: './property-shell.html',
  styleUrl: './property-shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PropertyShell {

  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  protected readonly notifications = inject(
    FcmNotificationService
  );

  readonly propertyId = input.required<string>();
  readonly propertyName = input.required<string>();
  readonly propertyCity = input.required<string>();
  readonly activeSection = input.required<PropertySection>();
  readonly isAdministrator = input(false);

  protected readonly user$ = this.authService.user$;

  protected changeProperty(): void {
    this.router.navigate(['/select-property']);
  }

  protected activateNotifications(): void {
    void this.notifications.activate();
  }

  protected async logout(): Promise<void> {
    await this.notifications.deactivate();
    this.authService.logout({
      logoutParams: {
        returnTo: window.location.origin
      }
    }).subscribe();
  }
}
