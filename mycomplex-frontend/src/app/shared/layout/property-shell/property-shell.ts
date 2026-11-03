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

import { PushNotificationsService } from '../../notifications/data-access/push-notifications.service';
import { PushNotificationButton } from '../../notifications/ui/push-notification-button/push-notification-button';

export type PropertySection =
  | 'dashboard'
  | 'news'
  | 'meetings';

@Component({
  selector: 'app-property-shell',
  imports: [
    AsyncPipe,
    NgOptimizedImage,
    PushNotificationButton,
    RouterLink
  ],
  templateUrl: './property-shell.html',
  styleUrl: './property-shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PropertyShell {

  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly pushNotifications = inject(
    PushNotificationsService
  );

  readonly propertyId = input.required<string>();
  readonly propertyName = input.required<string>();
  readonly propertyCity = input.required<string>();
  readonly activeSection = input.required<PropertySection>();

  protected readonly user$ = this.authService.user$;

  protected changeProperty(): void {
    this.router.navigate(['/select-property']);
  }

  protected async logout(): Promise<void> {
    await this.pushNotifications.deactivate();
    this.authService.logout({
      logoutParams: {
        returnTo: window.location.origin
      }
    }).subscribe();
  }
}
