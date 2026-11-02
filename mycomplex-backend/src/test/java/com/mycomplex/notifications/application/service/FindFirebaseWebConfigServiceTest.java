package com.mycomplex.notifications.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.mycomplex.notifications.application.port.out.FcmDeliveryPort;
import com.mycomplex.notifications.application.port.out.FirebaseWebConfigPort;
import com.mycomplex.notifications.application.port.out.FirebaseWebConfigPort.PublicFirebaseWebConfig;

class FindFirebaseWebConfigServiceTest {

    @Test
    void returnsPublicConfigurationWhenDeliveryIsAvailable() {
        FirebaseWebConfigPort configPort = mock(FirebaseWebConfigPort.class);
        FcmDeliveryPort delivery = mock(FcmDeliveryPort.class);
        when(configPort.load()).thenReturn(config(true));
        when(delivery.isConfigured()).thenReturn(true);

        var result = new FindFirebaseWebConfigService(configPort, delivery)
                .find();

        assertThat(result.enabled()).isTrue();
        assertThat(result.apiKey()).isEqualTo("public-api-key");
        assertThat(result.vapidKey()).isEqualTo("public-vapid-key");
    }

    @Test
    void hidesPublicConfigurationWhenDeliveryIsUnavailable() {
        FirebaseWebConfigPort configPort = mock(FirebaseWebConfigPort.class);
        FcmDeliveryPort delivery = mock(FcmDeliveryPort.class);
        when(configPort.load()).thenReturn(config(true));
        when(delivery.isConfigured()).thenReturn(false);

        var result = new FindFirebaseWebConfigService(configPort, delivery)
                .find();

        assertThat(result.enabled()).isFalse();
        assertThat(result.apiKey()).isEmpty();
        assertThat(result.vapidKey()).isEmpty();
    }

    private PublicFirebaseWebConfig config(boolean enabled) {
        return new PublicFirebaseWebConfig(
                enabled, "public-api-key", "project.firebaseapp.com",
                "project", "project.appspot.com", "sender", "app",
                "public-vapid-key"
        );
    }
}
