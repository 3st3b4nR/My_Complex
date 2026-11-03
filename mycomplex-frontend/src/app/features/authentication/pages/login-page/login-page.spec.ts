import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  convertToParamMap,
  Router
} from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { BehaviorSubject, of } from 'rxjs';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  let component: LoginPage;
  let fixture: ComponentFixture<LoginPage>;
  let authenticated: BehaviorSubject<boolean>;
  let navigateByUrl: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    authenticated = new BehaviorSubject(false);
    navigateByUrl = vi.fn().mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        {
          provide: AuthService,
          useValue: {
            isLoading$: of(false),
            error$: of(null),
            isAuthenticated$: authenticated,
            loginWithRedirect: vi.fn(() => of(undefined))
          }
        },
        {
          provide: Router,
          useValue: { navigateByUrl }
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({})
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('redirects an already authenticated visitor to property selection', () => {
    authenticated.next(true);

    expect(navigateByUrl).toHaveBeenCalledWith('/select-property');
  });

  it('redirects an authenticated Auth0 callback to property selection', async () => {
    TestBed.resetTestingModule();
    authenticated = new BehaviorSubject(false);
    navigateByUrl = vi.fn().mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        {
          provide: AuthService,
          useValue: {
            isLoading$: of(false),
            error$: of(null),
            isAuthenticated$: authenticated,
            loginWithRedirect: vi.fn(() => of(undefined))
          }
        },
        {
          provide: Router,
          useValue: { navigateByUrl }
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({
                code: 'authorization-code',
                state: 'auth0-state'
              })
            }
          }
        }
      ]
    }).compileComponents();

    const callbackFixture = TestBed.createComponent(LoginPage);
    callbackFixture.detectChanges();
    authenticated.next(true);

    expect(navigateByUrl).toHaveBeenCalledWith('/select-property');
  });
});
