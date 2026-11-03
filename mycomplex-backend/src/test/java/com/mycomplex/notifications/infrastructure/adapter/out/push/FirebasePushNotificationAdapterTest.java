package com.mycomplex.notifications.infrastructure.adapter.out.push;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.mycomplex.notifications.application.model.PushNotification;
import com.mycomplex.notifications.infrastructure.adapter.out.persistence.PushInstallationPersistenceAdapter;
import com.mycomplex.notifications.infrastructure.config.FirebasePushProperties;

import io.micrometer.observation.tck.TestObservationRegistry;

class FirebasePushNotificationAdapterTest {

    private FirebaseMessaging messaging;
    private TestObservationRegistry observations;
    private FirebasePushNotificationAdapter adapter;

    @BeforeEach
    void setUp() {
        messaging = mock(FirebaseMessaging.class);
        PushInstallationPersistenceAdapter installations = mock(
                PushInstallationPersistenceAdapter.class
        );
        when(installations.loadActiveInstallationIds(any()))
                .thenReturn(List.of("installation-secret"));
        observations = TestObservationRegistry.create();
        adapter = new FirebasePushNotificationAdapter(
                messaging,
                installations,
                new FirebasePushProperties(
                        true,
                        "project-id",
                        "http://localhost:4200"
                ),
                observations
        );
    }

    @Test
    void recordsSuccessfulPushWithoutDestinationOrPropertyTags()
            throws Exception {
        when(messaging.send(any(Message.class))).thenReturn("message-id");

        adapter.send(notification());

        assertObservation("success", false);
    }

    @Test
    void recordsFailedPushAndPreservesThrownFailure() throws Exception {
        FirebaseMessagingException failure = mock(
                FirebaseMessagingException.class
        );
        when(messaging.send(any(Message.class))).thenThrow(failure);

        assertThatThrownBy(() -> adapter.send(notification()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not send Firebase push notifications")
                .hasCause(failure);

        assertObservation("error", true);
    }

    private PushNotification notification() {
        return new PushNotification(
                Set.of(UUID.randomUUID()),
                "New meeting",
                "A meeting was scheduled.",
                Map.of(
                        "propertyId", UUID.randomUUID().toString(),
                        "meetingId", UUID.randomUUID().toString(),
                        "type", "MEETING_CREATED"
                )
        );
    }

    private void assertObservation(String outcome, boolean hasError) {
        var assertion = observations.assertThat()
                .hasSingleObservationThat()
                .hasNameEqualTo("firebase.fcm.send")
                .hasLowCardinalityKeyValue("messaging.system", "firebase")
                .hasLowCardinalityKeyValue(
                        "notification.type",
                        "meeting_created"
                )
                .hasLowCardinalityKeyValue("outcome", outcome)
                .hasOnlyKeys(
                        "messaging.system",
                        "notification.type",
                        "outcome"
                );

        if (hasError) {
            assertion.hasError();
        } else {
            assertion.doesNotHaveError();
        }
    }
}
