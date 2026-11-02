package com.mycomplex.tasks.infrastructure.adapter.in.rest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.CreateAdministrativeTask;
import com.mycomplex.tasks.domain.TaskPriority;
import com.mycomplex.tasks.domain.TaskStatus;
import com.mycomplex.tasks.domain.UpdateAdministrativeTask;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.CreateTaskRequest;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.TaskResponse;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.UpdateTaskRequest;

@Component
public class TaskRestMapper {

    public CreateAdministrativeTask toDomain(
            UUID propertyId,
            CreateTaskRequest request
    ) {
        return new CreateAdministrativeTask(
                propertyId, request.title(), request.description(),
                priority(request.priority()), request.location(),
                request.dueAt(), request.administrativeNote(),
                request.assignedMembershipId()
        );
    }

    public UpdateAdministrativeTask toDomain(
            UUID propertyId,
            UUID taskId,
            UpdateTaskRequest request
    ) {
        return new UpdateAdministrativeTask(
                propertyId, taskId, request.title(), request.description(),
                priority(request.priority()), request.location(),
                request.dueAt(), request.administrativeNote(),
                request.assignedMembershipId(), request.version()
        );
    }

    public TaskResponse toResponse(AdministrativeTask task) {
        boolean overdue = !task.status().isTerminal()
                && task.dueAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC));
        return new TaskResponse(
                task.id(), task.propertyId(), task.title(), task.description(),
                task.priority().name(), task.status().name(), task.location(),
                task.dueAt(), overdue, task.administrativeNote(),
                task.assignedMembershipId(), task.assignedWorkerUserId(),
                task.assignedWorkerName(), task.startedAt(),
                task.completedAt(), task.cancelledAt(), task.createdAt(),
                task.updatedAt(), task.version(),
                task.notificationStatus() == null ? null
                        : task.notificationStatus().name()
        );
    }

    public TaskPriority priority(String value) {
        try {
            return TaskPriority.valueOf(value);
        } catch (RuntimeException exception) {
            throw new com.mycomplex.tasks.application.exception.InvalidTaskException(
                    "La prioridad solicitada no es válida."
            );
        }
    }

    public TaskStatus status(String value) {
        try {
            return TaskStatus.valueOf(value);
        } catch (RuntimeException exception) {
            throw new com.mycomplex.tasks.application.exception.InvalidTaskException(
                    "El estado solicitado no es válido."
            );
        }
    }
}
