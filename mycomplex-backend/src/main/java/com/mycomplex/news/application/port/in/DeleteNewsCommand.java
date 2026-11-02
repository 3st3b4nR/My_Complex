package com.mycomplex.news.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record DeleteNewsCommand(
        UUID propertyId,
        UUID newsId,
        String auth0Subject,
        long version
) {

    public DeleteNewsCommand {
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

        if (version < 0) {
            throw new IllegalArgumentException(
                    "version must not be negative"
            );
        }

        auth0Subject = auth0Subject.trim();
    }
}
