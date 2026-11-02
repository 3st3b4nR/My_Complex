package com.mycomplex.tasks.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.mycomplex.tasks.domain.CreateAdministrativeTask;
import com.mycomplex.tasks.domain.TaskPriority;
import com.mycomplex.tasks.domain.TaskSearchCriteria;
import com.mycomplex.tasks.domain.TaskStatus;
import com.mycomplex.tasks.domain.UpdateAdministrativeTask;
import com.mycomplex.users.api.TaskAccessDirectory.Administrator;
import com.mycomplex.users.api.TaskAccessDirectory.Worker;

class AdministrativeTaskJdbcAdapterTest {

    @Test
    void appliesEverySearchFilterAndMapsDatabaseEnums() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet resultSet = resultSet("URGENTE", "EN_PROGRESO");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });
        var adapter = new AdministrativeTaskJdbcAdapter(jdbc);
        UUID propertyId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();

        assertThat(adapter.find(new TaskSearchCriteria(
                propertyId, "  ASCENSOR ", TaskStatus.IN_PROGRESS,
                TaskPriority.URGENT, membershipId, true
        ))).singleElement().satisfies(task -> {
            assertThat(task.priority()).isEqualTo(TaskPriority.URGENT);
            assertThat(task.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        });
        assertThat(adapter.find(new TaskSearchCriteria(
                propertyId, " ", null, null, null, false
        ))).hasSize(1);
    }

    @Test
    void coversCreateUpdateAndEveryStatusDatabaseValue() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet resultSet = resultSet("ALTA", "PENDIENTE");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        var adapter = new AdministrativeTaskJdbcAdapter(jdbc);
        UUID propertyId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        Worker worker = new Worker(
                UUID.randomUUID(), membershipId, "Ana Trabajadora"
        );
        Administrator administrator = new Administrator(
                UUID.randomUUID(), UUID.randomUUID()
        );
        OffsetDateTime dueAt = OffsetDateTime.now().plusDays(1);

        for (TaskPriority priority : TaskPriority.values()) {
            assertThat(adapter.create(new CreateAdministrativeTask(
                    propertyId, "  Título  ", "  Descripción  ", priority,
                    " ", dueAt, "  Nota  ", membershipId
            ), administrator, worker)).isNotNull();
        }

        UUID taskId = UUID.randomUUID();
        var update = new UpdateAdministrativeTask(
                propertyId, taskId, "  Título  ", "  Descripción  ",
                TaskPriority.MEDIUM, "  Torre 1  ", dueAt, " ",
                membershipId, 2
        );
        assertThat(adapter.update(update, worker)).isPresent();
        for (TaskStatus status : TaskStatus.values()) {
            assertThat(adapter.changeStatus(
                    propertyId, taskId, status, 2
            )).isPresent();
        }

        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
        assertThat(adapter.update(update, worker)).isEmpty();
        assertThat(adapter.changeStatus(
                propertyId, taskId, TaskStatus.CANCELLED, 2
        )).isEmpty();
    }

    @Test
    void mapsAllStoredPrioritiesAndStatusesAndRejectsUnknownValues()
            throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet resultSet = resultSet("BAJA", "PENDIENTE");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });
        var adapter = new AdministrativeTaskJdbcAdapter(jdbc);
        String[] priorities = { "BAJA", "MEDIA", "ALTA", "URGENTE" };
        String[] statuses = {
            "PENDIENTE", "EN_PROGRESO", "COMPLETADA", "CANCELADA"
        };
        for (int index = 0; index < priorities.length; index++) {
            when(resultSet.getString("prioridad")).thenReturn(priorities[index]);
            when(resultSet.getString("estado")).thenReturn(statuses[index]);
            assertThat(adapter.findOne(UUID.randomUUID(), UUID.randomUUID()))
                    .isPresent();
        }
        when(resultSet.getString("prioridad")).thenReturn("UNKNOWN");
        assertThatThrownBy(() -> adapter.findOne(
                UUID.randomUUID(), UUID.randomUUID()
        )).isInstanceOf(IllegalArgumentException.class);
        when(resultSet.getString("prioridad")).thenReturn("BAJA");
        when(resultSet.getString("estado")).thenReturn("UNKNOWN");
        assertThatThrownBy(() -> adapter.findOne(
                UUID.randomUUID(), UUID.randomUUID()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private ResultSet resultSet(String priority, String status)
            throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject(anyString(),
                org.mockito.ArgumentMatchers.eq(UUID.class)))
                .thenReturn(UUID.randomUUID());
        when(resultSet.getString("titulo")).thenReturn("Título");
        when(resultSet.getString("descripcion")).thenReturn("Descripción");
        when(resultSet.getString("prioridad")).thenReturn(priority);
        when(resultSet.getString("estado")).thenReturn(status);
        when(resultSet.getString("trabajador_nombre")).thenReturn("Ana");
        when(resultSet.getObject(anyString(),
                org.mockito.ArgumentMatchers.eq(OffsetDateTime.class)))
                .thenReturn(OffsetDateTime.now());
        when(resultSet.getLong("version")).thenReturn(1L);
        return resultSet;
    }
}
