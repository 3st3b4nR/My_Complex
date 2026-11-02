package com.mycomplex.tasks.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateAdministrativeTask(
        UUID propertyId,
        UUID taskId,
        String title,
        String description,
        TaskPriority priority,
        String location,
        OffsetDateTime dueAt,
        String administrativeNote,
        UUID assignedMembershipId,
        long version
) { }
