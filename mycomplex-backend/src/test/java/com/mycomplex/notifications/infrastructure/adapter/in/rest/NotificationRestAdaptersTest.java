package com.mycomplex.notifications.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import com.mycomplex.notifications.application.exception.FcmInstallationException;
import com.mycomplex.notifications.application.port.in.FindFirebaseWebConfigUseCase;
import com.mycomplex.notifications.application.port.in.ManageFcmInstallationUseCase;
import com.mycomplex.notifications.domain.FcmInstallation;
import com.mycomplex.notifications.infrastructure.adapter.in.rest.dto.RegisterFcmInstallationRequest;
import com.mycomplex.observability.api.CorrelationIds;

class NotificationRestAdaptersTest {

    @Test
    void registersRevokesAndReturnsPublicConfiguration() {
        ManageFcmInstallationUseCase installations = mock(
                ManageFcmInstallationUseCase.class
        );
        Jwt jwt = mock(Jwt.class);
        UUID installationId = UUID.randomUUID();
        when(jwt.getSubject()).thenReturn("auth0|worker");
        when(installations.register(
                "auth0|worker", "firebase-installation-id"
        )).thenReturn(new FcmInstallation(
                installationId, UUID.randomUUID()
        ));
        var installationController = new FcmInstallationController(
                installations
        );

        var response = installationController.register(
                new RegisterFcmInstallationRequest(
                        "firebase-installation-id"
                ), jwt
        );
        assertThat(response.id()).isEqualTo(installationId);
        assertThat(response.status()).isEqualTo("ACTIVE");
        installationController.revoke(installationId, jwt);
        verify(installations).revoke("auth0|worker", installationId);

        FindFirebaseWebConfigUseCase config = mock(
                FindFirebaseWebConfigUseCase.class
        );
        when(config.find()).thenReturn(
                new FindFirebaseWebConfigUseCase.FirebaseWebConfig(
                        true, "api", "domain", "project", "bucket",
                        "sender", "app", "vapid"
                )
        );
        var publicResponse = new FirebaseWebConfigController(config).find();
        assertThat(publicResponse.enabled()).isTrue();
        assertThat(publicResponse.apiKey()).isEqualTo("api");
        assertThat(publicResponse.vapidKey()).isEqualTo("vapid");
    }

    @Test
    void mapsInstallationErrorsAndUnexpectedFailure() {
        NotificationRestExceptionHandler handler =
                new NotificationRestExceptionHandler();
        assertThat(handler.handle(error("FCM_INSTALLATION_NOT_FOUND"))
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handle(error("FCM_INSTALLATION_CONFLICT"))
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handle(error("FCM_USER_NOT_AVAILABLE"))
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handle(error("INVALID_FCM_INSTALLATION"))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        MDC.put(CorrelationIds.MDC_KEY, "correlation-id");
        try {
            var response = handler.unexpected(new IllegalStateException());
            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().correlationId())
                    .isEqualTo("correlation-id");
        } finally {
            MDC.remove(CorrelationIds.MDC_KEY);
        }
    }

    private FcmInstallationException error(String code) {
        return new FcmInstallationException(code, "Mensaje seguro");
    }
}
