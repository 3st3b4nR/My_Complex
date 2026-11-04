package com.mycomplex.observability.infrastructure.adapter.out.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.mycomplex.observability.api.SecurityAuditEventType;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class MicrometerObservabilityMetricsAdapterTest {
    @Test
    void recordsAdministrativeInvitationAndAuditMetricsWithBoundedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerObservabilityMetricsAdapter adapter =
                new MicrometerObservabilityMetricsAdapter(registry);
        adapter.administrativeEvent(SecurityAuditEventType.MEMBER_REGISTERED);
        adapter.administrativeAccessDenied("member_registration");
        adapter.administrativeAccessDenied("unknown-operation");
        adapter.invitationFailed("resend", "AUTH0_RATE_LIMITED");
        adapter.invitationFailed("invite", "AUTH0_UNAVAILABLE");
        adapter.invitationFailed("other", "AUTH0_PERMISSION_DENIED");
        adapter.invitationFailed("other", null);
        adapter.auditRejected("validation");
        adapter.auditRejected("database");

        assertThat(registry.get("mycomplex.administrative.events")
                .tag("event", "member_registered").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.administrative.access.denied")
                .tag("operation", "member_registration").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.administrative.access.denied")
                .tag("operation", "other").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.invitation.failures")
                .tag("operation", "resend").tag("reason", "rate_limited")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.invitation.failures")
                .tag("operation", "invite").tag("reason", "unavailable")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.invitation.failures")
                .tag("operation", "invite").tag("reason", "permission_denied")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.invitation.failures")
                .tag("operation", "invite").tag("reason", "other")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.audit.rejected")
                .tag("reason", "validation").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.audit.rejected")
                .tag("reason", "persistence").counter().count()).isEqualTo(1);
    }

    @Test
    void recordsTaskAndFcmMetricsWithControlledCardinality() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerObservabilityMetricsAdapter adapter =
                new MicrometerObservabilityMetricsAdapter(registry);
        adapter.taskCreated("HIGH");
        adapter.taskCreated(null);
        adapter.taskAssignment(false);
        adapter.taskAssignment(true);
        adapter.taskStatusChanged("COMPLETED");
        adapter.taskStatusChanged("UNKNOWN");
        adapter.fcmDelivery("SENT");
        adapter.fcmDelivery(null);
        adapter.fcmDeliveryFailure("rate_limited");
        adapter.fcmDeliveryFailure("unexpected");
        adapter.fcmDeliveryRetry("not_configured");

        assertThat(registry.get("mycomplex.tasks.created")
                .tag("priority", "high").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.tasks.created")
                .tag("priority", "other").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.task.assignments")
                .tag("operation", "assign").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.task.assignments")
                .tag("operation", "reassign").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.task.status.changes")
                .tag("status", "completed").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.task.status.changes")
                .tag("status", "other").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.fcm.deliveries")
                .tag("result", "sent").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.fcm.deliveries")
                .tag("result", "other").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.fcm.delivery.failures")
                .tag("reason", "rate_limited").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.fcm.delivery.failures")
                .tag("reason", "other").counter().count()).isEqualTo(1);
        assertThat(registry.get("mycomplex.fcm.delivery.retries")
                .tag("reason", "not_configured").counter().count()).isEqualTo(1);
    }
}
