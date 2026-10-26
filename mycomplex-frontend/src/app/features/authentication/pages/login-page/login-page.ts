import {
  AsyncPipe,
  NgOptimizedImage
} from '@angular/common';

import {
  Component,
  inject
} from '@angular/core';

import { MatButton } from '@angular/material/button';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  imports: [
    AsyncPipe,
    NgOptimizedImage,
    MatButton
  ],
  selector: 'app-login-page',
  styleUrl: './login-page.scss',
  templateUrl: './login-page.html',
})
export class LoginPage {

  private readonly authService = inject(AuthService);

  protected readonly isLoading$ = this.authService.isLoading$;
  protected readonly error$ = this.authService.error$;
  protected readonly isAuthenticated$ =
    this.authService.isAuthenticated$;

  protected login(): void {
    this.authService.loginWithRedirect({
      appState: {
        target: '/select-property'
      }
    }).subscribe();
}
}