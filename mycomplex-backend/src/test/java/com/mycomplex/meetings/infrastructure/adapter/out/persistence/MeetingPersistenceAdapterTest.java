package com.mycomplex.meetings.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mycomplex.meetings.domain.model.Meeting;
import com.mycomplex.meetings.infrastructure.adapter.out.persistence.entity.MeetingEntity;
import com.mycomplex.meetings.infrastructure.adapter.out.persistence.mapper.MeetingPersistenceMapper;
import com.mycomplex.meetings.infrastructure.adapter.out.persistence.repository.MeetingJpaRepository;

class MeetingPersistenceAdapterTest {
    private static final Instant NOW = Instant.parse("2026-11-03T15:00:00Z");
    private MeetingJpaRepository repository;
    private MeetingPersistenceMapper mapper;
    private MeetingPersistenceAdapter adapter;
    private UUID propertyId;
    private UUID meetingId;
    private Meeting meeting;
    private MeetingEntity entity;

    @BeforeEach
    void setUp() {
        repository = mock(MeetingJpaRepository.class);
        mapper = mock(MeetingPersistenceMapper.class);
        adapter = new MeetingPersistenceAdapter(
                repository, mapper, Clock.fixed(NOW, ZoneOffset.UTC));
        propertyId = UUID.randomUUID();
        meetingId = UUID.randomUUID();
        meeting = Meeting.schedule(
                meetingId, propertyId, UUID.randomUUID(), "Asamblea",
                "Presupuesto", URI.create("https://meet.example/1"),
                NOW.plusSeconds(7200), NOW);
        entity = mock(MeetingEntity.class);
        when(mapper.toEntity(meeting)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(meeting);
    }

    @Test
    void savesAndMapsTheMeeting() {
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        assertThat(adapter.save(meeting)).isEqualTo(meeting);
    }

    @Test
    void loadsAndUpdatesAnActiveMeetingWithinTheProperty() {
        when(repository.findByIdAndPropertyIdAndDeletedAtIsNull(
                meetingId, propertyId)).thenReturn(Optional.of(entity));
        assertThat(adapter.loadActiveByIdAndPropertyId(meetingId, propertyId))
                .contains(meeting);
        assertThat(adapter.update(meeting)).isEqualTo(meeting);
        verify(entity).applyChanges(meeting);
        verify(repository).flush();
    }

    @Test
    void loadsOrderedPropertyMeetingsAndDueReminders() {
        when(repository.findActiveByPropertyId(propertyId, NOW))
                .thenReturn(List.of(entity));
        Instant windowEnd = NOW.plusSeconds(86400);
        when(repository.findDueForReminder(NOW, windowEnd))
                .thenReturn(List.of(entity));

        assertThat(adapter.loadActiveByPropertyId(propertyId))
                .containsExactly(meeting);
        assertThat(adapter.loadDueForReminder(NOW, windowEnd))
                .containsExactly(meeting);
    }

    @Test
    void reportsMissingMeetingDuringUpdateAndEmptyLookup() {
        when(repository.findByIdAndPropertyIdAndDeletedAtIsNull(
                meetingId, propertyId)).thenReturn(Optional.empty());
        assertThat(adapter.loadActiveByIdAndPropertyId(meetingId, propertyId))
                .isEmpty();
        assertThatThrownBy(() -> adapter.update(meeting))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disappeared");
    }
}
