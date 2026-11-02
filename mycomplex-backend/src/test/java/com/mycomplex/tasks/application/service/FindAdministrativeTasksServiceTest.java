package com.mycomplex.tasks.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.notifications.api.NotificationDeliveryStatus;
import com.mycomplex.notifications.api.TaskNotificationOutbox;
import com.mycomplex.tasks.application.exception.TaskAccessDeniedException;
import com.mycomplex.tasks.application.exception.TaskNotFoundException;
import com.mycomplex.tasks.application.port.out.AdministrativeTaskPort;
import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.TaskPriority;
import com.mycomplex.tasks.domain.TaskSearchCriteria;
import com.mycomplex.tasks.domain.TaskStatus;
import com.mycomplex.users.api.TaskAccessDirectory;

class FindAdministrativeTasksServiceTest {

    private static final UUID PROPERTY_ID = UUID.randomUUID();

    @Test
    void findsTasksAndMapsLatestNotificationStatus() {
        Fixture fixture = fixture(true);
        AdministrativeTask task = task();
        TaskSearchCriteria criteria = new TaskSearchCriteria(
                PROPERTY_ID, "ascensor", TaskStatus.PENDING,
                TaskPriority.HIGH, null, false
        );
        when(fixture.tasks().find(criteria)).thenReturn(List.of(task));
        when(fixture.notifications().findLatestStatuses(List.of(task.id())))
                .thenReturn(Map.of(task.id(), NotificationDeliveryStatus.SENT));

        List<AdministrativeTask> result = fixture.service().find(
                criteria, "auth0|admin"
        );

        assertThat(result).singleElement().satisfies(found ->
                assertThat(found.notificationStatus().name()).isEqualTo("SENT"));
    }

    @Test
    void findsOneWithoutNotificationAndListsEligibleWorkers() {
        Fixture fixture = fixture(true);
        AdministrativeTask task = task();
        var worker = new TaskAccessDirectory.Worker(
                UUID.randomUUID(), UUID.randomUUID(), "Ana Trabajadora"
        );
        when(fixture.tasks().findOne(PROPERTY_ID, task.id()))
                .thenReturn(Optional.of(task));
        when(fixture.notifications().findLatestStatuses(List.of(task.id())))
                .thenReturn(Map.of());
        when(fixture.access().findEligibleWorkers(PROPERTY_ID))
                .thenReturn(List.of(worker));

        assertThat(fixture.service().findOne(
                PROPERTY_ID, task.id(), "auth0|admin"
        ).notificationStatus()).isNull();
        assertThat(fixture.service().findEligibleWorkers(
                PROPERTY_ID, "auth0|admin"
        )).containsExactly(worker);
        verify(fixture.access()).findEligibleWorkers(PROPERTY_ID);
    }

    @Test
    void rejectsUnauthorizedOrMissingTask() {
        Fixture denied = fixture(false);
        assertThatThrownBy(() -> denied.service().find(
                new TaskSearchCriteria(PROPERTY_ID, null, null, null, null,
                        false),
                "auth0|other"
        )).isInstanceOf(TaskAccessDeniedException.class);

        Fixture allowed = fixture(true);
        UUID missingId = UUID.randomUUID();
        when(allowed.tasks().findOne(PROPERTY_ID, missingId))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> allowed.service().findOne(
                PROPERTY_ID, missingId, "auth0|admin"
        )).isInstanceOf(TaskNotFoundException.class);
    }

    private Fixture fixture(boolean authorized) {
        AdministrativeTaskPort tasks = mock(AdministrativeTaskPort.class);
        TaskAccessDirectory access = mock(TaskAccessDirectory.class);
        TaskNotificationOutbox notifications = mock(TaskNotificationOutbox.class);
        when(access.findActiveAdministrator(PROPERTY_ID,
                authorized ? "auth0|admin" : "auth0|other"))
                .thenReturn(authorized
                        ? Optional.of(new TaskAccessDirectory.Administrator(
                                UUID.randomUUID(), UUID.randomUUID()))
                        : Optional.empty());
        return new Fixture(
                new FindAdministrativeTasksService(tasks, access, notifications),
                tasks, access, notifications
        );
    }

    private AdministrativeTask task() {
        OffsetDateTime now = OffsetDateTime.now();
        return new AdministrativeTask(
                UUID.randomUUID(), PROPERTY_ID, "Revisar ascensor",
                "Validar funcionamiento", TaskPriority.HIGH,
                TaskStatus.PENDING, null, now.plusDays(1), null,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Ana Trabajadora", null, null, null, now, now, 0, null
        );
    }

    private record Fixture(
            FindAdministrativeTasksService service,
            AdministrativeTaskPort tasks,
            TaskAccessDirectory access,
            TaskNotificationOutbox notifications
    ) { }
}
