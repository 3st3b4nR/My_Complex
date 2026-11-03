import {
  AsyncPipe,
  NgOptimizedImage
} from '@angular/common';

import {
  Component,
  inject,
  OnInit
} from '@angular/core';

import { MatButton } from '@angular/material/button';
import { AuthService } from '@auth0/auth0-angular';
import { Router } from '@angular/router';
import { filter, take } from 'rxjs';

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
export class LoginPage implements OnInit {

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly isLoading$ = this.authService.isLoading$;
  protected readonly error$ = this.authService.error$;
  protected readonly isAuthenticated$ =
    this.authService.isAuthenticated$;

  ngOnInit(): void {
    this.isAuthenticated$.pipe(
      filter(Boolean),
      take(1)
    ).subscribe(() => {
      void this.router.navigateByUrl('/select-property');
    });
  }

  protected login(): void {
    this.authService.loginWithRedirect({
      appState: {
        target: '/select-property'
      }
    }).subscribe();
  }
}
