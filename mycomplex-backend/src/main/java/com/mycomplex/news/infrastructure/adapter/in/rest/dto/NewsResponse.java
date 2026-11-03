package com.mycomplex.news.infrastructure.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record NewsResponse(
        UUID newsId,
        UUID propertyId,
        String title,
        String content,
        boolean imageAvailable,
        Instant publishedAt,
        Instant updatedAt,
        long version
) {
}