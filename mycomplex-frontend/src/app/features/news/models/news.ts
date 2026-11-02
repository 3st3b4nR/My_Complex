export interface NewsItem {
  newsId: string;
  propertyId: string;
  title: string;
  content: string;
  imageAvailable: boolean;
  publishedAt: string;
  updatedAt: string;
  version: number;
}

export interface NewsPageResponse {
  items: NewsItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface NewsDraft {
  title: string;
  content: string;
}

export interface NewsUpdate extends NewsDraft {
  version: number;
}
