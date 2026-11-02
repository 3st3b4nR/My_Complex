package com.mycomplex.notifications.infrastructure.adapter.out.push;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import com.mycomplex.notifications.application.model.PushNotification;
import com.mycomplex.notifications.application.port.out.SendPushNotificationPort;
import com.mycomplex.notifications.infrastructure.adapter.out.persistence.PushInstallationPersistenceAdapter;
import com.mycomplex.notifications.infrastructure.config.FirebasePushProperties;

@Component
@ConditionalOnProperty(
        prefix = "mycomplex.notifications.push.firebase",
        name = "enabled",
        havingValue = "true"
)
public class FirebasePushNotificationAdapter
        implements SendPushNotificationPort {

    private static final int MAX_MULTICAST_SIZE = 500;

    private final FirebaseMessaging messaging;
    private final PushInstallationPersistenceAdapter installations;
    private final FirebasePushProperties properties;

    public FirebasePushNotificationAdapter(
            FirebaseMessaging messaging,
            PushInstallationPersistenceAdapter installations,
            FirebasePushProperties properties
    ) {
        this.messaging = messaging;
        this.installations = installations;
        this.properties = properties;
    }

    @Override
    public void send(PushNotification notification) {
        List<String> destinationIds = installations
                .loadActiveInstallationIds(notification.recipientIds());

        for (int start = 0;
                start < destinationIds.size();
                start += MAX_MULTICAST_SIZE) {
            int end = Math.min(
                    start + MAX_MULTICAST_SIZE,
                    destinationIds.size()
            );
            sendBatch(notification, destinationIds.subList(start, end));
        }
    }

    private void sendBatch(
            PushNotification notification,
            List<String> destinationIds
    ) {
        if (destinationIds.isEmpty()) {
            return;
        }

        String propertyId = notification.data().get("propertyId");
        String link = properties.frontendBaseUrl()
                + "/properties/"
                + propertyId
                + "/news";

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(destinationIds)
                .setNotification(Notification.builder()
                        .setTitle(notification.title())
                        .setBody(notification.body())
                        .build())
                .putAllData(notification.data())
                .setWebpushConfig(WebpushConfig.builder()
                        .setFcmOptions(WebpushFcmOptions.withLink(link))
                        .build())
                .build();

        try {
            BatchResponse response = messaging.sendEachForMulticast(message);
            if (response.getFailureCount() > 0) {
                throw new IllegalStateException(
                        response.getFailureCount()
                                + " Firebase push notifications failed"
                );
            }
        } catch (FirebaseMessagingException exception) {
            throw new IllegalStateException(
                    "Could not send Firebase push notifications",
                    exception
            );
        }
    }
}
