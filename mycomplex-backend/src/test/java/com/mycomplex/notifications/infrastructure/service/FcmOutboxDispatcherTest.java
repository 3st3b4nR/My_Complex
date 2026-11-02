package com.mycomplex.notifications.infrastructure.service;

import static org.mockito.Mockito.mock;
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

    private FcmOutboxDispatcher dispatcher(FcmOutboxPort outbox,
            FcmInstallationPort installations, FcmDeliveryPort delivery,
            TaskOperationalMetrics metrics) {
        when(delivery.isConfigured()).thenReturn(true);
        FcmProperties properties = new FcmProperties();
        properties.setEnabled(true);
        return new FcmOutboxDispatcher(
                outbox, installations, delivery, properties, metrics
        );
    }

    private FcmOutboxEvent event() {
        return new FcmOutboxEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), NotificationEventType.TASK_ASSIGNED, 0,
                OffsetDateTime.now()
        );
    }
}
