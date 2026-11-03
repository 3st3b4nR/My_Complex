package com.mycomplex.notifications.application.service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.mycomplex.meetings.domain.event.MeetingChangedEvent;
import com.mycomplex.notifications.application.exception.NotificationDeliveryException;
import com.mycomplex.notifications.application.model.EmailNotification;
import com.mycomplex.notifications.application.model.NotificationRecipient;
import com.mycomplex.notifications.application.model.PushNotification;
import com.mycomplex.notifications.application.port.in.NotifyMeetingChangedUseCase;
import com.mycomplex.notifications.application.port.out.LoadMeetingNotificationRecipientsPort;
import com.mycomplex.notifications.application.port.out.SendEmailNotificationPort;
import com.mycomplex.notifications.application.port.out.SendPushNotificationPort;

public final class NotifyMeetingChangedService
        implements NotifyMeetingChangedUseCase {

    private final LoadMeetingNotificationRecipientsPort recipientsPort;
    private final List<SendEmailNotificationPort> emailPorts;
    private final List<SendPushNotificationPort> pushPorts;

    public NotifyMeetingChangedService(
            LoadMeetingNotificationRecipientsPort recipientsPort,
            List<SendEmailNotificationPort> emailPorts,
            List<SendPushNotificationPort> pushPorts
    ) {
        this.recipientsPort = Objects.requireNonNull(recipientsPort);
        this.emailPorts = List.copyOf(emailPorts);
        this.pushPorts = List.copyOf(pushPorts);
    }

    @Override
    public void notify(MeetingChangedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        List<NotificationRecipient> recipients = recipientsPort
                .loadActiveByPropertyId(event.propertyId());

        if (recipients.isEmpty()) {
            return;
        }

        String action = switch (event.changeType()) {
            case CREATED -> "Nueva reunión";
            case UPDATED -> "Reunión actualizada";
            case REMINDER -> "Recordatorio de reunión";
        };

        String subject = action + ": " + event.title();
        String scheduledAt = DateTimeFormatter.ISO_INSTANT.format(
                event.scheduledAt()
        );
        String body = switch (event.changeType()) {
            case CREATED ->
                    "Se programó una reunión para " + scheduledAt + ".";
            case UPDATED ->
                    "Se actualizó la reunión programada para "
                            + scheduledAt + ".";
            case REMINDER ->
                    "La reunión comienza en aproximadamente 24 horas: "
                            + scheduledAt + ".";
        };
        body = body + System.lineSeparator()
                + "Enlace: " + event.meetingUrl();

        RuntimeException failure = null;
        failure = sendEmail(event, recipients, subject, body, failure);
        failure = sendPush(event, recipients, subject, body, failure);

        if (failure != null) {
            throw new NotificationDeliveryException(
                    "One or more meeting notifications could not be sent",
                    failure
            );
        }
    }

    private RuntimeException sendEmail(
            MeetingChangedEvent event,
            List<NotificationRecipient> recipients,
            String subject,
            String body,
            RuntimeException currentFailure
    ) {
        if (emailPorts.isEmpty()) {
            return currentFailure;
        }

        Set<String> emails = new LinkedHashSet<>();
        recipients.forEach(recipient -> emails.add(recipient.email()));

        String deliveryKey = "meeting-"
                + event.meetingId()
                + "-"
                + event.changeType().name().toLowerCase(Locale.ROOT)
                + "-"
                + event.occurredAt().toEpochMilli();

        EmailNotification notification = new EmailNotification(
                emails,
                subject,
                body,
                deliveryKey
        );

        RuntimeException failure = currentFailure;
        for (SendEmailNotificationPort port : emailPorts) {
            try {
                port.send(notification);
            } catch (RuntimeException exception) {
                failure = combine(failure, exception);
            }
        }
        return failure;
    }

    private RuntimeException sendPush(
            MeetingChangedEvent event,
            List<NotificationRecipient> recipients,
            String title,
            String body,
            RuntimeException currentFailure
    ) {
        if (pushPorts.isEmpty()) {
            return currentFailure;
        }

        Set<UUID> recipientIds = new LinkedHashSet<>();
        recipients.forEach(
                recipient -> recipientIds.add(recipient.userId())
        );

        PushNotification notification = new PushNotification(
                recipientIds,
                title,
                body,
                Map.of(
                        "type",
                        "MEETING_" + event.changeType().name(),
                        "meetingId",
                        event.meetingId().toString(),
                        "propertyId",
                        event.propertyId().toString(),
                        "meetingUrl",
                        event.meetingUrl().toString()
                )
        );

        RuntimeException failure = currentFailure;
        for (SendPushNotificationPort port : pushPorts) {
            try {
                port.send(notification);
            } catch (RuntimeException exception) {
                failure = combine(failure, exception);
            }
        }
        return failure;
    }

    private RuntimeException combine(
            RuntimeException current,
            RuntimeException next
    ) {
        if (current == null) {
            return next;
        }

        current.addSuppressed(next);
        return current;
    }
}
