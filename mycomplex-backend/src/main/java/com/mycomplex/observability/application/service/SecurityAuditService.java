package com.mycomplex.observability.application.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.api.SecurityAuditCommand;
import com.mycomplex.observability.application.port.out.AppendSecurityAuditEventPort;
import com.mycomplex.observability.application.port.out.CurrentRequestContextPort;
import com.mycomplex.observability.application.port.out.ObservabilityMetricsPort;
import com.mycomplex.observability.domain.SecurityAuditEvent;

public final class SecurityAuditService implements RecordSecurityAuditUseCase {

    private static final Set<String> ALLOWED_METADATA_KEYS = Set.of(
            "registrationMode", "changedField", "invitationMode"
    );
    private static final int MAX_METADATA_VALUE_LENGTH = 80;

    private final AppendSecurityAuditEventPort appendPort;
    private final CurrentRequestContextPort requestContextPort;
    private final ObservabilityMetricsPort metricsPort;
    private final Clock clock;

    public SecurityAuditService(
            AppendSecurityAuditEventPort appendPort,
            CurrentRequestContextPort requestContextPort,
            ObservabilityMetricsPort metricsPort,
            Clock clock
    ) {
        this.appendPort = appendPort;
        this.requestContextPort = requestContextPort;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Override
    public void record(SecurityAuditCommand command) {
        try {
            validate(command);
            SecurityAuditEvent event = new SecurityAuditEvent(
                    UUID.randomUUID(),
                    OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
                    command.type(), command.result(), command.actorUserId(),
                    command.propertyId(), command.targetUserId(),
                    requestContextPort.correlationId(), command.origin(),
                    Map.copyOf(command.metadata())
            );
            appendPort.append(event);
            metricsPort.administrativeEvent(command.type());
        } catch (RuntimeException exception) {
            metricsPort.auditRejected(
                    exception instanceof IllegalArgumentException
                            ? "validation" : "persistence"
            );
            throw exception;
        }
    }

    private void validate(SecurityAuditCommand command) {
        if (command == null || command.type() == null
                || command.result() == null || command.origin() == null
                || command.origin().isBlank() || command.origin().length() > 80
                || command.metadata() == null) {
            throw new IllegalArgumentException("Invalid security audit event");
        }
        command.metadata().forEach((key, value) -> {
            if (!ALLOWED_METADATA_KEYS.contains(key) || value == null
                    || value.isBlank()
                    || value.length() > MAX_METADATA_VALUE_LENGTH) {
                throw new IllegalArgumentException(
                        "Security audit metadata is not allowed"
                );
            }
        });
    }
}
