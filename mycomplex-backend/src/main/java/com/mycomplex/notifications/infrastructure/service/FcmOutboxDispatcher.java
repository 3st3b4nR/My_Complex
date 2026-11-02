package com.mycomplex.notifications.infrastructure.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.notifications.application.port.out.FcmDeliveryPort;
import com.mycomplex.notifications.application.port.out.FcmInstallationPort;
import com.mycomplex.notifications.application.port.out.FcmOutboxPort;
import com.mycomplex.notifications.domain.FcmDeliveryResult;
import com.mycomplex.notifications.domain.FcmOutboxEvent;
import com.mycomplex.notifications.infrastructure.config.FcmProperties;
import com.mycomplex.observability.api.TaskOperationalMetrics;

@Component
public class FcmOutboxDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            FcmOutboxDispatcher.class
    );

    private final FcmOutboxPort outbox;
    private final FcmInstallationPort installations;
    private final FcmDeliveryPort delivery;
    private final FcmProperties properties;
    private final TaskOperationalMetrics metrics;

    public FcmOutboxDispatcher(
            FcmOutboxPort outbox,
            FcmInstallationPort installations,
            FcmDeliveryPort delivery,
            FcmProperties properties,
            TaskOperationalMetrics metrics
    ) {
        this.outbox = outbox;
        this.installations = installations;
        this.delivery = delivery;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${fcm.dispatch-interval:5s}")
    @Transactional
    public void dispatchNext() {
        if (!properties.isEnabled() || !delivery.isConfigured()) {
            return;
        }
        outbox.claimNext().ifPresent(this::dispatch);
    }

    private void dispatch(FcmOutboxEvent event) {
        var destination = installations.findMostRecentActive(
                event.targetUserId()
        );
        if (destination.isEmpty()) {
            outbox.markNoDestination(event.id(), "no_destination");
            metrics.fcmDelivery("NO_DESTINATION");
            return;
        }

        FcmDeliveryResult result = delivery.sendAssignment(
                destination.get().identifier(),
                event.eventType()
        );
        switch (result.outcome()) {
            case SENT -> {
                outbox.markSent(event.id(), result.externalMessageId());
                metrics.fcmDelivery("SENT");
            }
            case INVALID_DESTINATION -> {
                installations.revokeByIdentifier(
                        destination.get().identifier()
                );
                outbox.markNoDestination(event.id(), result.safeReason());
                metrics.fcmDelivery("NO_DESTINATION");
                metrics.fcmDeliveryFailure("invalid_destination");
            }
            case RETRYABLE_FAILURE -> retryOrFail(event, result.safeReason());
            case PERMANENT_FAILURE -> {
                outbox.markFailed(event.id(), result.safeReason());
                metrics.fcmDelivery("FAILED");
                metrics.fcmDeliveryFailure(result.safeReason());
            }
            case NOT_CONFIGURED -> {
                outbox.markFailed(event.id(), "not_configured");
                metrics.fcmDelivery("NOT_CONFIGURED");
                metrics.fcmDeliveryFailure("not_configured");
            }
        }
    }

    private void retryOrFail(FcmOutboxEvent event, String reason) {
        int attempts = event.attempts() + 1;
        if (attempts >= Math.max(1, properties.getMaxRetries())) {
            outbox.markFailed(event.id(), reason);
            metrics.fcmDelivery("FAILED");
            metrics.fcmDeliveryFailure(reason);
            return;
        }
        long multiplier = 1L << Math.min(attempts - 1, 6);
        Duration delay = properties.getRetryDelay().multipliedBy(multiplier);
        outbox.scheduleRetry(event.id(), attempts, delay, reason);
        metrics.fcmDeliveryRetry(reason);
        LOGGER.atWarn()
                .addKeyValue("fcm.reason", reason)
                .addKeyValue("fcm.attempt", attempts)
                .log("FCM delivery scheduled for retry");
    }
}
