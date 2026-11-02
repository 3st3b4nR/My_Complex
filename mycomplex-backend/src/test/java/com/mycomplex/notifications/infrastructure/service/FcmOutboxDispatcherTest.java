package com.mycomplex.notifications.infrastructure.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.notifications.application.port.out.FcmDeliveryPort;
import com.mycomplex.notifications.application.port.out.FcmInstallationPort;
import com.mycomplex.notifications.application.port.out.FcmOutboxPort;
import com.mycomplex.notifications.domain.FcmDeliveryResult;
import com.mycomplex.notifications.domain.FcmOutboxEvent;
import com.mycomplex.notifications.domain.NotificationEventType;
import com.mycomplex.notifications.infrastructure.config.FcmProperties;
import com.mycomplex.observability.api.TaskOperationalMetrics;

class FcmOutboxDispatcherTest {

    @Test
    void marksEventSentWithoutExposingDestination() {
        FcmOutboxPort outbox = mock(FcmOutboxPort.class);
        FcmInstallationPort installations = mock(FcmInstallationPort.class);
        FcmDeliveryPort delivery = mock(FcmDeliveryPort.class);
        TaskOperationalMetrics metrics = mock(TaskOperationalMetrics.class);
        FcmOutboxEvent event = event();
        var destination = new FcmInstallationPort.Destination(
                UUID.randomUUID(), "firebase-installation-identifier"
        );
        when(outbox.claimNext()).thenReturn(Optional.of(event));
        when(installations.findMostRecentActive(event.targetUserId()))
                .thenReturn(Optional.of(destination));
        when(delivery.sendAssignment(
                destination.identifier(), event.eventType()
        )).thenReturn(FcmDeliveryResult.sent("message-id"));

        dispatcher(outbox, installations, delivery, metrics).dispatchNext();

        verify(outbox).markSent(event.id(), "message-id");
        verify(metrics).fcmDelivery("SENT");
    }

    @Test
    void marksNoDestinationWhenUserHasNoActiveInstallation() {
        FcmOutboxPort outbox = mock(FcmOutboxPort.class);
        FcmInstallationPort installations = mock(FcmInstallationPort.class);
        FcmDeliveryPort delivery = mock(FcmDeliveryPort.class);
        TaskOperationalMetrics metrics = mock(TaskOperationalMetrics.class);
        FcmOutboxEvent event = event();
        when(outbox.claimNext()).thenReturn(Optional.of(event));
        when(installations.findMostRecentActive(event.targetUserId()))
                .thenReturn(Optional.empty());

        dispatcher(outbox, installations, delivery, metrics).dispatchNext();

        verify(outbox).markNoDestination(event.id(), "no_destination");
        verify(metrics).fcmDelivery("NO_DESTINATION");
    }

    @Test
    void skipsDispatchWhenFcmIsDisabledOrDeliveryIsUnavailable() {
        FcmOutboxPort outbox = mock(FcmOutboxPort.class);
        FcmInstallationPort installations = mock(FcmInstallationPort.class);
        FcmDeliveryPort delivery = mock(FcmDeliveryPort.class);
        FcmProperties disabled = properties(false, 3);

        new FcmOutboxDispatcher(outbox, installations, delivery, disabled,
                mock(TaskOperationalMetrics.class)).dispatchNext();
        verify(outbox, never()).claimNext();

        disabled.setEnabled(true);
        when(delivery.isConfigured()).thenReturn(false);
        new FcmOutboxDispatcher(outbox, installations, delivery, disabled,
                mock(TaskOperationalMetrics.class)).dispatchNext();
        verify(outbox, never()).claimNext();
    }

    @Test
    void revokesInvalidDestinationAndMarksEventWithoutDestination() {
        Scenario scenario = scenario(FcmDeliveryResult.failed(
                FcmDeliveryResult.Outcome.INVALID_DESTINATION,
                "invalid_destination"
        ), 0, 3);

        scenario.dispatcher().dispatchNext();

        verify(scenario.installations()).revokeByIdentifier(
                scenario.destination().identifier()
        );
        verify(scenario.outbox()).markNoDestination(
                scenario.event().id(), "invalid_destination"
        );
        verify(scenario.metrics()).fcmDeliveryFailure("invalid_destination");
    }

