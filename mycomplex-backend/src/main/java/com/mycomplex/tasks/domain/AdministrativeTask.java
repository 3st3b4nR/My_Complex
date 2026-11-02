package com.mycomplex.tasks.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdministrativeTask(
        UUID id,
        UUID propertyId,
        String title,
        String description,
        TaskPriority priority,
        TaskStatus status,
        String location,
        OffsetDateTime dueAt,
        String administrativeNote,
        UUID creatorMembershipId,
        UUID assignedMembershipId,
        UUID assignedWorkerUserId,
        String assignedWorkerName,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long version,
        TaskNotificationStatus notificationStatus
) {
    public AdministrativeTask withNotificationStatus(
            TaskNotificationStatus value
    ) {
        return new AdministrativeTask(
                id, propertyId, title, description, priority, status,
                location, dueAt, administrativeNote, creatorMembershipId,
                assignedMembershipId, assignedWorkerUserId,
                assignedWorkerName, startedAt, completedAt, cancelledAt,
                createdAt, updatedAt, version, value
        );
    }
}
