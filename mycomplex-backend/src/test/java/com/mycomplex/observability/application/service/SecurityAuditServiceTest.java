package com.mycomplex.observability.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.mycomplex.observability.api.SecurityAuditCommand;
import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.observability.api.SecurityAuditResult;
import com.mycomplex.observability.application.port.out.ObservabilityMetricsPort;
import com.mycomplex.observability.domain.SecurityAuditEvent;

class SecurityAuditServiceTest {

    @Test
    void appendsAllowedEventWithUtcAndCorrelationId() {
        AtomicReference<SecurityAuditEvent> appended = new AtomicReference<>();
        RecordingMetrics metrics = new RecordingMetrics();
        SecurityAuditService service = new SecurityAuditService(
                appended::set,
                () -> "11111111-1111-4111-8111-111111111111",
                metrics,
                Clock.fixed(
                        Instant.parse("2026-10-30T20:00:00Z"),
                        ZoneOffset.UTC
                )
        );

        service.record(new SecurityAuditCommand(
                SecurityAuditEventType.MEMBER_REGISTERED,
                SecurityAuditResult.SUCCESS,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "users", Map.of("registrationMode", "CREATE_PENDING_USER")
        ));

        assertThat(appended.get().occurredAt().toString())
                .isEqualTo("2026-10-30T20:00Z");
        assertThat(appended.get().correlationId())
                .isEqualTo("11111111-1111-4111-8111-111111111111");
        assertThat(metrics.lastEvent)
                .isEqualTo(SecurityAuditEventType.MEMBER_REGISTERED);
    }

    @Test
    void rejectsSensitiveOrUnlistedMetadataBeforePersistence() {
        AtomicReference<SecurityAuditEvent> appended = new AtomicReference<>();
        RecordingMetrics metrics = new RecordingMetrics();
        SecurityAuditService service = new SecurityAuditService(
                appended::set, UUID.randomUUID()::toString, metrics,
                Clock.systemUTC()
        );

        assertThatThrownBy(() -> service.record(new SecurityAuditCommand(
                SecurityAuditEventType.MEMBER_REGISTERED,
                SecurityAuditResult.SUCCESS,
                null, null, null, "users",
                Map.of("email", "sensitive@example.test")
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThat(appended.get()).isNull();
        assertThat(metrics.rejectionReason).isEqualTo("validation");
    }

    private static final class RecordingMetrics
            implements ObservabilityMetricsPort {
        private SecurityAuditEventType lastEvent;
        private String rejectionReason;

        @Override
        public void administrativeEvent(SecurityAuditEventType type) {
            lastEvent = type;
        }

        @Override
        public void administrativeAccessDenied(String operation) {
        }

        @Override
        public void invitationFailed(String operation, String reasonCode) {
        }

        @Override
        public void auditRejected(String reason) {
            rejectionReason = reason;
        }
    }
}
