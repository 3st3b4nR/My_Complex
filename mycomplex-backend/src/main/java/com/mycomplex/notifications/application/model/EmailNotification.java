package com.mycomplex.notifications.application.model;

import java.util.Set;

public record EmailNotification(
        Set<String> recipients,
        String subject,
        String body,
        String deliveryKey
) {

    public EmailNotification {
        recipients = Set.copyOf(recipients);

        if (recipients.isEmpty()) {
            throw new IllegalArgumentException(
                    "email recipients must not be empty"
            );
        }

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException(
                    "email subject must not be blank"
            );
        }

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException(
                    "email body must not be blank"
            );
        }

        if (deliveryKey == null || deliveryKey.isBlank()) {
            throw new IllegalArgumentException(
                    "email delivery key must not be blank"
            );
        }

        subject = subject.trim();
        body = body.trim();
        deliveryKey = deliveryKey.trim();
    }
}
