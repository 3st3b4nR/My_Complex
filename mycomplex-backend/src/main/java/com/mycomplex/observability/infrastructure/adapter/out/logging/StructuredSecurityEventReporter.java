package com.mycomplex.observability.infrastructure.adapter.out.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mycomplex.observability.api.SecurityEventReporter;
import com.mycomplex.observability.application.port.out.ObservabilityMetricsPort;

@Component
public class StructuredSecurityEventReporter implements SecurityEventReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            StructuredSecurityEventReporter.class
    );

    private final ObservabilityMetricsPort metricsPort;

    public StructuredSecurityEventReporter(
            ObservabilityMetricsPort metricsPort
    ) {
        this.metricsPort = metricsPort;
    }

    @Override
    public void administrativeAccessDenied(String operation) {
        metricsPort.administrativeAccessDenied(operation);
        LOGGER.atWarn()
                .addKeyValue("security.event", "administrative_access_denied")
                .addKeyValue("operation", operation)
                .log("Administrative access denied");
    }

    @Override
    public void invitationFailed(String operation, String reasonCode) {
        metricsPort.invitationFailed(operation, reasonCode);
        LOGGER.atWarn()
                .addKeyValue("security.event", "invitation_failed")
                .addKeyValue("operation", operation)
                .addKeyValue("reason", safeReason(reasonCode))
                .log("Member invitation failed");
    }

    private String safeReason(String value) {
        return switch (value == null ? "" : value) {
            case "AUTH0_RATE_LIMITED", "AUTH0_UNAVAILABLE",
                    "AUTH0_INVITATION_UNAVAILABLE",
                    "AUTH0_PERMISSION_DENIED" -> value;
            default -> "OTHER";
        };
    }
}
