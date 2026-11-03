package com.mycomplex.notifications.application.model;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record PushNotification(
        Set<UUID> recipientIds,
        String title,
        String body,
        Map<String, String> data
) {

    public PushNotification {
        recipientIds = Set.copyOf(recipientIds);
        data = Map.copyOf(data);

        if (recipientIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "push recipients must not be empty"
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "push title must not be blank"
            );
        }

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException(
                    "push body must not be blank"
            );
        }

        title = title.trim();
        body = body.trim();
    }
}
