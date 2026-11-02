package com.mycomplex.notifications.application.port.out;

import com.mycomplex.notifications.domain.FcmDeliveryResult;
import com.mycomplex.notifications.domain.NotificationEventType;

public interface FcmDeliveryPort {
    boolean isConfigured();
    FcmDeliveryResult sendAssignment(
            String installationIdentifier,
            NotificationEventType eventType
    );
}
