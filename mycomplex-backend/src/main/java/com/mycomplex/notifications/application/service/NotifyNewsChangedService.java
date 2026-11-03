package com.mycomplex.notifications.application.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.mycomplex.news.domain.event.NewsChangedEvent;
import com.mycomplex.notifications.application.exception.NotificationDeliveryException;
import com.mycomplex.notifications.application.model.EmailNotification;
import com.mycomplex.notifications.application.model.NotificationRecipient;
import com.mycomplex.notifications.application.model.PushNotification;
import com.mycomplex.notifications.application.port.in.NotifyNewsChangedUseCase;
import com.mycomplex.notifications.application.port.out.LoadNewsNotificationRecipientsPort;
import com.mycomplex.notifications.application.port.out.SendEmailNotificationPort;
import com.mycomplex.notifications.application.port.out.SendPushNotificationPort;

public final class NotifyNewsChangedService
        implements NotifyNewsChangedUseCase {

    private final LoadNewsNotificationRecipientsPort recipientsPort;
    private final List<SendEmailNotificationPort> emailPorts;
    private final List<SendPushNotificationPort> pushPorts;

    public NotifyNewsChangedService(
            LoadNewsNotificationRecipientsPort recipientsPort,
            List<SendEmailNotificationPort> emailPorts,
            List<SendPushNotificationPort> pushPorts
    ) {
        this.recipientsPort = Objects.requireNonNull(recipientsPort);
        this.emailPorts = List.copyOf(emailPorts);
        this.pushPorts = List.copyOf(pushPorts);
    }

    @Override
    public void notify(NewsChangedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        List<NotificationRecipient> recipients = recipientsPort
                .loadActiveByPropertyId(event.propertyId());

        if (recipients.isEmpty()) {
            return;
        }

        String action = switch (event.changeType()) {
            case CREATED -> "Nueva noticia";
            case UPDATED -> "Noticia actualizada";
        };

        String title = action + ": " + event.title();
        String body = switch (event.changeType()) {
            case CREATED ->
                    "Se publicó una nueva noticia en tu copropiedad.";
            case UPDATED ->
                    "Se actualizó una noticia de tu copropiedad.";
        };

        RuntimeException failure = null;

        failure = sendEmail(
                event,
                recipients,
                title,
                body,
                failure
        );

        failure = sendPush(
                event,
                recipients,
                title,
                body,
                failure
        );

        if (failure != null) {
            throw new NotificationDeliveryException(
                    "One or more news notifications could not be sent",
                    failure
            );
        }
    }

    private RuntimeException sendEmail(
            NewsChangedEvent event,
            List<NotificationRecipient> recipients,
            String subject,
            String body,
            RuntimeException currentFailure
    ) {
        if (emailPorts.isEmpty()) {
            return currentFailure;
        }

        Set<String> emails = new LinkedHashSet<>();
        recipients.forEach(recipient ->
                emails.add(recipient.email())
        );

        String deliveryKey = "news-"
                + event.newsId()
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
            NewsChangedEvent event,
            List<NotificationRecipient> recipients,
            String title,
            String body,
            RuntimeException currentFailure
    ) {
        if (pushPorts.isEmpty()) {
            return currentFailure;
        }

        Set<UUID> recipientIds = new LinkedHashSet<>();
        recipients.forEach(recipient ->
                recipientIds.add(recipient.userId())
        );

        PushNotification notification = new PushNotification(
                recipientIds,
                title,
                body,
                Map.of(
                        "type", "NEWS_" + event.changeType().name(),
                        "newsId", event.newsId().toString(),
                        "propertyId", event.propertyId().toString()
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