    @Test
    void schedulesRetryWithConfiguredBackoffForTemporaryFailure() {
        Scenario scenario = scenario(FcmDeliveryResult.failed(
                FcmDeliveryResult.Outcome.RETRYABLE_FAILURE, "unavailable"
        ), 1, 4);

        scenario.dispatcher().dispatchNext();

        verify(scenario.outbox()).scheduleRetry(
                scenario.event().id(), 2, java.time.Duration.ofSeconds(2),
                "unavailable"
        );
        verify(scenario.metrics()).fcmDeliveryRetry("unavailable");
    }

    @Test
    void exhaustsRetriesAndHandlesPermanentOrUnconfiguredDelivery() {
        Scenario exhausted = scenario(FcmDeliveryResult.failed(
                FcmDeliveryResult.Outcome.RETRYABLE_FAILURE, "unavailable"
        ), 2, 3);
        exhausted.dispatcher().dispatchNext();
        verify(exhausted.outbox()).markFailed(
                exhausted.event().id(), "unavailable"
        );

        Scenario permanent = scenario(FcmDeliveryResult.failed(
                FcmDeliveryResult.Outcome.PERMANENT_FAILURE,
                "permission_denied"
        ), 0, 3);
        permanent.dispatcher().dispatchNext();
        verify(permanent.outbox()).markFailed(
                permanent.event().id(), "permission_denied"
        );

        Scenario unconfigured = scenario(FcmDeliveryResult.failed(
                FcmDeliveryResult.Outcome.NOT_CONFIGURED, "not_configured"
        ), 0, 3);
        unconfigured.dispatcher().dispatchNext();
        verify(unconfigured.outbox()).markFailed(
                unconfigured.event().id(), "not_configured"
        );
    }

    private FcmOutboxDispatcher dispatcher(FcmOutboxPort outbox,
            FcmInstallationPort installations, FcmDeliveryPort delivery,
            TaskOperationalMetrics metrics) {
        when(delivery.isConfigured()).thenReturn(true);
        FcmProperties properties = new FcmProperties();
        properties.setEnabled(true);
        properties.setRetryDelay(java.time.Duration.ofSeconds(1));
        return new FcmOutboxDispatcher(
                outbox, installations, delivery, properties, metrics
        );
    }

    private Scenario scenario(FcmDeliveryResult result, int attempts,
            int maxRetries) {
        FcmOutboxPort outbox = mock(FcmOutboxPort.class);
        FcmInstallationPort installations = mock(FcmInstallationPort.class);
        FcmDeliveryPort delivery = mock(FcmDeliveryPort.class);
        TaskOperationalMetrics metrics = mock(TaskOperationalMetrics.class);
        FcmOutboxEvent event = new FcmOutboxEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), NotificationEventType.TASK_ASSIGNED,
                attempts, OffsetDateTime.now()
        );
        var destination = new FcmInstallationPort.Destination(
                UUID.randomUUID(), "firebase-installation-identifier"
        );
        when(outbox.claimNext()).thenReturn(Optional.of(event));
        when(installations.findMostRecentActive(event.targetUserId()))
                .thenReturn(Optional.of(destination));
        when(delivery.isConfigured()).thenReturn(true);
        when(delivery.sendAssignment(
                destination.identifier(), event.eventType()
        )).thenReturn(result);
        FcmProperties properties = properties(true, maxRetries);
        return new Scenario(
                new FcmOutboxDispatcher(outbox, installations, delivery,
                        properties, metrics),
                outbox, installations, metrics, event, destination
        );
    }

    private FcmProperties properties(boolean enabled, int maxRetries) {
        FcmProperties properties = new FcmProperties();
        properties.setEnabled(enabled);
        properties.setMaxRetries(maxRetries);
        properties.setRetryDelay(java.time.Duration.ofSeconds(1));
        return properties;
    }

    private record Scenario(
            FcmOutboxDispatcher dispatcher,
            FcmOutboxPort outbox,
            FcmInstallationPort installations,
            TaskOperationalMetrics metrics,
            FcmOutboxEvent event,
            FcmInstallationPort.Destination destination
    ) { }

    private FcmOutboxEvent event() {
        return new FcmOutboxEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), NotificationEventType.TASK_ASSIGNED, 0,
                OffsetDateTime.now()
        );
    }
}
