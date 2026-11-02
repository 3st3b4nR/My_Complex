package com.mycomplex.tasks.application.service;

import java.util.List;
import java.util.UUID;

import com.mycomplex.notifications.api.TaskNotificationOutbox;
import com.mycomplex.tasks.application.exception.TaskAccessDeniedException;
import com.mycomplex.tasks.application.exception.TaskNotFoundException;
import com.mycomplex.tasks.application.port.in.FindAdministrativeTasksUseCase;
import com.mycomplex.tasks.application.port.out.AdministrativeTaskPort;
import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.TaskSearchCriteria;
import com.mycomplex.tasks.domain.TaskNotificationStatus;
import com.mycomplex.users.api.TaskAccessDirectory;
import com.mycomplex.users.api.TaskAccessDirectory.Worker;

public final class FindAdministrativeTasksService
        implements FindAdministrativeTasksUseCase {

    private final AdministrativeTaskPort tasks;
    private final TaskAccessDirectory access;
    private final TaskNotificationOutbox notifications;

    public FindAdministrativeTasksService(
            AdministrativeTaskPort tasks,
            TaskAccessDirectory access,
            TaskNotificationOutbox notifications
    ) {
        this.tasks = tasks;
        this.access = access;
        this.notifications = notifications;
    }

    @Override
    public List<AdministrativeTask> find(
            TaskSearchCriteria criteria,
            String auth0Subject
    ) {
        requireAdministrator(criteria.propertyId(), auth0Subject);
        List<AdministrativeTask> found = tasks.find(criteria);
        var statuses = notifications.findLatestStatuses(
                found.stream().map(AdministrativeTask::id).toList()
        );
        return found.stream().map(task -> task.withNotificationStatus(
                status(statuses.get(task.id()))
        )).toList();
    }

    @Override
    public AdministrativeTask findOne(
            UUID propertyId,
            UUID taskId,
            String auth0Subject
    ) {
        requireAdministrator(propertyId, auth0Subject);
        AdministrativeTask task = tasks.findOne(propertyId, taskId)
                .orElseThrow(TaskNotFoundException::new);
        return task.withNotificationStatus(status(
                notifications.findLatestStatuses(List.of(taskId)).get(taskId)
        ));
    }

    @Override
    public List<Worker> findEligibleWorkers(
            UUID propertyId,
            String auth0Subject
    ) {
        requireAdministrator(propertyId, auth0Subject);
        return access.findEligibleWorkers(propertyId);
    }

    private void requireAdministrator(UUID propertyId, String subject) {
        access.findActiveAdministrator(propertyId, subject)
                .orElseThrow(TaskAccessDeniedException::new);
    }

    private TaskNotificationStatus status(
            com.mycomplex.notifications.api.NotificationDeliveryStatus value
    ) {
        return value == null ? null : TaskNotificationStatus.valueOf(
                value.name()
        );
    }
}
