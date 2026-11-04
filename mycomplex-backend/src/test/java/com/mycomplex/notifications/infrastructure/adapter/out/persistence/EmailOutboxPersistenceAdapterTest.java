package com.mycomplex.notifications.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.mycomplex.notifications.application.model.EmailNotification;
import com.mycomplex.notifications.domain.EmailOutboxEvent;

class EmailOutboxPersistenceAdapterTest {
    @Test
    void enqueuesEveryRecipientWithAnIdempotencyKey() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        EmailOutboxPersistenceAdapter adapter =
                new EmailOutboxPersistenceAdapter(jdbc);

        adapter.enqueue(new EmailNotification(
                Set.of("one@example.com", "two@example.com"),
                "Aviso", "Contenido", "delivery-1"));

        verify(jdbc, org.mockito.Mockito.times(2)).update(
                anyString(), any(UUID.class), anyString(),
                org.mockito.ArgumentMatchers.eq("Aviso"),
                org.mockito.ArgumentMatchers.eq("Contenido"),
                org.mockito.ArgumentMatchers.eq("delivery-1"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void claimsTheNextEventOrReturnsEmpty() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        EmailOutboxPersistenceAdapter adapter =
                new EmailOutboxPersistenceAdapter(jdbc);
        EmailOutboxEvent event = new EmailOutboxEvent(
                UUID.randomUUID(), "owner@example.com", "Aviso", "Contenido",
                "delivery-1", 0, OffsetDateTime.now());
        when(jdbc.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(event), List.of());

        assertThat(adapter.claimNext()).contains(event);
        assertThat(adapter.claimNext()).isEmpty();
    }

    @Test
    void marksSentFailedAndSchedulesSanitizedRetries() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        EmailOutboxPersistenceAdapter adapter =
                new EmailOutboxPersistenceAdapter(jdbc);
        UUID eventId = UUID.randomUUID();

        adapter.markSent(eventId, "  external-id  ");
        verify(jdbc).update(anyString(),
                org.mockito.ArgumentMatchers.eq("external-id"),
                org.mockito.ArgumentMatchers.eq(eventId));

        adapter.markFailed(eventId, "x".repeat(130));
        verify(jdbc).update(anyString(),
                org.mockito.ArgumentMatchers.eq("x".repeat(120)),
                org.mockito.ArgumentMatchers.eq(eventId));

        adapter.scheduleRetry(eventId, 2, Duration.ofMinutes(1), "  retry  ");
        verify(jdbc).update(anyString(),
                org.mockito.ArgumentMatchers.eq(2),
                any(java.sql.Timestamp.class),
                org.mockito.ArgumentMatchers.eq("retry"),
                org.mockito.ArgumentMatchers.eq(eventId));

        adapter.markSent(eventId, " ");
        verify(jdbc).update(anyString(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(eventId));
    }
}
