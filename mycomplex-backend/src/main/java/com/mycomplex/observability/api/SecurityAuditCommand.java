package com.mycomplex.observability.api;

import java.util.Map;
import java.util.UUID;

public record SecurityAuditCommand(
        SecurityAuditEventType type,
        SecurityAuditResult result,
        UUID actorUserId,
        UUID propertyId,
        UUID targetUserId,
        String origin,
        Map<String, String> metadata
) {
}
