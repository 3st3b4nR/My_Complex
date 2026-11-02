import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { filter, take } from 'rxjs';

import { PushNotificationsService } from './shared/notifications/data-access/push-notifications.service';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App implements OnInit {

  private readonly authService = inject(AuthService);
  private readonly pushNotifications = inject(
    PushNotificationsService
  );

  ngOnInit(): void {
    this.authService.isAuthenticated$.pipe(
      filter(Boolean),
      take(1)
    ).subscribe(() => {
      void this.pushNotifications.synchronize();
    });
  }
}
