package com.mycomplex.news.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record News(
        UUID id,
        UUID propertyId,
        UUID authorId,
        UUID updatedById,
        String title,
        String content,
        NewsImage image,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        long version
) {

    public News {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(
                propertyId,
                "propertyId must not be null"
        );
        Objects.requireNonNull(
                authorId,
                "authorId must not be null"
        );
        Objects.requireNonNull(
                updatedById,
                "updatedById must not be null"
        );
        Objects.requireNonNull(
                publishedAt,
                "publishedAt must not be null"
        );
        Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );
        Objects.requireNonNull(
                updatedAt,
                "updatedAt must not be null"
        );

        title = requireText(title, "title");
        content = requireText(content, "content");

        if (title.length() > 180) {
            throw new IllegalArgumentException(
                    "title must not exceed 180 characters"
            );
        }

        if (version < 0) {
            throw new IllegalArgumentException(
                    "version must not be negative"
            );
        }
    }

    public static News publish(
            UUID id,
            UUID propertyId,
            UUID authorId,
            String title,
            String content,
            NewsImage image,
            Instant publicationTime
    ) {
        return new News(
                id,
                propertyId,
                authorId,
                authorId,
                title,
                content,
                image,
                publicationTime,
                publicationTime,
                publicationTime,
                null,
                0
        );
    }

    public News update(
            String newTitle,
            String newContent,
            NewsImage newImage,
            UUID editorId,
            Instant updateTime
    ) {
        ensureNotDeleted();

        return new News(
                id,
                propertyId,
                authorId,
                editorId,
                newTitle,
                newContent,
                newImage,
                publishedAt,
                createdAt,
                updateTime,
                null,
                version
        );
    }

    public News delete(
            UUID editorId,
            Instant deletionTime
    ) {
        ensureNotDeleted();

        return new News(
                id,
                propertyId,
                authorId,
                editorId,
                title,
                content,
                image,
                publishedAt,
                createdAt,
                deletionTime,
                deletionTime,
                version
        );
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new IllegalStateException(
                    "deleted news cannot be modified"
            );
        }
    }

    private static String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        return value.trim();
    }
}