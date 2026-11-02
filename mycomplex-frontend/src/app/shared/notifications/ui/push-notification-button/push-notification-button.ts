import {
  ChangeDetectionStrategy,
  Component,
  inject
} from '@angular/core';

import { PushNotificationsService } from '../../data-access/push-notifications.service';

@Component({
  selector: 'app-push-notification-button',
  templateUrl: './push-notification-button.html',
  styleUrl: './push-notification-button.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PushNotificationButton {

  private readonly pushNotifications = inject(
    PushNotificationsService
  );

  protected readonly state = this.pushNotifications.state;

  protected enable(): void {
    void this.pushNotifications.enable();
  }
}
