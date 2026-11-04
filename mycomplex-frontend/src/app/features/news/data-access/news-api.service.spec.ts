import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { NewsApiService } from './news-api.service';

describe('NewsApiService', () => {
  let service: NewsApiService;
  let http: HttpTestingController;
  const propertyId = 'property-1';
  const baseUrl = `${environment.apiUrl}/api/properties/${propertyId}/news`;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      NewsApiService,
      provideHttpClient(),
      provideHttpClientTesting()
    ] });
    service = TestBed.inject(NewsApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists a property page with pagination parameters', () => {
    service.list(propertyId, 2, 15).subscribe(page =>
      expect(page.totalPages).toBe(3));

    const request = http.expectOne(candidate => candidate.url === baseUrl);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('15');
    request.flush({ items: [], page: 2, size: 15,
      totalElements: 31, totalPages: 3 });
  });

  it('creates and updates multipart news in the selected property', () => {
    const image = new File(['image'], 'notice.png', { type: 'image/png' });

    service.create(propertyId, { title: 'Aviso', content: 'Contenido' }, image)
      .subscribe();
    const create = http.expectOne(baseUrl);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toBeInstanceOf(FormData);
    const uploadedImage = (create.request.body as FormData).get('image') as File;
    expect(uploadedImage.name).toBe('notice.png');
    expect(uploadedImage.type).toBe('image/png');
    expect((create.request.body as FormData).get('data')).toBeInstanceOf(Blob);
    create.flush({});

    service.update(propertyId, 'news-1', {
      title: 'Actualizado', content: 'Nuevo', version: 4
    }, null).subscribe();
    const update = http.expectOne(`${baseUrl}/news-1`);
    expect(update.request.method).toBe('PUT');
    expect((update.request.body as FormData).has('image')).toBe(false);
    update.flush({});
  });

  it('deletes with the optimistic version and loads an image as a blob', () => {
    service.delete(propertyId, 'news-1', 3).subscribe();
    const deletion = http.expectOne(candidate =>
      candidate.url === `${baseUrl}/news-1`);
    expect(deletion.request.method).toBe('DELETE');
    expect(deletion.request.params.get('version')).toBe('3');
    deletion.flush(null);

    service.loadImage(propertyId, 'news-1').subscribe(image =>
      expect(image.type).toBe('image/png'));
    const image = http.expectOne(`${baseUrl}/news-1/image`);
    expect(image.request.method).toBe('GET');
    expect(image.request.responseType).toBe('blob');
    image.flush(new Blob(['image'], { type: 'image/png' }));
  });
});
