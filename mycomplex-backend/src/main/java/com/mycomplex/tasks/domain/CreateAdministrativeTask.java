package com.mycomplex.tasks.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateAdministrativeTask(
        UUID propertyId,
        String title,
        String description,
        TaskPriority priority,
        String location,
        OffsetDateTime dueAt,
        String administrativeNote,
        UUID assignedMembershipId
) { }
