package com.mycomplex.notifications.infrastructure.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.notifications.application.port.out.EmailDeliveryPort;
import com.mycomplex.notifications.application.port.out.EmailOutboxPort;
import com.mycomplex.notifications.domain.EmailDeliveryResult;
import com.mycomplex.notifications.domain.EmailOutboxEvent;
import com.mycomplex.notifications.infrastructure.config.ResendEmailProperties;

class EmailOutboxDispatcherTest {

    @Test
    void schedulesRetryWhenResendIsTemporarilyUnavailable() {
        EmailOutboxPort outbox = mock(EmailOutboxPort.class);
        EmailDeliveryPort delivery = mock(EmailDeliveryPort.class);
        EmailOutboxEvent event = event(1);
        ResendEmailProperties properties = properties(4);

        when(outbox.claimNext()).thenReturn(Optional.of(event));
        when(delivery.deliver(event)).thenReturn(
                EmailDeliveryResult.retryable("temporary_failure")
        );

        new EmailOutboxDispatcher(
                outbox,
                delivery,
                properties
        ).dispatchNext();

        verify(outbox).scheduleRetry(
                event.id(),
                2,
                Duration.ofSeconds(2),
                "temporary_failure"
        );
    }

    @Test
    void marksEmailSentAfterSuccessfulDelivery() {
        EmailOutboxPort outbox = mock(EmailOutboxPort.class);
        EmailDeliveryPort delivery = mock(EmailDeliveryPort.class);
        EmailOutboxEvent event = event(0);

        when(outbox.claimNext()).thenReturn(Optional.of(event));
        when(delivery.deliver(event)).thenReturn(
                EmailDeliveryResult.sent("resend-message-id")
        );

        new EmailOutboxDispatcher(
                outbox,
                delivery,
                properties(4)
        ).dispatchNext();

        verify(outbox).markSent(event.id(), "resend-message-id");
    }

    private ResendEmailProperties properties(int maxRetries) {
        ResendEmailProperties properties = new ResendEmailProperties();
        properties.setEnabled(true);
        properties.setMaxRetries(maxRetries);
        properties.setRetryDelay(Duration.ofSeconds(1));
        return properties;
    }

    private EmailOutboxEvent event(int attempts) {
        return new EmailOutboxEvent(
                UUID.randomUUID(),
                "resident@example.com",
                "Community update",
                "The water service is restored.",
                "news-42-created-resident",
                attempts,
                OffsetDateTime.now()
        );
    }
}
