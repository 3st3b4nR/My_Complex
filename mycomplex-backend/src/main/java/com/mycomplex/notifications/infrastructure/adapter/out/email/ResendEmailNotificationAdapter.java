package com.mycomplex.notifications.infrastructure.adapter.out.email;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.mycomplex.notifications.application.model.EmailNotification;
import com.mycomplex.notifications.application.port.out.SendEmailNotificationPort;

public final class ResendEmailNotificationAdapter
        implements SendEmailNotificationPort {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final RestClient restClient;
    private final String from;

    public ResendEmailNotificationAdapter(
            String baseUrl,
            String apiKey,
            String from
    ) {
        String validatedBaseUrl = requireText(baseUrl, "base URL");
        String validatedApiKey = requireText(apiKey, "API key");
        this.from = requireText(from, "sender");

        this.restClient = RestClient.builder()
                .baseUrl(validatedBaseUrl)
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + validatedApiKey
                )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    @Override
    public void send(EmailNotification notification) {
        Objects.requireNonNull(
                notification,
                "email notification must not be null"
        );

        RuntimeException failure = null;

        for (String recipient : notification.recipients()) {
            try {
                sendToRecipient(notification, recipient);
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = new IllegalStateException(
                            "One or more Resend emails could not be sent"
                    );
                }

                failure.addSuppressed(exception);
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    private void sendToRecipient(
            EmailNotification notification,
            String recipient
    ) {
        ResendEmailRequest request = new ResendEmailRequest(
                from,
                List.of(recipient),
                notification.subject(),
                notification.body()
        );

        restClient.post()
                .uri("/emails")
                .header(
                        IDEMPOTENCY_HEADER,
                        idempotencyKey(notification.deliveryKey(), recipient)
                )
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private String idempotencyKey(
            String deliveryKey,
            String recipient
    ) {
        return deliveryKey + "-" + recipientFingerprint(recipient);
    }

    private String recipientFingerprint(String recipient) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    recipient
                            .trim()
                            .toLowerCase(Locale.ROOT)
                            .getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of()
                    .formatHex(hash)
                    .substring(0, 24);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Resend " + fieldName + " must not be blank"
            );
        }

        return value.trim();
    }

    private record ResendEmailRequest(
            String from,
            List<String> to,
            String subject,
            String text
    ) {
    }
}
