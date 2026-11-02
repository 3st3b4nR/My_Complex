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
import com.mycomplex.tasks.application.exception.InvalidTaskException;
import com.mycomplex.tasks.application.exception.TaskConflictException;
import com.mycomplex.tasks.application.port.out.AdministrativeTaskPort;
import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.CreateAdministrativeTask;
import com.mycomplex.tasks.domain.TaskPriority;
import com.mycomplex.tasks.domain.TaskStatus;
import com.mycomplex.tasks.domain.UpdateAdministrativeTask;
import com.mycomplex.users.api.TaskAccessDirectory;

class ManageAdministrativeTasksServiceTest {

    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID WORKER_USER_ID = UUID.randomUUID();
    private static final UUID WORKER_MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();

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

    @Test
    void rejectsInvalidTaskDataAndUnavailableWorker() {
        Fixture fixture = fixture();
        CreateAdministrativeTask invalid = new CreateAdministrativeTask(
                PROPERTY_ID, " ", "Descripción", TaskPriority.HIGH, null,
                OffsetDateTime.now().plusDays(1), null,
                WORKER_MEMBERSHIP_ID
        );
        assertThatThrownBy(() -> fixture.service().create(
                invalid, "auth0|admin"
        )).isInstanceOf(InvalidTaskException.class);

        CreateAdministrativeTask missingPriority = new CreateAdministrativeTask(
                PROPERTY_ID, "Título", "Descripción", null, null,
                OffsetDateTime.now().plusDays(1), null,
                WORKER_MEMBERSHIP_ID
        );
        assertThatThrownBy(() -> fixture.service().create(
                missingPriority, "auth0|admin"
        )).isInstanceOf(InvalidTaskException.class);

        CreateAdministrativeTask expired = new CreateAdministrativeTask(
                PROPERTY_ID, "Título", "Descripción", TaskPriority.HIGH,
                null, OffsetDateTime.now().minusMinutes(1), null,
                WORKER_MEMBERSHIP_ID
        );
        assertThatThrownBy(() -> fixture.service().create(
                expired, "auth0|admin"
        )).isInstanceOf(InvalidTaskException.class);

        when(fixture.access().findActiveAdministrator(
                PROPERTY_ID, "auth0|admin"
        )).thenReturn(Optional.of(administrator()));
        when(fixture.access().findEligibleWorker(
                PROPERTY_ID, WORKER_MEMBERSHIP_ID
        )).thenReturn(Optional.empty());
        assertThatThrownBy(() -> fixture.service().create(
                command(), "auth0|admin"
        )).isInstanceOf(InvalidTaskException.class)
                .satisfies(exception -> assertThat(
                        ((InvalidTaskException) exception).safeMessage()
                ).contains("trabajador"));
    }

    @Test
    void updatesAndReassignsTaskWithAuditAndNotification() {
        Fixture fixture = fixture();
        AdministrativeTask before = task();
        UUID newMembership = UUID.randomUUID();
        UUID newUser = UUID.randomUUID();
        var worker = new TaskAccessDirectory.Worker(
                newUser, newMembership, "Carlos Técnico"
        );
        UpdateAdministrativeTask command = update(newMembership, 0);
        AdministrativeTask updated = task(
                TaskStatus.PENDING, newMembership, newUser, 1
        );
        authorize(fixture, before);
        when(fixture.access().findEligibleWorker(PROPERTY_ID, newMembership))
                .thenReturn(Optional.of(worker));
        when(fixture.tasks().update(command, worker))
                .thenReturn(Optional.of(updated));

        assertThat(fixture.service().update(command, "auth0|admin"))
                .isEqualTo(updated);
        verify(fixture.notifications()).enqueueAssignment(
                updated.id(), newUser, newMembership, true
        );
        verify(fixture.metrics()).taskAssignment(true);
        verify(fixture.audit(), org.mockito.Mockito.times(2)).record(any());
    }

    @Test
    void rejectsStaleOrTerminalTaskUpdates() {
        Fixture fixture = fixture();
        authorize(fixture, task());
        assertThatThrownBy(() -> fixture.service().update(
                update(WORKER_MEMBERSHIP_ID, 99), "auth0|admin"
        )).isInstanceOf(TaskConflictException.class);

        when(fixture.tasks().findOne(PROPERTY_ID, task().id()))
                .thenReturn(Optional.of(task(
                        TaskStatus.COMPLETED, WORKER_MEMBERSHIP_ID,
                        WORKER_USER_ID, 0
                )));
        assertThatThrownBy(() -> fixture.service().update(
                update(WORKER_MEMBERSHIP_ID, 0), "auth0|admin"
        )).isInstanceOf(InvalidTaskException.class)
                .satisfies(exception -> assertThat(
                        ((InvalidTaskException) exception).safeMessage()
                ).contains("finalizada"));
    }

