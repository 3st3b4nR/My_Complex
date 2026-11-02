package com.mycomplex.tasks.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.notifications.api.TaskNotificationOutbox;
import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.api.TaskOperationalMetrics;
import com.mycomplex.tasks.application.exception.TaskAccessDeniedException;
import com.mycomplex.tasks.application.port.out.AdministrativeTaskPort;
import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.CreateAdministrativeTask;
import com.mycomplex.tasks.domain.TaskPriority;
import com.mycomplex.tasks.domain.TaskStatus;
import com.mycomplex.users.api.TaskAccessDirectory;

class ManageAdministrativeTasksServiceTest {

    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID WORKER_USER_ID = UUID.randomUUID();
    private static final UUID WORKER_MEMBERSHIP_ID = UUID.randomUUID();

    @Test
    void createsTaskAndEnqueuesAssignmentForSamePropertyWorker() {
        AdministrativeTaskPort tasks = mock(AdministrativeTaskPort.class);
        TaskAccessDirectory access = mock(TaskAccessDirectory.class);
        TaskNotificationOutbox notifications =
                mock(TaskNotificationOutbox.class);
        RecordSecurityAuditUseCase audit =
                mock(RecordSecurityAuditUseCase.class);
        TaskOperationalMetrics metrics = mock(TaskOperationalMetrics.class);
        var administrator = new TaskAccessDirectory.Administrator(
                ADMIN_USER_ID, ADMIN_MEMBERSHIP_ID
        );
        var worker = new TaskAccessDirectory.Worker(
                WORKER_USER_ID, WORKER_MEMBERSHIP_ID, "Ana Trabajadora"
        );
        var command = command();
        var created = task();
        when(access.findActiveAdministrator(PROPERTY_ID, "auth0|admin"))
                .thenReturn(Optional.of(administrator));
        when(access.findEligibleWorker(PROPERTY_ID, WORKER_MEMBERSHIP_ID))
                .thenReturn(Optional.of(worker));
        when(tasks.create(command, administrator, worker))
                .thenReturn(created);

        var service = new ManageAdministrativeTasksService(
                tasks, access, notifications, audit, metrics
        );

        assertThat(service.create(command, "auth0|admin")).isEqualTo(created);
        verify(notifications).enqueueAssignment(
                created.id(), WORKER_USER_ID, WORKER_MEMBERSHIP_ID, false
        );
        verify(audit, org.mockito.Mockito.times(2)).record(any());
        verify(metrics).taskCreated("HIGH");
        verify(metrics).taskAssignment(false);
    }

    @Test
    void rejectsSubjectWithoutAdministratorMembershipBeforeWriting() {
        AdministrativeTaskPort tasks = mock(AdministrativeTaskPort.class);
        TaskAccessDirectory access = mock(TaskAccessDirectory.class);
        when(access.findActiveAdministrator(PROPERTY_ID, "auth0|other"))
                .thenReturn(Optional.empty());
        var service = new ManageAdministrativeTasksService(
                tasks, access, mock(TaskNotificationOutbox.class),
                mock(RecordSecurityAuditUseCase.class),
                mock(TaskOperationalMetrics.class)
        );

        assertThatThrownBy(() -> service.create(command(), "auth0|other"))
                .isInstanceOf(TaskAccessDeniedException.class);
        verify(tasks, never()).create(any(), any(), any());
    }

    private CreateAdministrativeTask command() {
        return new CreateAdministrativeTask(
                PROPERTY_ID, "Revisar ascensor", "Validar funcionamiento",
                TaskPriority.HIGH, "Torre 1",
                OffsetDateTime.now().plusDays(1), null,
                WORKER_MEMBERSHIP_ID
        );
    }

    private AdministrativeTask task() {
        OffsetDateTime now = OffsetDateTime.now();
        return new AdministrativeTask(
                UUID.randomUUID(), PROPERTY_ID, "Revisar ascensor",
                "Validar funcionamiento", TaskPriority.HIGH,
                TaskStatus.PENDING, "Torre 1", now.plusDays(1), null,
                ADMIN_MEMBERSHIP_ID, WORKER_MEMBERSHIP_ID, WORKER_USER_ID,
                "Ana Trabajadora", null, null, null, now, now, 0, null
        );
    }
}
