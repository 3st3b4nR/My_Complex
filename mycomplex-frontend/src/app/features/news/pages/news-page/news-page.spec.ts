import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { PropertyApiService }
  from '../../../property-selection/data-access/property-api.service';
import { NewsApiService } from '../../data-access/news-api.service';
import { NewsItem } from '../../models/news';
import { NewsPage } from './news-page';

const PROPERTY = {
  propertyId: 'property-1', name: 'Los Cerezos', city: 'Rionegro',
  role: 'PROPERTY_ADMIN' as const
};
const NEWS: NewsItem = {
  newsId: 'news-1', propertyId: 'property-1', title: 'Corte de agua',
  content: 'Mantenimiento programado', imageAvailable: false,
  publishedAt: '2026-11-01T10:00:00Z',
  updatedAt: '2026-11-01T10:00:00Z', version: 0
};
const PAGE = {
  items: [NEWS], page: 0, size: 20, totalElements: 1, totalPages: 2
};

describe('NewsPage', () => {
  async function createPage(options: {
    propertyId?: string | null;
    role?: 'PROPERTY_ADMIN' | 'RESIDENT';
    properties?: unknown[];
    list?: unknown;
    api?: Record<string, unknown>;
  } = {}) {
    const router = { navigate: vi.fn() };
    const propertyApi = {
      getMyProperties: vi.fn().mockReturnValue(of(
        options.properties ?? [{ ...PROPERTY, role: options.role ?? PROPERTY.role }]
      ))
    };
    const api = {
      list: vi.fn().mockReturnValue(options.list ?? of(PAGE)),
      create: vi.fn().mockReturnValue(of(NEWS)),
      update: vi.fn().mockReturnValue(of({ ...NEWS, title: 'Actualizada', version: 1 })),
      delete: vi.fn().mockReturnValue(of(undefined)),
      loadImage: vi.fn().mockReturnValue(of(new Blob(['image']))),
      ...options.api
    };
    await TestBed.configureTestingModule({
      imports: [NewsPage],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: {
          get: () => options.propertyId === undefined
            ? 'property-1' : options.propertyId
        } } } },
        { provide: Router, useValue: router },
        { provide: PropertyApiService, useValue: propertyApi },
        { provide: NewsApiService, useValue: api }
      ]
    }).overrideComponent(NewsPage, { set: { template: '' } })
      .compileComponents();
    const fixture: ComponentFixture<NewsPage> = TestBed.createComponent(NewsPage);
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance as any,
      api, router, propertyApi };
  }

  beforeEach(() => {
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true, value: vi.fn(() => 'blob:news-image')
    });
    Object.defineProperty(URL, 'revokeObjectURL', {
      configurable: true, value: vi.fn()
    });
    Object.defineProperty(window, 'scrollTo', {
      configurable: true, value: vi.fn()
    });
  });

  it('loads the property, news, pagination and available images', async () => {
    const pictured = { ...NEWS, imageAvailable: true };
    const { component, api } = await createPage({
      list: of({ ...PAGE, items: [pictured] })
    });

    expect(component.loading()).toBe(false);
    expect(component.property()).toEqual(PROPERTY);
    expect(component.news()).toEqual([pictured]);
    expect(component.canLoadMore()).toBe(true);
    expect(api.list).toHaveBeenCalledWith('property-1');
    expect(api.loadImage).toHaveBeenCalledWith('property-1', 'news-1');
    expect(component.imageUrl('news-1')).toBe('blob:news-image');
    expect(component.imageUrl('missing')).toBeNull();
  });

  it('redirects without propertyId and reports an unavailable property', async () => {
    const missingId = await createPage({ propertyId: null });
    expect(missingId.router.navigate).toHaveBeenCalledWith(['/select-property']);
    expect(missingId.api.list).not.toHaveBeenCalled();

    TestBed.resetTestingModule();
    const unavailable = await createPage({ properties: [] });
    expect(unavailable.component.pageError())
      .toBe('La copropiedad no está asociada con tu usuario.');
  });

  it('maps authorization and generic load errors', async () => {
    const forbidden = await createPage({
      list: throwError(() => new HttpErrorResponse({ status: 403 }))
    });
    expect(forbidden.component.pageError())
      .toBe('No tienes permisos para realizar esta acción.');

    TestBed.resetTestingModule();
    const failed = await createPage({
      list: throwError(() => new Error('network'))
    });
    expect(failed.component.pageError())
      .toBe('No fue posible completar la operación. Inténtalo nuevamente.');
  });

  it('validates permissions and required fields before creating', async () => {
    const resident = await createPage({ role: 'RESIDENT' });
    resident.component.openCreateForm();
    resident.component.submit();
    expect(resident.api.create).not.toHaveBeenCalled();

    TestBed.resetTestingModule();
    const admin = await createPage();
    admin.component.openCreateForm();
    admin.component.submit();
    expect(admin.api.create).not.toHaveBeenCalled();
    expect(admin.component.newsForm.controls.title.touched).toBe(true);

    admin.component.newsForm.setValue({ title: 'Nueva', content: 'Contenido' });
    admin.component.submit();
    expect(admin.api.create).toHaveBeenCalledWith(
      'property-1', { title: 'Nueva', content: 'Contenido' }, null
    );
    expect(admin.component.news()[0]).toEqual(NEWS);
    expect(admin.component.formVisible()).toBe(false);
    expect(admin.component.saving()).toBe(false);
  });

  it('edits news, selects an image and controls form closing', async () => {
    const { component, api } = await createPage();
    component.openEditForm(NEWS);
    expect(component.editingNews()).toEqual(NEWS);
    expect(component.newsForm.getRawValue()).toEqual({
      title: NEWS.title, content: NEWS.content
    });
    const image = new File(['image'], 'notice.png');
    component.selectImage({ target: { files: { item: () => image } } } as any);
    component.newsForm.controls.title.setValue('Actualizada');
    component.submit();
    expect(api.update).toHaveBeenCalledWith('property-1', 'news-1', {
      title: 'Actualizada', content: NEWS.content, version: 0
    }, image);
    expect(component.news()[0].title).toBe('Actualizada');

    component.openCreateForm();
    component.saving.set(true);
    component.closeForm();
    expect(component.formVisible()).toBe(true);
    component.saving.set(false);
    component.closeForm();
    expect(component.formVisible()).toBe(false);
  });

  it('keeps create conflicts controlled', async () => {
    const { component } = await createPage({ api: {
      create: vi.fn().mockReturnValue(throwError(() =>
        new HttpErrorResponse({ status: 409 })))
    } });
    component.openCreateForm();
    component.newsForm.setValue({ title: 'Nueva', content: 'Contenido' });
    component.submit();
    expect(component.formError())
      .toBe('La noticia cambió. Recarga la página e inténtalo otra vez.');
    expect(component.saving()).toBe(false);
  });

  it('confirms deletion, removes images and handles deletion failures', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const { component, api } = await createPage();
    component.deleteNews(NEWS);
    expect(api.delete).not.toHaveBeenCalled();

    confirm.mockReturnValue(true);
    component.imageUrls.set({ 'news-1': 'blob:old' });
    component.deleteNews(NEWS);
    expect(api.delete).toHaveBeenCalledWith('property-1', 'news-1', 0);
    expect(component.news()).toEqual([]);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:old');

    api.delete.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 401 })));
    component.news.set([NEWS]);
    component.deleteNews(NEWS);
    expect(component.pageError())
      .toBe('La sesión expiró. Inicia sesión nuevamente.');
  });

  it('loads subsequent pages once and reports pagination errors', async () => {
    const second = { ...NEWS, newsId: 'news-2', title: 'Segunda' };
    const { component, api } = await createPage();
    api.list.mockReturnValue(of({ ...PAGE, items: [second], page: 1 }));
    component.loadMore();
    expect(api.list).toHaveBeenLastCalledWith('property-1', 1);
    expect(component.news()).toEqual([NEWS, second]);
    expect(component.loadingMore()).toBe(false);

    component.loadMore();
    expect(api.list).toHaveBeenCalledTimes(2);
    component.currentPage.set(0);
    api.list.mockReturnValue(throwError(() => new Error('network')));
    component.loadMore();
    expect(component.pageError()).toContain('No fue posible');
  });

  it('navigates back and revokes object URLs on destroy', async () => {
    const { component, router } = await createPage();
    component.imageUrls.set({ first: 'blob:first', second: 'blob:second' });
    component.backToDashboard();
    expect(router.navigate).toHaveBeenCalledWith([
      '/properties', 'property-1', 'dashboard'
    ]);
    component.ngOnDestroy();
    expect(URL.revokeObjectURL).toHaveBeenCalledTimes(2);
  });
});
