package com.mycomplex.observability.infrastructure.adapter.out.logging;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.mycomplex.observability.application.port.out.ObservabilityMetricsPort;

class StructuredSecurityEventReporterTest {

    @Test
    void delegatesDeniedAccessAndKnownInvitationFailureToMetrics() {
        ObservabilityMetricsPort metrics =
                Mockito.mock(ObservabilityMetricsPort.class);
        StructuredSecurityEventReporter reporter =
                new StructuredSecurityEventReporter(metrics);

        reporter.administrativeAccessDenied("membership_update");
        reporter.invitationFailed("resend", "AUTH0_RATE_LIMITED");

        verify(metrics).administrativeAccessDenied("membership_update");
        verify(metrics).invitationFailed("resend", "AUTH0_RATE_LIMITED");
    }

    @Test
    void safelyLogsNullAndUnknownInvitationReasons() {
        ObservabilityMetricsPort metrics =
                Mockito.mock(ObservabilityMetricsPort.class);
        StructuredSecurityEventReporter reporter =
                new StructuredSecurityEventReporter(metrics);

        reporter.invitationFailed("invite", null);
        reporter.invitationFailed("invite", "SECRET_PROVIDER_DETAIL");

        verify(metrics).invitationFailed("invite", null);
        verify(metrics).invitationFailed(
                "invite", "SECRET_PROVIDER_DETAIL"
        );
    }
}
