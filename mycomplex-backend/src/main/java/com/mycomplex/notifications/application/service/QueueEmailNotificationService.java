package com.mycomplex.notifications.application.service;

import java.util.Objects;

import com.mycomplex.notifications.application.model.EmailNotification;
import com.mycomplex.notifications.application.port.out.EmailOutboxPort;
import com.mycomplex.notifications.application.port.out.SendEmailNotificationPort;

public final class QueueEmailNotificationService
        implements SendEmailNotificationPort {

    private final EmailOutboxPort outbox;

    public QueueEmailNotificationService(EmailOutboxPort outbox) {
        this.outbox = Objects.requireNonNull(outbox);
    }

    @Override
    public void send(EmailNotification notification) {
        outbox.enqueue(notification);
    }
}
