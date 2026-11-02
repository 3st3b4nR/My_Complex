package com.mycomplex.news.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record UpdateNewsCommand(
        UUID propertyId,
        UUID newsId,
        String auth0Subject,
        String title,
        String content,
        long version,
        NewsImageUpload image
) {

    public UpdateNewsCommand {
        Objects.requireNonNull(
                propertyId,
                "propertyId must not be null"
        );
        Objects.requireNonNull(
                newsId,
                "newsId must not be null"
        );

        if (auth0Subject == null || auth0Subject.isBlank()) {
            throw new IllegalArgumentException(
                    "auth0Subject must not be blank"
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "title must not be blank"
            );
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "content must not be blank"
            );
        }

        if (version < 0) {
            throw new IllegalArgumentException(
                    "version must not be negative"
            );
        }

        auth0Subject = auth0Subject.trim();
        title = title.trim();
        content = content.trim();
    }
}
