package com.mycomplex.notifications.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface TaskNotificationOutbox {

    void enqueueAssignment(
            UUID taskId,
            UUID targetUserId,
            UUID assignmentMembershipId,
            boolean reassignment
    );

    Map<UUID, NotificationDeliveryStatus> findLatestStatuses(
            Collection<UUID> taskIds
    );
}
