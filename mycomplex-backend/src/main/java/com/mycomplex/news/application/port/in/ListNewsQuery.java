package com.mycomplex.news.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record ListNewsQuery(
        UUID propertyId,
        String auth0Subject,
        int page,
        int size
) {

    private static final int MAX_PAGE_SIZE = 100;

    public ListNewsQuery {
        Objects.requireNonNull(
                propertyId,
                "propertyId must not be null"
        );

        if (auth0Subject == null || auth0Subject.isBlank()) {
            throw new IllegalArgumentException(
                    "auth0Subject must not be blank"
            );
        }

        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must not be negative"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 100"
            );
        }

        auth0Subject = auth0Subject.trim();
    }
}
