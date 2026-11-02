package com.mycomplex.tasks.application.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.mycomplex.notifications.api.TaskNotificationOutbox;
import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.api.SecurityAuditCommand;
import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.observability.api.SecurityAuditResult;
import com.mycomplex.observability.api.TaskOperationalMetrics;
import com.mycomplex.tasks.application.exception.InvalidTaskException;
import com.mycomplex.tasks.application.exception.TaskAccessDeniedException;
import com.mycomplex.tasks.application.exception.TaskConflictException;
import com.mycomplex.tasks.application.exception.TaskNotFoundException;
import com.mycomplex.tasks.application.port.in.ManageAdministrativeTasksUseCase;
import com.mycomplex.tasks.application.port.out.AdministrativeTaskPort;
import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.CreateAdministrativeTask;
import com.mycomplex.tasks.domain.TaskStatus;
import com.mycomplex.tasks.domain.UpdateAdministrativeTask;
import com.mycomplex.users.api.TaskAccessDirectory;
import com.mycomplex.users.api.TaskAccessDirectory.Administrator;
import com.mycomplex.users.api.TaskAccessDirectory.Worker;

public final class ManageAdministrativeTasksService
        implements ManageAdministrativeTasksUseCase {

    private final AdministrativeTaskPort tasks;
    private final TaskAccessDirectory access;
    private final TaskNotificationOutbox notifications;
    private final RecordSecurityAuditUseCase audit;
    private final TaskOperationalMetrics metrics;

    public ManageAdministrativeTasksService(
            AdministrativeTaskPort tasks,
            TaskAccessDirectory access,
            TaskNotificationOutbox notifications,
            RecordSecurityAuditUseCase audit,
            TaskOperationalMetrics metrics
    ) {
        this.tasks = tasks;
        this.access = access;
        this.notifications = notifications;
        this.audit = audit;
        this.metrics = metrics;
    }

    @Override
    public AdministrativeTask create(
            CreateAdministrativeTask command,
            String auth0Subject
    ) {
        validate(command.title(), command.description(), command.dueAt(),
                command.priority());
        Administrator administrator = administrator(
                command.propertyId(), auth0Subject
        );
        Worker worker = worker(
                command.propertyId(), command.assignedMembershipId()
        );
        AdministrativeTask created = tasks.create(
                command, administrator, worker
        );
        notifications.enqueueAssignment(
                created.id(), worker.userId(), worker.membershipId(), false
        );
        audit(SecurityAuditEventType.TASK_CREATED, administrator,
                created, worker.userId(), Map.of(
                        "priority", created.priority().name()
                ));
        audit(SecurityAuditEventType.TASK_ASSIGNED, administrator,
                created, worker.userId(), Map.of(
                        "assignmentAction", "ASSIGNED"
                ));
        metrics.taskCreated(created.priority().name());
        metrics.taskAssignment(false);
        return created;
    }

    @Override
    public AdministrativeTask update(
            UpdateAdministrativeTask command,
            String auth0Subject
    ) {
        validate(command.title(), command.description(), command.dueAt(),
                command.priority());
        Administrator administrator = administrator(
                command.propertyId(), auth0Subject
        );
        AdministrativeTask before = requireTask(
                command.propertyId(), command.taskId()
        );
        if (before.version() != command.version()) {
            throw new TaskConflictException();
        }
        if (before.status().isTerminal()) {
            throw new InvalidTaskException(
                    "Una tarea finalizada no puede modificarse."
            );
        }
        Worker worker = worker(
                command.propertyId(), command.assignedMembershipId()
        );
        AdministrativeTask updated = tasks.update(command, worker)
                .orElseThrow(TaskConflictException::new);
        boolean reassigned = !before.assignedMembershipId().equals(
                updated.assignedMembershipId()
        );
        if (reassigned) {
            notifications.enqueueAssignment(
                    updated.id(), worker.userId(), worker.membershipId(), true
            );
            audit(SecurityAuditEventType.TASK_REASSIGNED, administrator,
                    updated, worker.userId(), Map.of(
                            "assignmentAction", "REASSIGNED"
                    ));
            metrics.taskAssignment(true);
        }
        audit(SecurityAuditEventType.TASK_UPDATED, administrator,
                updated, worker.userId(), Map.of(
                        "changedField", "task"
                ));
        return updated;
    }

    @Override
    public AdministrativeTask changeStatus(
            UUID propertyId,
            UUID taskId,
            TaskStatus status,
            long version,
            String auth0Subject
    ) {
        Administrator administrator = administrator(propertyId, auth0Subject);
        AdministrativeTask before = requireTask(propertyId, taskId);
        if (before.version() != version) {
            throw new TaskConflictException();
        }
        if (status == null || !before.status().canTransitionTo(status)) {
            throw new InvalidTaskException(
                    "La transición de estado solicitada no es válida."
            );
        }
        AdministrativeTask updated = tasks.changeStatus(
                propertyId, taskId, status, version
        ).orElseThrow(TaskConflictException::new);
        var event = status == TaskStatus.CANCELLED
                ? SecurityAuditEventType.TASK_CANCELLED
                : SecurityAuditEventType.TASK_STATUS_CHANGED;
        audit(event, administrator, updated,
                updated.assignedWorkerUserId(), Map.of(
                        "previousStatus", before.status().name(),
                        "newStatus", status.name()
                ));
        metrics.taskStatusChanged(status.name());
        return updated;
    }

    private Administrator administrator(UUID propertyId, String subject) {
        return access.findActiveAdministrator(propertyId, subject)
                .orElseThrow(TaskAccessDeniedException::new);
    }

    private Worker worker(UUID propertyId, UUID membershipId) {
        return access.findEligibleWorker(propertyId, membershipId)
                .orElseThrow(() -> new InvalidTaskException(
                        "El trabajador seleccionado no está disponible."
                ));
    }

    private AdministrativeTask requireTask(UUID propertyId, UUID taskId) {
        return tasks.findOne(propertyId, taskId)
                .orElseThrow(TaskNotFoundException::new);
    }

    private void validate(String title, String description,
            OffsetDateTime dueAt,
            com.mycomplex.tasks.domain.TaskPriority priority) {
        if (title == null || title.isBlank()
                || description == null || description.isBlank()) {
            throw new InvalidTaskException(
                    "El título y la descripción son obligatorios."
            );
        }
        if (priority == null) {
            throw new InvalidTaskException(
                    "La prioridad es obligatoria."
            );
        }
        if (dueAt == null || !dueAt.isAfter(
                OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new InvalidTaskException(
                    "La fecha límite debe ser posterior al momento actual."
            );
        }
    }

    private void audit(SecurityAuditEventType type,
            Administrator administrator, AdministrativeTask task,
            UUID targetUserId, Map<String, String> metadata) {
        audit.record(new SecurityAuditCommand(
                type, SecurityAuditResult.SUCCESS,
                administrator.userId(), task.propertyId(), targetUserId,
                "tasks", metadata
        ));
    }
}
