package com.mycomplex.news.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.mycomplex.news.application.model.NewsPage;
import com.mycomplex.news.domain.model.News;

public interface LoadNewsPort {

    NewsPage loadActiveByPropertyId(
            UUID propertyId,
            int page,
            int size
    );

    Optional<News> loadActiveByIdAndPropertyId(
            UUID newsId,
            UUID propertyId
    );
}
