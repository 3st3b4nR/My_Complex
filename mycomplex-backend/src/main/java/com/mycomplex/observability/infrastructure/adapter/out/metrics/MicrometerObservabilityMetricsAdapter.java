package com.mycomplex.observability.infrastructure.adapter.out.metrics;

import org.springframework.stereotype.Component;

import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.observability.api.TaskOperationalMetrics;
import com.mycomplex.observability.application.port.out.ObservabilityMetricsPort;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class MicrometerObservabilityMetricsAdapter
        implements ObservabilityMetricsPort, TaskOperationalMetrics {

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

    @Override
    public void taskCreated(String priority) {
        registry.counter(
                "mycomplex.tasks.created",
                "priority", normalizePriority(priority)
        ).increment();
    }

    @Override
    public void taskAssignment(boolean reassignment) {
        registry.counter(
                "mycomplex.task.assignments",
                "operation", reassignment ? "reassign" : "assign"
        ).increment();
    }

    @Override
    public void taskStatusChanged(String status) {
        registry.counter(
                "mycomplex.task.status.changes",
                "status", normalizeTaskStatus(status)
        ).increment();
    }

    @Override
    public void fcmDelivery(String result) {
        registry.counter(
                "mycomplex.fcm.deliveries",
                "result", normalizeDeliveryResult(result)
        ).increment();
    }

    @Override
    public void fcmDeliveryFailure(String reason) {
        registry.counter(
                "mycomplex.fcm.delivery.failures",
                "reason", normalizeFcmReason(reason)
        ).increment();
    }

    @Override
    public void fcmDeliveryRetry(String reason) {
        registry.counter(
                "mycomplex.fcm.delivery.retries",
                "reason", normalizeFcmReason(reason)
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

    private String normalizePriority(String value) {
        return switch (value == null ? "" : value) {
            case "LOW", "MEDIUM", "HIGH", "URGENT" ->
                    value.toLowerCase();
            default -> "other";
        };
    }

    private String normalizeTaskStatus(String value) {
        return switch (value == null ? "" : value) {
            case "PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED" ->
                    value.toLowerCase();
            default -> "other";
        };
    }

    private String normalizeDeliveryResult(String value) {
        return switch (value == null ? "" : value) {
            case "SENT", "FAILED", "NO_DESTINATION", "NOT_CONFIGURED" ->
                    value.toLowerCase();
            default -> "other";
        };
    }

    private String normalizeFcmReason(String value) {
        return switch (value == null ? "" : value) {
            case "unavailable", "rate_limited", "invalid_destination",
                    "permission_denied", "not_configured" -> value;
            default -> "other";
        };
    }
}
