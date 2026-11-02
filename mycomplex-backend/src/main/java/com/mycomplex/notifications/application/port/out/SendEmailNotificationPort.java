package com.mycomplex.notifications.application.port.out;

import com.mycomplex.notifications.application.model.EmailNotification;

public interface SendEmailNotificationPort {

    void send(EmailNotification notification);
}
