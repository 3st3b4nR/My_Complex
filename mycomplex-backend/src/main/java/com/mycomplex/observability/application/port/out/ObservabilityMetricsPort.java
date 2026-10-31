package com.mycomplex.observability.application.port.out;

import com.mycomplex.observability.api.SecurityAuditEventType;

public interface ObservabilityMetricsPort {

    void administrativeEvent(SecurityAuditEventType type);

    void administrativeAccessDenied(String operation);

    void invitationFailed(String operation, String reasonCode);

    void auditRejected(String reason);
}
