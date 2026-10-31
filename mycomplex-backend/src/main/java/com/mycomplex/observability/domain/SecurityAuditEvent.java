package com.mycomplex.observability.domain;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.observability.api.SecurityAuditResult;

public record SecurityAuditEvent(
        UUID id,
        OffsetDateTime occurredAt,
        SecurityAuditEventType type,
        SecurityAuditResult result,
        UUID actorUserId,
        UUID propertyId,
        UUID targetUserId,
        String correlationId,
        String origin,
        Map<String, String> metadata
) {
}
