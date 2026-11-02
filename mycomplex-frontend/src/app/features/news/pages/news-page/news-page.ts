import {
  DatePipe,
  NgOptimizedImage
} from '@angular/common';

import { HttpErrorResponse } from '@angular/common/http';

import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnDestroy,
  OnInit,
  signal
} from '@angular/core';

import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import {
  ActivatedRoute,
  Router,
  RouterLink
} from '@angular/router';

import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '@auth0/auth0-angular';

import {
  finalize,
  forkJoin,
  take
} from 'rxjs';

import { PropertyApiService } from '../../../property-selection/data-access/property-api.service';
import { AssignedProperty } from '../../../property-selection/models/assigned-property';
import { PushNotificationButton } from '../../../../shared/notifications/ui/push-notification-button/push-notification-button';
import { PushNotificationsService } from '../../../../shared/notifications/data-access/push-notifications.service';
import { NewsApiService } from '../../data-access/news-api.service';
import { NewsItem } from '../../models/news';

@Component({
  selector: 'app-news-page',
  imports: [
    DatePipe,
    NgOptimizedImage,
    PushNotificationButton,
    ReactiveFormsModule,
    RouterLink
  ],
  templateUrl: './news-page.html',
  styleUrl: './news-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NewsPage implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly newsApi = inject(NewsApiService);
  private readonly pushNotifications = inject(
    PushNotificationsService
  );
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly propertyId =
    this.route.snapshot.paramMap.get('propertyId') ?? '';

  protected readonly property = signal<AssignedProperty | null>(null);
  protected readonly news = signal<NewsItem[]>([]);
  protected readonly imageUrls = signal<Record<string, string>>({});
  protected readonly loading = signal(true);
  protected readonly loadingMore = signal(false);
  protected readonly saving = signal(false);
  protected readonly deletingNewsId = signal<string | null>(null);
  protected readonly pageError = signal<string | null>(null);
  protected readonly formError = signal<string | null>(null);
  protected readonly formVisible = signal(false);
  protected readonly editingNews = signal<NewsItem | null>(null);
  protected readonly selectedImage = signal<File | null>(null);
  protected readonly currentPage = signal(0);
  protected readonly totalPages = signal(0);

  protected readonly isAdministrator = computed(
    () => this.property()?.role === 'PROPERTY_ADMIN'
  );

  protected readonly canLoadMore = computed(
    () => this.currentPage() + 1 < this.totalPages()
  );

  protected readonly newsForm = this.formBuilder.nonNullable.group({
    title: ['', [
      Validators.required,
      Validators.maxLength(180)
    ]],
    content: ['', Validators.required]
  });

  ngOnInit(): void {
    if (!this.propertyId) {
      this.router.navigate(['/select-property']);
      return;
    }

    this.loadInitialData();
  }

  ngOnDestroy(): void {
    Object.values(this.imageUrls()).forEach(url =>
      URL.revokeObjectURL(url)
    );
  }

  protected openCreateForm(): void {
    this.editingNews.set(null);
    this.selectedImage.set(null);
    this.formError.set(null);
    this.newsForm.reset({ title: '', content: '' });
    this.formVisible.set(true);
  }

  protected openEditForm(item: NewsItem): void {
    this.editingNews.set(item);
    this.selectedImage.set(null);
    this.formError.set(null);
    this.newsForm.reset({
      title: item.title,
      content: item.content
    });
    this.formVisible.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formVisible.set(false);
    this.formError.set(null);
    this.selectedImage.set(null);
  }

  protected selectImage(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedImage.set(input.files?.item(0) ?? null);
  }

  protected submit(): void {
    if (!this.isAdministrator()) {
      return;
    }

    if (this.newsForm.invalid) {
      this.newsForm.markAllAsTouched();
      return;
    }

    const value = this.newsForm.getRawValue();
    const editing = this.editingNews();

    this.saving.set(true);
    this.formError.set(null);

    const request = editing
      ? this.newsApi.update(
          this.propertyId,
          editing.newsId,
          {
            ...value,
            version: editing.version
          },
          this.selectedImage()
        )
      : this.newsApi.create(
          this.propertyId,
          value,
          this.selectedImage()
        );

    request.pipe(
      take(1),
      finalize(() => this.saving.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: savedNews => {
        this.storeSavedNews(savedNews, Boolean(editing));
        this.formVisible.set(false);
        this.editingNews.set(null);
        this.selectedImage.set(null);
      },
      error: error => {
        this.formError.set(this.requestErrorMessage(error));
      }
    });
  }

  protected deleteNews(item: NewsItem): void {
    if (!this.isAdministrator()) {
      return;
    }

    const confirmed = window.confirm(
      `¿Deseas eliminar la noticia “${item.title}”?`
    );

    if (!confirmed) {
      return;
    }

    this.deletingNewsId.set(item.newsId);
    this.pageError.set(null);

    this.newsApi.delete(
      this.propertyId,
      item.newsId,
      item.version
    ).pipe(
      take(1),
      finalize(() => this.deletingNewsId.set(null)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        this.news.update(items =>
          items.filter(current => current.newsId !== item.newsId)
        );
        this.removeImageUrl(item.newsId);
      },
      error: error => {
        this.pageError.set(this.requestErrorMessage(error));
      }
    });
  }

  protected loadMore(): void {
    if (!this.canLoadMore() || this.loadingMore()) {
      return;
    }

    this.loadingMore.set(true);
    this.pageError.set(null);

    this.newsApi.list(
      this.propertyId,
      this.currentPage() + 1
    ).pipe(
      take(1),
      finalize(() => this.loadingMore.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: page => {
        this.currentPage.set(page.page);
        this.totalPages.set(page.totalPages);
        this.news.update(items => [...items, ...page.items]);
        this.loadImages(page.items);
      },
      error: error => {
        this.pageError.set(this.requestErrorMessage(error));
      }
    });
  }

  protected imageUrl(newsId: string): string | null {
    return this.imageUrls()[newsId] ?? null;
  }

  protected backToDashboard(): void {
    this.router.navigate([
      '/properties',
      this.propertyId,
      'dashboard'
    ]);
  }

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

  private loadInitialData(): void {
    this.loading.set(true);
    this.pageError.set(null);

    forkJoin({
      properties: this.propertyApi.getMyProperties(),
      newsPage: this.newsApi.list(this.propertyId)
    }).pipe(
      take(1),
      finalize(() => this.loading.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: ({ properties, newsPage }) => {
        const property = properties.find(
          current => current.propertyId === this.propertyId
        );

        if (!property) {
          this.pageError.set(
            'La copropiedad no está asociada con tu usuario.'
          );
          return;
        }

        this.property.set(property);
        this.news.set(newsPage.items);
        this.currentPage.set(newsPage.page);
        this.totalPages.set(newsPage.totalPages);
        this.loadImages(newsPage.items);
      },
      error: error => {
        this.pageError.set(this.requestErrorMessage(error));
      }
    });
  }

  private loadImages(items: NewsItem[]): void {
    items
      .filter(item => item.imageAvailable)
      .forEach(item => this.loadImage(item));
  }

  private loadImage(item: NewsItem): void {
    this.newsApi.loadImage(
      this.propertyId,
      item.newsId
    ).pipe(
      take(1),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: image => {
        this.removeImageUrl(item.newsId);
        const url = URL.createObjectURL(image);
        this.imageUrls.update(urls => ({
          ...urls,
          [item.newsId]: url
        }));
      }
    });
  }

  private storeSavedNews(
    savedNews: NewsItem,
    replacing: boolean
  ): void {
    if (replacing) {
      this.news.update(items => items.map(item =>
        item.newsId === savedNews.newsId ? savedNews : item
      ));
      this.removeImageUrl(savedNews.newsId);
    } else {
      this.news.update(items => [savedNews, ...items]);
    }

    if (savedNews.imageAvailable) {
      this.loadImage(savedNews);
    }
  }

  private removeImageUrl(newsId: string): void {
    const existingUrl = this.imageUrls()[newsId];

    if (existingUrl) {
      URL.revokeObjectURL(existingUrl);
    }

    this.imageUrls.update(urls => {
      const updatedUrls = { ...urls };
      delete updatedUrls[newsId];
      return updatedUrls;
    });
  }

  private requestErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        return 'La sesión expiró. Inicia sesión nuevamente.';
      }

      if (error.status === 403) {
        return 'No tienes permisos para realizar esta acción.';
      }

      if (error.status === 409) {
        return 'La noticia cambió. Recarga la página e inténtalo otra vez.';
      }
    }

    return 'No fue posible completar la operación. Inténtalo nuevamente.';
  }
}