    @Test
    void changesAndCancelsStatusAndRejectsInvalidTransition() {
        Fixture fixture = fixture();
        AdministrativeTask before = task();
        AdministrativeTask cancelled = task(
                TaskStatus.CANCELLED, WORKER_MEMBERSHIP_ID,
                WORKER_USER_ID, 1
        );
        authorize(fixture, before);
        when(fixture.tasks().changeStatus(
                PROPERTY_ID, before.id(), TaskStatus.CANCELLED, 0
        )).thenReturn(Optional.of(cancelled));

        assertThat(fixture.service().changeStatus(
                PROPERTY_ID, before.id(), TaskStatus.CANCELLED, 0,
                "auth0|admin"
        )).isEqualTo(cancelled);
        verify(fixture.metrics()).taskStatusChanged("CANCELLED");

        assertThatThrownBy(() -> fixture.service().changeStatus(
                PROPERTY_ID, before.id(), TaskStatus.COMPLETED, 0,
                "auth0|admin"
        )).isInstanceOf(InvalidTaskException.class);
        assertThatThrownBy(() -> fixture.service().changeStatus(
                PROPERTY_ID, before.id(), null, 0, "auth0|admin"
        )).isInstanceOf(InvalidTaskException.class);
        assertThatThrownBy(() -> fixture.service().changeStatus(
                PROPERTY_ID, before.id(), TaskStatus.IN_PROGRESS, 3,
                "auth0|admin"
        )).isInstanceOf(TaskConflictException.class);
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
                TASK_ID, PROPERTY_ID, "Revisar ascensor",
                "Validar funcionamiento", TaskPriority.HIGH,
                TaskStatus.PENDING, "Torre 1", now.plusDays(1), null,
                ADMIN_MEMBERSHIP_ID, WORKER_MEMBERSHIP_ID, WORKER_USER_ID,
                "Ana Trabajadora", null, null, null, now, now, 0, null
        );
    }

    private AdministrativeTask task(TaskStatus status, UUID membershipId,
            UUID userId, long version) {
        AdministrativeTask base = task();
        return new AdministrativeTask(
                base.id(), base.propertyId(), base.title(),
                base.description(), base.priority(), status, base.location(),
                base.dueAt(), base.administrativeNote(),
                base.creatorMembershipId(), membershipId, userId,
                base.assignedWorkerName(), base.startedAt(),
                base.completedAt(), base.cancelledAt(), base.createdAt(),
                base.updatedAt(), version, base.notificationStatus()
        );
    }

    private UpdateAdministrativeTask update(UUID membershipId, long version) {
        return new UpdateAdministrativeTask(
                PROPERTY_ID, TASK_ID, "Revisar ascensor",
                "Validar funcionamiento", TaskPriority.HIGH, "Torre 1",
                OffsetDateTime.now().plusDays(1), null, membershipId, version
        );
    }

    private TaskAccessDirectory.Administrator administrator() {
        return new TaskAccessDirectory.Administrator(
                ADMIN_USER_ID, ADMIN_MEMBERSHIP_ID
        );
    }

    private Fixture fixture() {
        AdministrativeTaskPort tasks = mock(AdministrativeTaskPort.class);
        TaskAccessDirectory access = mock(TaskAccessDirectory.class);
        TaskNotificationOutbox notifications = mock(TaskNotificationOutbox.class);
        RecordSecurityAuditUseCase audit = mock(RecordSecurityAuditUseCase.class);
        TaskOperationalMetrics metrics = mock(TaskOperationalMetrics.class);
        return new Fixture(
                new ManageAdministrativeTasksService(
                        tasks, access, notifications, audit, metrics
                ), tasks, access, notifications, audit, metrics
        );
    }

    private void authorize(Fixture fixture, AdministrativeTask task) {
        when(fixture.access().findActiveAdministrator(
                PROPERTY_ID, "auth0|admin"
        )).thenReturn(Optional.of(administrator()));
        when(fixture.tasks().findOne(PROPERTY_ID, task.id()))
                .thenReturn(Optional.of(task));
    }

    private record Fixture(
            ManageAdministrativeTasksService service,
            AdministrativeTaskPort tasks,
            TaskAccessDirectory access,
            TaskNotificationOutbox notifications,
            RecordSecurityAuditUseCase audit,
            TaskOperationalMetrics metrics
    ) { }
}
