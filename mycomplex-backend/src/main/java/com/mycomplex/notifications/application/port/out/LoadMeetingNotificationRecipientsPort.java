package com.mycomplex.notifications.application.port.out;

import java.util.List;
import java.util.UUID;

import com.mycomplex.notifications.application.model.NotificationRecipient;

public interface LoadMeetingNotificationRecipientsPort {

    List<NotificationRecipient> loadActiveByPropertyId(
            UUID propertyId
    );
}
