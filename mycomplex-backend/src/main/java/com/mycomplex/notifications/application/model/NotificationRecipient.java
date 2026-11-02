package com.mycomplex.notifications.application.model;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record NotificationRecipient(
        UUID userId,
        String email
) {

    public NotificationRecipient {
        Objects.requireNonNull(
                userId,
                "userId must not be null"
        );

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "email must not be blank"
            );
        }

        email = email.trim().toLowerCase(Locale.ROOT);
    }
}
