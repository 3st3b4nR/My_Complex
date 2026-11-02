package com.mycomplex.notifications.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.mycomplex.notifications.application.exception.FcmInstallationException;

class FcmInstallationPersistenceAdapterTest {

    @Test
    void registersNewInstallationAndRevokesIt() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        var adapter = new FcmInstallationPersistenceAdapter(jdbc);
        UUID userId = UUID.randomUUID();

        var installation = adapter.register(userId, "installation-id");

        assertThat(installation.userId()).isEqualTo(userId);
        assertThat(adapter.revoke(userId, installation.id())).isTrue();
        adapter.revokeByIdentifier("installation-id");
        verify(jdbc, org.mockito.Mockito.times(3))
                .update(anyString(), any(Object[].class));
    }

    @Test
    void refreshesExistingInstallationForSameUser() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id", UUID.class)).thenReturn(id);
        when(resultSet.getObject("usuario_id", UUID.class)).thenReturn(userId);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        var installation = new FcmInstallationPersistenceAdapter(jdbc)
                .register(userId, "installation-id");

        assertThat(installation.id()).isEqualTo(id);
        verify(jdbc).update(anyString(), any(Object[].class));
    }

    @Test
    void rejectsInstallationOwnedByAnotherUserOrConcurrentInsert()
            throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
        when(resultSet.getObject("usuario_id", UUID.class))
                .thenReturn(UUID.randomUUID());
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });
        var adapter = new FcmInstallationPersistenceAdapter(jdbc);
        assertThatThrownBy(() -> adapter.register(
                UUID.randomUUID(), "installation-id"
        )).isInstanceOf(FcmInstallationException.class);

        JdbcTemplate concurrent = mock(JdbcTemplate.class);
        when(concurrent.query(anyString(), any(RowMapper.class),
                any(Object[].class))).thenReturn(List.of());
        when(concurrent.update(anyString(), any(Object[].class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> new FcmInstallationPersistenceAdapter(
                concurrent
        ).register(UUID.randomUUID(), "installation-id"))
                .isInstanceOf(FcmInstallationException.class);
    }

    @Test
    void findsMostRecentDestinationAndSupportsMissingDestination()
            throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        UUID id = UUID.randomUUID();
        when(resultSet.getObject("id", UUID.class)).thenReturn(id);
        when(resultSet.getString("identificador_instalacion"))
                .thenReturn("installation-id");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });
        var adapter = new FcmInstallationPersistenceAdapter(jdbc);

        assertThat(adapter.findMostRecentActive(UUID.randomUUID()))
                .get().extracting(destination -> destination.identifier())
                .isEqualTo("installation-id");

        JdbcTemplate empty = mock(JdbcTemplate.class);
        when(empty.query(anyString(), any(RowMapper.class),
                any(Object[].class))).thenReturn(List.of());
        assertThat(new FcmInstallationPersistenceAdapter(empty)
                .findMostRecentActive(UUID.randomUUID())).isEmpty();
    }
}
