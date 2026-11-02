package com.mycomplex.tasks.infrastructure.adapter.in.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID propertyId,
        String title,
        String description,
        String priority,
        String status,
        String location,
        OffsetDateTime dueAt,
        boolean overdue,
        String administrativeNote,
        UUID assignedMembershipId,
        UUID assignedWorkerUserId,
        String assignedWorkerName,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long version,
        String notificationStatus
) { }
