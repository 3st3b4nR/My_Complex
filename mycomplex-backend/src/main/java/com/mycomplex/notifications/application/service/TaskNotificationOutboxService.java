package com.mycomplex.notifications.application.service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.mycomplex.notifications.api.NotificationDeliveryStatus;
import com.mycomplex.notifications.api.TaskNotificationOutbox;
import com.mycomplex.notifications.application.port.out.FcmOutboxPort;
import com.mycomplex.notifications.domain.NotificationEventType;

public final class TaskNotificationOutboxService
        implements TaskNotificationOutbox {

    private final FcmOutboxPort outbox;

    public TaskNotificationOutboxService(FcmOutboxPort outbox) {
        this.outbox = outbox;
    }

    @Override
    public void enqueueAssignment(
            UUID taskId,
            UUID targetUserId,
            UUID assignmentMembershipId,
            boolean reassignment
    ) {
        outbox.enqueue(
                taskId,
                targetUserId,
                assignmentMembershipId,
                reassignment
                        ? NotificationEventType.TASK_REASSIGNED
                        : NotificationEventType.TASK_ASSIGNED
        );
    }

    @Override
    public Map<UUID, NotificationDeliveryStatus> findLatestStatuses(
            Collection<UUID> taskIds
    ) {
        return outbox.findLatestStatuses(taskIds);
    }
}
