import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';

import {
  NewsDraft,
  NewsItem,
  NewsPageResponse,
  NewsUpdate
} from '../models/news';

@Injectable({
  providedIn: 'root'
})
export class NewsApiService {

  private readonly http = inject(HttpClient);

  list(
    propertyId: string,
    page = 0,
    size = 20
  ): Observable<NewsPageResponse> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<NewsPageResponse>(
      this.baseUrl(propertyId),
      { params }
    );
  }

  create(
    propertyId: string,
    draft: NewsDraft,
    image: File | null
  ): Observable<NewsItem> {
    return this.http.post<NewsItem>(
      this.baseUrl(propertyId),
      this.toFormData(draft, image)
    );
  }

  update(
    propertyId: string,
    newsId: string,
    update: NewsUpdate,
    image: File | null
  ): Observable<NewsItem> {
    return this.http.put<NewsItem>(
      `${this.baseUrl(propertyId)}/${newsId}`,
      this.toFormData(update, image)
    );
  }

  delete(
    propertyId: string,
    newsId: string,
    version: number
  ): Observable<void> {
    const params = new HttpParams().set('version', version);

    return this.http.delete<void>(
      `${this.baseUrl(propertyId)}/${newsId}`,
      { params }
    );
  }

  loadImage(
    propertyId: string,
    newsId: string
  ): Observable<Blob> {
    return this.http.get(
      `${this.baseUrl(propertyId)}/${newsId}/image`,
      { responseType: 'blob' }
    );
  }

  private baseUrl(propertyId: string): string {
    return `${environment.apiUrl}/api/properties/${propertyId}/news`;
  }

  private toFormData(
    data: NewsDraft | NewsUpdate,
    image: File | null
  ): FormData {
    const formData = new FormData();

    formData.append(
      'data',
      new Blob(
        [JSON.stringify(data)],
        { type: 'application/json' }
      )
    );

    if (image) {
      formData.append('image', image, image.name);
    }

    return formData;
  }
}
