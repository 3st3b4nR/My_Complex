package com.mycomplex.notifications.infrastructure.adapter.out.firebase;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.mycomplex.notifications.application.port.out.FcmDeliveryPort;
import com.mycomplex.notifications.domain.FcmDeliveryResult;
import com.mycomplex.notifications.domain.NotificationEventType;
import com.mycomplex.notifications.infrastructure.config.FcmProperties;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

@Component
public class FirebaseFcmDeliveryAdapter implements FcmDeliveryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            FirebaseFcmDeliveryAdapter.class
    );
    private static final String APP_NAME = "mycomplex-fcm";

    private final FirebaseMessaging messaging;
    private final ObservationRegistry observations;

    public FirebaseFcmDeliveryAdapter(
            FcmProperties properties,
            ObservationRegistry observations
    ) {
        messaging = initialize(properties);
        this.observations = observations;
    }

    @Override
    public boolean isConfigured() {
        return messaging != null;
    }

    @Override
    public FcmDeliveryResult sendAssignment(
            String installationIdentifier,
            NotificationEventType eventType
    ) {
        if (messaging == null) {
            return FcmDeliveryResult.failed(
                    FcmDeliveryResult.Outcome.NOT_CONFIGURED,
                    "not_configured"
            );
        }
        Observation observation = Observation.start(
                "firebase.fcm.send", observations
        ).lowCardinalityKeyValue("messaging.system", "firebase");
        try {
            Message message = Message.builder()
                    .setFid(installationIdentifier)
                    .putData("title",
                            eventType == NotificationEventType.TASK_REASSIGNED
                                    ? "Tarea reasignada"
                                    : "Nueva tarea asignada")
                    .putData("body",
                            "Tienes una tarea pendiente en MyComplex.")
                    .putData("link", "/select-property")
                    .putData("event", eventType.name())
                    .build();
            String messageId;
            try (Observation.Scope ignored = observation.openScope()) {
                messageId = messaging.send(message);
            }
            return FcmDeliveryResult.sent(messageId);
        } catch (FirebaseMessagingException exception) {
            observation.error(exception);
            return classify(exception.getMessagingErrorCode());
        } finally {
            observation.stop();
        }
    }

    private FirebaseMessaging initialize(FcmProperties properties) {
        if (!properties.isEnabled()) {
            LOGGER.info("FCM delivery is disabled");
            return null;
        }
        try {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setConnectTimeout(5_000)
                    .setReadTimeout(8_000)
                    .setWriteTimeout(8_000);
            if (properties.getProjectId() != null
                    && !properties.getProjectId().isBlank()) {
                builder.setProjectId(properties.getProjectId().strip());
            }
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(candidate -> APP_NAME.equals(candidate.getName()))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(
                            builder.build(), APP_NAME
                    ));
            return FirebaseMessaging.getInstance(app);
        } catch (IOException | RuntimeException exception) {
            LOGGER.atWarn()
                    .addKeyValue("fcm.configured", false)
                    .log("FCM credentials are unavailable; delivery disabled");
            return null;
        }
    }

    private FcmDeliveryResult classify(MessagingErrorCode code) {
        if (code == null) {
            return failure(
                    FcmDeliveryResult.Outcome.RETRYABLE_FAILURE,
                    "unavailable"
            );
        }
        return switch (code) {
            case UNREGISTERED, SENDER_ID_MISMATCH -> failure(
                    FcmDeliveryResult.Outcome.INVALID_DESTINATION,
                    "invalid_destination"
            );
            case QUOTA_EXCEEDED -> failure(
                    FcmDeliveryResult.Outcome.RETRYABLE_FAILURE,
                    "rate_limited"
            );
            case INTERNAL, UNAVAILABLE -> failure(
                    FcmDeliveryResult.Outcome.RETRYABLE_FAILURE,
                    "unavailable"
            );
            case THIRD_PARTY_AUTH_ERROR -> failure(
                    FcmDeliveryResult.Outcome.PERMANENT_FAILURE,
                    "permission_denied"
            );
            default -> failure(
                    FcmDeliveryResult.Outcome.PERMANENT_FAILURE,
                    "other"
            );
        };
    }

    private FcmDeliveryResult failure(
            FcmDeliveryResult.Outcome outcome,
            String safeReason
    ) {
        return FcmDeliveryResult.failed(outcome, safeReason);
    }
}
