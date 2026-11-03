package com.mycomplex.notifications.infrastructure.adapter.out.email;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.mycomplex.notifications.application.port.out.EmailDeliveryPort;
import com.mycomplex.notifications.domain.EmailDeliveryResult;
import com.mycomplex.notifications.domain.EmailOutboxEvent;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

public final class ResendEmailNotificationAdapter
        implements EmailDeliveryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            ResendEmailNotificationAdapter.class
    );
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final RestClient restClient;
    private final String from;
    private final ObservationRegistry observations;

    public ResendEmailNotificationAdapter(
            String baseUrl,
            String apiKey,
            String from,
            ObservationRegistry observations
    ) {
        String validatedBaseUrl = requireText(baseUrl, "base URL");
        String validatedApiKey = requireText(apiKey, "API key");
        this.from = requireText(from, "sender");
        this.observations = Objects.requireNonNull(
                observations,
                "observation registry must not be null"
        );

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
    public EmailDeliveryResult deliver(EmailOutboxEvent event) {
        Objects.requireNonNull(
                event,
                "email outbox event must not be null"
        );

        Observation observation = Observation.start(
                "resend.email.send",
                observations
        );

        try {
            EmailDeliveryResult result;
            try (Observation.Scope ignored = observation.openScope()) {
                result = sendToRecipient(event);
            }

            boolean sent = result.outcome()
                    == EmailDeliveryResult.Outcome.SENT;
            observation.lowCardinalityKeyValue(
                    "outcome",
                    sent ? "success" : "error"
            );

            if (!sent) {
                observation.error(
                        new IllegalStateException(result.safeReason())
                );
                LOGGER.atWarn()
                        .addKeyValue("integration", "resend")
                        .addKeyValue("operation", "send")
                        .addKeyValue("reason", result.safeReason())
                        .log("External email delivery failed");
            }

            return result;
        } catch (RuntimeException exception) {
            observation.lowCardinalityKeyValue("outcome", "error");
            observation.error(exception);
            LOGGER.atWarn()
                    .addKeyValue("integration", "resend")
                    .addKeyValue("operation", "send")
                    .log("External email delivery failed");
            return EmailDeliveryResult.permanent("unexpected_error");
        } finally {
            observation.stop();
        }
    }

    private EmailDeliveryResult sendToRecipient(EmailOutboxEvent event) {
        ResendEmailRequest request = new ResendEmailRequest(
                from,
                List.of(event.recipient()),
                event.subject(),
                event.body()
        );

        try {
            ResendEmailResponse response = restClient.post()
                    .uri("/emails")
                    .header(
                            IDEMPOTENCY_HEADER,
                            idempotencyKey(
                                    event.deliveryKey(),
                                    event.recipient()
                            )
                    )
                    .body(request)
                    .retrieve()
                    .body(ResendEmailResponse.class);

            return EmailDeliveryResult.sent(
                    response == null ? null : response.id()
            );
        } catch (ResourceAccessException exception) {
            return EmailDeliveryResult.retryable("network_error");
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 408 || status == 429 || status >= 500) {
                return EmailDeliveryResult.retryable("http_" + status);
            }
            return EmailDeliveryResult.permanent("http_" + status);
        }
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

    private record ResendEmailResponse(String id) { }
}
