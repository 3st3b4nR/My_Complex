package com.mycomplex.notifications.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import com.mycomplex.notifications.api.NotificationDeliveryStatus;
import com.mycomplex.notifications.domain.NotificationEventType;

class FcmOutboxPersistenceAdapterTest {

    @Test
    void enqueuesBothTaskEventTypesAndHandlesEmptyStatusLookup() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var adapter = new FcmOutboxPersistenceAdapter(jdbc);
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();

        adapter.enqueue(taskId, userId, membershipId,
                NotificationEventType.TASK_ASSIGNED);
        adapter.enqueue(taskId, userId, membershipId,
                NotificationEventType.TASK_REASSIGNED);

        verify(jdbc, org.mockito.Mockito.times(2))
                .update(anyString(), any(Object[].class));
        assertThat(adapter.findLatestStatuses(null)).isEmpty();
        assertThat(adapter.findLatestStatuses(List.of())).isEmpty();
    }

    @Test
    void mapsLatestDatabaseDeliveryStatuses() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        UUID taskId = UUID.randomUUID();
        when(resultSet.getObject("tarea_id", UUID.class)).thenReturn(taskId);
        when(resultSet.getString("estado"))
                .thenReturn("PENDIENTE", "ENVIADA", "FALLIDA", "SIN_DESTINO");
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            handler.processRow(resultSet);
            handler.processRow(resultSet);
            handler.processRow(resultSet);
            handler.processRow(resultSet);
            return null;
        }).when(jdbc).query(anyString(), any(RowCallbackHandler.class),
                any(Object[].class));

        var statuses = new FcmOutboxPersistenceAdapter(jdbc)
                .findLatestStatuses(List.of(taskId));

        assertThat(statuses).containsEntry(
                taskId, NotificationDeliveryStatus.NO_DESTINATION
        );
    }

    @Test
    void claimsAssignedAndReassignedEventsAndRejectsUnknownType()
            throws Exception {
        UUID eventId = UUID.randomUUID();
        ResultSet resultSet = eventResultSet(eventId);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(resultSet.getString("tipo_evento"))
                .thenReturn("TAREA_ASIGNADA", "TAREA_REASIGNADA", "UNKNOWN");
        when(jdbc.query(anyString(), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });
        var adapter = new FcmOutboxPersistenceAdapter(jdbc);

        assertThat(adapter.claimNext()).get().satisfies(event ->
                assertThat(event.eventType())
                        .isEqualTo(NotificationEventType.TASK_ASSIGNED));
        assertThat(adapter.claimNext()).get().satisfies(event ->
                assertThat(event.eventType())
                        .isEqualTo(NotificationEventType.TASK_REASSIGNED));
        assertThatThrownBy(adapter::claimNext)
                .isInstanceOf(IllegalArgumentException.class);

        JdbcTemplate empty = mock(JdbcTemplate.class);
        when(empty.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of());
        assertThat(new FcmOutboxPersistenceAdapter(empty).claimNext()).isEmpty();
    }

    @Test
    void updatesSentRetryAndTerminalOutcomesWithSafeLengths() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var adapter = new FcmOutboxPersistenceAdapter(jdbc);
        UUID eventId = UUID.randomUUID();
        String longReason = "x".repeat(200);

        adapter.markSent(eventId, " ");
        adapter.markSent(eventId, "m".repeat(300));
        adapter.markNoDestination(eventId, longReason);
        adapter.markFailed(eventId, "permanent");
        adapter.scheduleRetry(eventId, 2, Duration.ofSeconds(4), longReason);

        verify(jdbc, org.mockito.Mockito.times(5))
                .update(anyString(), any(Object[].class));
    }

    @Test
    void rejectsUnsupportedDatabaseStatus() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("tarea_id", UUID.class))
                .thenReturn(UUID.randomUUID());
        when(resultSet.getString("estado")).thenReturn("UNKNOWN");
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            handler.processRow(resultSet);
            return null;
        }).when(jdbc).query(anyString(), any(RowCallbackHandler.class),
                any(Object[].class));

        assertThatThrownBy(() -> new FcmOutboxPersistenceAdapter(jdbc)
                .findLatestStatuses(List.of(UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ResultSet eventResultSet(UUID eventId) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id", UUID.class)).thenReturn(eventId);
        when(resultSet.getObject("tarea_id", UUID.class))
                .thenReturn(UUID.randomUUID());
        when(resultSet.getObject("usuario_objetivo_id", UUID.class))
                .thenReturn(UUID.randomUUID());
        when(resultSet.getObject("vinculacion_asignada_id", UUID.class))
                .thenReturn(UUID.randomUUID());
        when(resultSet.getInt("intentos")).thenReturn(1);
        when(resultSet.getObject("proximo_intento_at", OffsetDateTime.class))
                .thenReturn(OffsetDateTime.now());
        return resultSet;
    }
}
