package com.mycomplex.notifications.infrastructure.adapter.out.push;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.mycomplex.notifications.application.model.PushNotification;
import com.mycomplex.notifications.application.port.out.SendPushNotificationPort;
import com.mycomplex.notifications.infrastructure.adapter.out.persistence.PushInstallationPersistenceAdapter;
import com.mycomplex.notifications.infrastructure.config.FirebasePushProperties;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

@Component
@ConditionalOnProperty(
        prefix = "mycomplex.notifications.push.firebase",
        name = "enabled",
        havingValue = "true"
)
public class FirebasePushNotificationAdapter
        implements SendPushNotificationPort {

    private final FirebaseMessaging messaging;
    private final PushInstallationPersistenceAdapter installations;
    private final FirebasePushProperties properties;
    private final ObservationRegistry observations;

    public FirebasePushNotificationAdapter(
            FirebaseMessaging messaging,
            PushInstallationPersistenceAdapter installations,
            FirebasePushProperties properties,
            ObservationRegistry observations
    ) {
        this.messaging = messaging;
        this.installations = installations;
        this.properties = properties;
        this.observations = observations;
    }

    @Override
    public void send(PushNotification notification) {
        List<String> destinationIds = installations
                .loadActiveInstallationIds(notification.recipientIds());

        for (String destinationId : destinationIds) {
            sendToInstallation(notification, destinationId);
        }
    }

    private void sendToInstallation(
            PushNotification notification,
            String destinationId
    ) {
        String propertyId = notification.data().get("propertyId");
        String type = notification.data().getOrDefault("type", "UNKNOWN");
        String resourcePath = type.startsWith("MEETING_")
                ? "/meetings"
                : "/news";
        String link = properties.frontendBaseUrl()
                + "/properties/"
                + propertyId
                + resourcePath;

        Message message = Message.builder()
                .setFid(destinationId)
                .putAllData(notification.data())
                .putData("title", notification.title())
                .putData("body", notification.body())
                .putData("link", link)
                .build();

        Observation observation = Observation.start(
                "firebase.fcm.send",
                observations
        ).lowCardinalityKeyValue("messaging.system", "firebase")
                .lowCardinalityKeyValue(
                        "notification.type",
                        type.toLowerCase(Locale.ROOT)
                );

        try {
            try (Observation.Scope ignored = observation.openScope()) {
                messaging.send(message);
            }
            observation.lowCardinalityKeyValue("outcome", "success");
        } catch (FirebaseMessagingException exception) {
            observation.lowCardinalityKeyValue("outcome", "error");
            observation.error(exception);
            throw new IllegalStateException(
                    "Could not send Firebase push notifications",
                    exception
            );
        } finally {
            observation.stop();
        }
    }
}
