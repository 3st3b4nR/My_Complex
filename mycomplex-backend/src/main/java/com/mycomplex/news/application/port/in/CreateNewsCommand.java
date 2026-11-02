package com.mycomplex.news.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record CreateNewsCommand(
        UUID propertyId,
        String auth0Subject,
        String title,
        String content,
        NewsImageUpload image
) {

    public CreateNewsCommand {
        Objects.requireNonNull(
                propertyId,
                "propertyId must not be null"
        );

        auth0Subject = requireText(
                auth0Subject,
                "auth0Subject"
        );

        title = requireText(title, "title");
        content = requireText(content, "content");
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