package com.mycomplex.notifications.infrastructure.adapter.out.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mycomplex.notifications.domain.EmailDeliveryResult;
import com.mycomplex.notifications.domain.EmailOutboxEvent;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.micrometer.observation.tck.TestObservationRegistry;

class ResendEmailNotificationAdapterTest {

    private HttpServer server;
    private final AtomicInteger responseStatus = new AtomicInteger(202);

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/emails", this::respond);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void recordsSuccessfulResendDeliveryWithoutRecipientTags() {
        TestObservationRegistry observations = TestObservationRegistry.create();
        ResendEmailNotificationAdapter adapter = adapter(observations);

        EmailDeliveryResult result = adapter.deliver(event());

        assertThat(result.outcome())
                .isEqualTo(EmailDeliveryResult.Outcome.SENT);
        assertThat(result.externalMessageId()).isEqualTo("message-id");

        observations.assertThat()
                .hasSingleObservationThat()
                .hasNameEqualTo("resend.email.send")
                .hasLowCardinalityKeyValue("outcome", "success")
                .hasOnlyKeys("outcome")
                .doesNotHaveError();
    }

    @Test
    void recordsFailedResendDeliveryAndPreservesFailure() {
        responseStatus.set(503);
        TestObservationRegistry observations = TestObservationRegistry.create();
        ResendEmailNotificationAdapter adapter = adapter(observations);

        EmailDeliveryResult result = adapter.deliver(event());

        assertThat(result.outcome())
                .isEqualTo(EmailDeliveryResult.Outcome.RETRYABLE_FAILURE);
        assertThat(result.safeReason()).isEqualTo("http_503");

        observations.assertThat()
                .hasSingleObservationThat()
                .hasNameEqualTo("resend.email.send")
                .hasLowCardinalityKeyValue("outcome", "error")
                .hasOnlyKeys("outcome")
                .hasError();
    }

    private ResendEmailNotificationAdapter adapter(
            TestObservationRegistry observations
    ) {
        return new ResendEmailNotificationAdapter(
                "http://localhost:" + server.getAddress().getPort(),
                "test-api-key",
                "notifications@example.com",
                observations
        );
    }

    private EmailOutboxEvent event() {
        return new EmailOutboxEvent(
                UUID.randomUUID(),
                "resident@example.com",
                "Community update",
                "The water service is restored.",
                "news-42-created",
                0,
                OffsetDateTime.now()
        );
    }

    private void respond(HttpExchange exchange) throws IOException {
        exchange.getRequestBody().readAllBytes();
        byte[] body = "{\"id\":\"message-id\"}"
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add(
                "Content-Type",
                "application/json"
        );
        exchange.sendResponseHeaders(responseStatus.get(), body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
