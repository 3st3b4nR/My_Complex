import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FcmNotificationService } from '../../data-access/fcm-notification.service';

@Component({
  selector: 'app-notification-activation',
  templateUrl: './notification-activation.html',
  styleUrl: './notification-activation.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationActivation {
  protected readonly notifications = inject(FcmNotificationService);
}
