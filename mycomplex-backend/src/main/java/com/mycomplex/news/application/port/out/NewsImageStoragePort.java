package com.mycomplex.news.application.port.out;

import java.util.UUID;

import com.mycomplex.news.domain.model.NewsImage;

public interface NewsImageStoragePort {

    NewsImage store(
            UUID propertyId,
            UUID newsId,
            String originalFilename,
            String contentType,
            byte[] content
    );

    void delete(NewsImage image);
}