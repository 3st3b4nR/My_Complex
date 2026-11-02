package com.mycomplex.tasks.domain;

import java.util.UUID;

public record TaskSearchCriteria(
        UUID propertyId,
        String query,
        TaskStatus status,
        TaskPriority priority,
        UUID assignedMembershipId,
        boolean overdueOnly
) { }
