package com.mycomplex.notifications.infrastructure.adapter.out.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.mycomplex.notifications.domain.FcmDeliveryResult;
import com.mycomplex.notifications.domain.NotificationEventType;

import io.micrometer.observation.tck.TestObservationRegistry;

class FirebaseFcmDeliveryAdapterObservationTest {

    @Test
    void recordsSuccessfulDeliveryWithoutInstallationTags() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        when(messaging.send(any(Message.class))).thenReturn("message-id");
        TestObservationRegistry observations = TestObservationRegistry.create();
        FirebaseFcmDeliveryAdapter adapter = new FirebaseFcmDeliveryAdapter(
                messaging,
                observations
        );

        FcmDeliveryResult result = adapter.sendAssignment(
                "installation-secret",
                NotificationEventType.TASK_ASSIGNED
        );

        assertThat(result.outcome()).isEqualTo(FcmDeliveryResult.Outcome.SENT);
        assertThat(result.externalMessageId()).isEqualTo("message-id");
        assertObservation(observations, "success", false);
    }

    @Test
    void recordsFailedDeliveryAndPreservesClassification() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        FirebaseMessagingException failure = mock(
                FirebaseMessagingException.class
        );
        when(failure.getMessagingErrorCode())
                .thenReturn(MessagingErrorCode.UNAVAILABLE);
        when(messaging.send(any(Message.class))).thenThrow(failure);
        TestObservationRegistry observations = TestObservationRegistry.create();
        FirebaseFcmDeliveryAdapter adapter = new FirebaseFcmDeliveryAdapter(
                messaging,
                observations
        );

        FcmDeliveryResult result = adapter.sendAssignment(
                "installation-secret",
                NotificationEventType.TASK_REASSIGNED
        );

        assertThat(result.outcome()).isEqualTo(
                FcmDeliveryResult.Outcome.RETRYABLE_FAILURE
        );
        assertThat(result.safeReason()).isEqualTo("unavailable");
        assertObservation(observations, "error", true);
    }

    @Test
    void recordsNotConfiguredDeliveryWithoutCallingFirebase() {
        TestObservationRegistry observations = TestObservationRegistry.create();
        FirebaseFcmDeliveryAdapter adapter = new FirebaseFcmDeliveryAdapter(
                (FirebaseMessaging) null,
                observations
        );

        FcmDeliveryResult result = adapter.sendAssignment(
                "installation-secret",
                NotificationEventType.TASK_ASSIGNED
        );

        assertThat(result.outcome()).isEqualTo(
                FcmDeliveryResult.Outcome.NOT_CONFIGURED
        );
        assertObservation(observations, "not_configured", false);
    }

    private void assertObservation(
            TestObservationRegistry observations,
            String outcome,
            boolean hasError
    ) {
        var assertion = observations.assertThat()
                .hasSingleObservationThat()
                .hasNameEqualTo("firebase.fcm.send")
                .hasLowCardinalityKeyValue("messaging.system", "firebase")
                .hasLowCardinalityKeyValue("outcome", outcome)
                .hasOnlyKeys("messaging.system", "outcome");

        if (hasError) {
            assertion.hasError();
        } else {
            assertion.doesNotHaveError();
        }
    }
}
