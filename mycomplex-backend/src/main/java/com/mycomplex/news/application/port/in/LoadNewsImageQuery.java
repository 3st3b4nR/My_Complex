package com.mycomplex.news.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record LoadNewsImageQuery(
        UUID propertyId,
        UUID newsId,
        String auth0Subject
) {

    public LoadNewsImageQuery {
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

        auth0Subject = auth0Subject.trim();
    }
}
