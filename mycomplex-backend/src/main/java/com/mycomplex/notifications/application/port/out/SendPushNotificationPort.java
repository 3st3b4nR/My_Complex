package com.mycomplex.notifications.application.port.out;

import com.mycomplex.notifications.application.model.PushNotification;

public interface SendPushNotificationPort {

    void send(PushNotification notification);
}
