package com.mycomplex.notifications.application.port.out;

import java.util.List;
import java.util.UUID;

import com.mycomplex.notifications.application.model.NotificationRecipient;

public interface LoadNewsNotificationRecipientsPort {

    List<NotificationRecipient> loadActiveByPropertyId(
            UUID propertyId
    );
}
