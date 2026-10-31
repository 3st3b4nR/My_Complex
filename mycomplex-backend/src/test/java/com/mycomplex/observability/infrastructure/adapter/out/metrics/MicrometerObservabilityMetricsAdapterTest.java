package com.mycomplex.observability.infrastructure.adapter.out.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.mycomplex.observability.api.SecurityAuditEventType;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class MicrometerObservabilityMetricsAdapterTest {

    @Test
    void recordsAdministrativeEventsAndNormalizesBoundedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerObservabilityMetricsAdapter metrics =
                new MicrometerObservabilityMetricsAdapter(registry);

        metrics.administrativeEvent(SecurityAuditEventType.MEMBER_REGISTERED);
        metrics.administrativeAccessDenied("member_registration");
        metrics.administrativeAccessDenied("unexpected_operation");
        metrics.auditRejected("validation");
        metrics.auditRejected("database_error");

        assertThat(registry.get("mycomplex.administrative.events")
                .tag("event", "member_registered").counter().count())
                .isEqualTo(1.0);
        assertThat(registry.get("mycomplex.administrative.access.denied")
                .tag("operation", "member_registration")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("mycomplex.administrative.access.denied")
                .tag("operation", "other").counter().count())
                .isEqualTo(1.0);
        assertThat(registry.get("mycomplex.audit.rejected")
                .tag("reason", "validation").counter().count())
                .isEqualTo(1.0);
        assertThat(registry.get("mycomplex.audit.rejected")
                .tag("reason", "persistence").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void normalizesInvitationOperationAndFailureReasons() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerObservabilityMetricsAdapter metrics =
                new MicrometerObservabilityMetricsAdapter(registry);

        metrics.invitationFailed("resend", "AUTH0_RATE_LIMITED");
        metrics.invitationFailed("invite", "AUTH0_UNAVAILABLE");
        metrics.invitationFailed("other", "AUTH0_INVITATION_UNAVAILABLE");
        metrics.invitationFailed("invite", "AUTH0_PERMISSION_DENIED");
        metrics.invitationFailed("invite", null);
        metrics.invitationFailed("invite", "UNKNOWN");

        assertCounter(registry, "resend", "rate_limited", 1.0);
        assertCounter(registry, "invite", "unavailable", 2.0);
        assertCounter(registry, "invite", "permission_denied", 1.0);
        assertCounter(registry, "invite", "other", 2.0);
    }

    private void assertCounter(
            SimpleMeterRegistry registry,
            String operation,
            String reason,
            double expected
    ) {
        assertThat(registry.get("mycomplex.invitation.failures")
                .tag("operation", operation)
                .tag("reason", reason)
                .counter().count()).isEqualTo(expected);
    }
}
