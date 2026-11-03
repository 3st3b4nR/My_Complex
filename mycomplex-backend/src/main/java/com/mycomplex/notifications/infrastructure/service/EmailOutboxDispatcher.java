package com.mycomplex.notifications.infrastructure.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.notifications.application.port.out.EmailDeliveryPort;
import com.mycomplex.notifications.application.port.out.EmailOutboxPort;
import com.mycomplex.notifications.domain.EmailDeliveryResult;
import com.mycomplex.notifications.domain.EmailOutboxEvent;
import com.mycomplex.notifications.infrastructure.config.ResendEmailProperties;

public class EmailOutboxDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            EmailOutboxDispatcher.class
    );

    private final EmailOutboxPort outbox;
    private final EmailDeliveryPort delivery;
    private final ResendEmailProperties properties;

    public EmailOutboxDispatcher(
            EmailOutboxPort outbox,
            EmailDeliveryPort delivery,
            ResendEmailProperties properties
    ) {
        this.outbox = outbox;
        this.delivery = delivery;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString =
                    "${mycomplex.notifications.email.resend.dispatch-interval:5s}"
    )
    @Transactional
    public void dispatchNext() {
        if (!properties.isEnabled()) {
            return;
        }
        outbox.claimNext().ifPresent(this::dispatch);
    }

    private void dispatch(EmailOutboxEvent event) {
        EmailDeliveryResult result = delivery.deliver(event);
        switch (result.outcome()) {
            case SENT -> outbox.markSent(
                    event.id(),
                    result.externalMessageId()
            );
            case RETRYABLE_FAILURE -> retryOrFail(
                    event,
                    result.safeReason()
            );
            case PERMANENT_FAILURE -> outbox.markFailed(
                    event.id(),
                    result.safeReason()
            );
        }
    }

    private void retryOrFail(
            EmailOutboxEvent event,
            String reason
    ) {
        int attempts = event.attempts() + 1;
        if (attempts >= Math.max(1, properties.getMaxRetries())) {
            outbox.markFailed(event.id(), reason);
            return;
        }

        long multiplier = 1L << Math.min(attempts - 1, 6);
        Duration delay = properties.getRetryDelay().multipliedBy(multiplier);
        outbox.scheduleRetry(event.id(), attempts, delay, reason);
        LOGGER.atWarn()
                .addKeyValue("email.reason", reason)
                .addKeyValue("email.attempt", attempts)
                .log("Email delivery scheduled for retry");
    }
}
