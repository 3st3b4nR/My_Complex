package com.mycomplex.tasks.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.tasks.application.exception.InvalidTaskException;
import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.TaskNotificationStatus;
import com.mycomplex.tasks.domain.TaskPriority;
import com.mycomplex.tasks.domain.TaskStatus;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.CreateTaskRequest;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.UpdateTaskRequest;

class TaskRestMapperTest {

    private final TaskRestMapper mapper = new TaskRestMapper();

    @Test
    void mapsCreateAndUpdateRequests() {
        UUID propertyId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        OffsetDateTime dueAt = OffsetDateTime.now().plusDays(1);
        var create = new CreateTaskRequest(
                "Título", "Descripción", "HIGH", "Torre 1", dueAt,
                "Nota", workerId
        );
        var update = new UpdateTaskRequest(
                "Título", "Descripción", "URGENT", "Torre 2", dueAt,
                "Nota", workerId, 4
        );

        assertThat(mapper.toDomain(propertyId, create).priority())
                .isEqualTo(TaskPriority.HIGH);
        assertThat(mapper.toDomain(propertyId, taskId, update).version())
                .isEqualTo(4);
    }

    @Test
    void mapsOverdueAndTerminalResponsesWithNotificationStatus() {
        AdministrativeTask overdue = task(
                TaskStatus.PENDING, OffsetDateTime.now().minusMinutes(1),
                TaskNotificationStatus.SENT
        );
        AdministrativeTask terminal = task(
                TaskStatus.COMPLETED, OffsetDateTime.now().minusDays(1), null
        );

        assertThat(mapper.toResponse(overdue).overdue()).isTrue();
        assertThat(mapper.toResponse(overdue).notificationStatus())
                .isEqualTo("SENT");
        assertThat(mapper.toResponse(terminal).overdue()).isFalse();
        assertThat(mapper.toResponse(terminal).notificationStatus()).isNull();
    }

    @Test
    void validatesPriorityAndStatusValues() {
        assertThat(mapper.priority("LOW")).isEqualTo(TaskPriority.LOW);
        assertThat(mapper.status("IN_PROGRESS"))
                .isEqualTo(TaskStatus.IN_PROGRESS);
        assertThatThrownBy(() -> mapper.priority("INVALID"))
                .isInstanceOf(InvalidTaskException.class);
        assertThatThrownBy(() -> mapper.status(null))
                .isInstanceOf(InvalidTaskException.class);
    }

    private AdministrativeTask task(TaskStatus status, OffsetDateTime dueAt,
            TaskNotificationStatus notificationStatus) {
        OffsetDateTime now = OffsetDateTime.now();
        return new AdministrativeTask(
                UUID.randomUUID(), UUID.randomUUID(), "Título",
                "Descripción", TaskPriority.MEDIUM, status, null, dueAt,
                null, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Ana", null, status == TaskStatus.COMPLETED ? now : null,
                null, now, now, 1, notificationStatus
        );
    }
}
