package com.mycomplex.observability.infrastructure.adapter.out.metrics;

import org.springframework.stereotype.Component;

import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.observability.application.port.out.ObservabilityMetricsPort;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class MicrometerObservabilityMetricsAdapter
        implements ObservabilityMetricsPort {

    private final MeterRegistry registry;

    public MicrometerObservabilityMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void administrativeEvent(SecurityAuditEventType type) {
        registry.counter(
                "mycomplex.administrative.events",
                "event", type.name().toLowerCase()
        ).increment();
    }

    @Override
    public void administrativeAccessDenied(String operation) {
        registry.counter(
                "mycomplex.administrative.access.denied",
                "operation", normalizeOperation(operation)
        ).increment();
    }

    @Override
    public void invitationFailed(String operation, String reasonCode) {
        registry.counter(
                "mycomplex.invitation.failures",
                "operation", normalizeInvitationOperation(operation),
                "reason", normalizeInvitationReason(reasonCode)
        ).increment();
    }

    @Override
    public void auditRejected(String reason) {
        registry.counter(
                "mycomplex.audit.rejected",
                "reason", "validation".equals(reason)
                        ? "validation" : "persistence"
        ).increment();
    }

    private String normalizeOperation(String value) {
        return switch (value) {
            case "member_registration", "membership_update",
                    "member_unlink", "member_invitation" -> value;
            default -> "other";
        };
    }

    private String normalizeInvitationOperation(String value) {
        return "resend".equals(value) ? "resend" : "invite";
    }

    private String normalizeInvitationReason(String value) {
        if (value == null) {
            return "other";
        }
        return switch (value) {
            case "AUTH0_RATE_LIMITED" -> "rate_limited";
            case "AUTH0_UNAVAILABLE", "AUTH0_INVITATION_UNAVAILABLE" ->
                    "unavailable";
            case "AUTH0_PERMISSION_DENIED" -> "permission_denied";
            default -> "other";
        };
    }
}
