package com.mycomplex.tasks.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import com.mycomplex.tasks.application.port.in.FindAdministrativeTasksUseCase;
import com.mycomplex.tasks.application.port.in.ManageAdministrativeTasksUseCase;
import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.TaskPriority;
import com.mycomplex.tasks.domain.TaskStatus;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.ChangeTaskStatusRequest;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.CreateTaskRequest;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.UpdateTaskRequest;
import com.mycomplex.users.api.TaskAccessDirectory;

class AdministrativeTaskControllerTest {

    @Test
    void delegatesQueriesAndCommandsWithAuthenticatedSubject() {
        FindAdministrativeTasksUseCase query = mock(
                FindAdministrativeTasksUseCase.class
        );
        ManageAdministrativeTasksUseCase commands = mock(
                ManageAdministrativeTasksUseCase.class
        );
        TaskRestMapper mapper = new TaskRestMapper();
        var controller = new AdministrativeTaskController(
                query, commands, mapper
        );
        UUID propertyId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        AdministrativeTask task = task(propertyId, taskId, workerId);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth0|admin");
        when(query.find(any(), org.mockito.ArgumentMatchers.eq("auth0|admin")))
                .thenReturn(List.of(task));
        when(query.findOne(propertyId, taskId, "auth0|admin"))
                .thenReturn(task);
        when(query.findEligibleWorkers(propertyId, "auth0|admin"))
                .thenReturn(List.of(new TaskAccessDirectory.Worker(
                        UUID.randomUUID(), workerId, "Ana"
                )));
        when(commands.create(any(), org.mockito.ArgumentMatchers.eq("auth0|admin")))
                .thenReturn(task);
        when(commands.update(any(), org.mockito.ArgumentMatchers.eq("auth0|admin")))
                .thenReturn(task);
        when(commands.changeStatus(propertyId, taskId,
                TaskStatus.IN_PROGRESS, 0, "auth0|admin"))
                .thenReturn(task);

        assertThat(controller.find(propertyId, "ascensor", "PENDING",
                "HIGH", workerId, true, jwt)).hasSize(1);
        assertThat(controller.find(propertyId, null, " ", "", null,
                false, jwt)).hasSize(1);
        assertThat(controller.findOne(propertyId, taskId, jwt).id())
                .isEqualTo(taskId);
        assertThat(controller.findWorkers(propertyId, jwt))
                .singleElement().satisfies(worker ->
                        assertThat(worker.membershipId()).isEqualTo(workerId));

        OffsetDateTime dueAt = OffsetDateTime.now().plusDays(1);
        assertThat(controller.create(propertyId, new CreateTaskRequest(
                "Título", "Descripción", "HIGH", null, dueAt, null,
                workerId
        ), jwt).id()).isEqualTo(taskId);
        assertThat(controller.update(propertyId, taskId,
                new UpdateTaskRequest(
                        "Título", "Descripción", "HIGH", null, dueAt,
                        null, workerId, 0
                ), jwt).id()).isEqualTo(taskId);
        assertThat(controller.changeStatus(propertyId, taskId,
                new ChangeTaskStatusRequest("IN_PROGRESS", 0), jwt).id())
                .isEqualTo(taskId);
        verify(commands).changeStatus(
                propertyId, taskId, TaskStatus.IN_PROGRESS, 0, "auth0|admin"
        );
    }

    private AdministrativeTask task(UUID propertyId, UUID taskId,
            UUID workerId) {
        OffsetDateTime now = OffsetDateTime.now();
        return new AdministrativeTask(
                taskId, propertyId, "Título", "Descripción",
                TaskPriority.HIGH, TaskStatus.PENDING, null, now.plusDays(1),
                null, UUID.randomUUID(), workerId, UUID.randomUUID(), "Ana",
                null, null, null, now, now, 0, null
        );
    }
}
