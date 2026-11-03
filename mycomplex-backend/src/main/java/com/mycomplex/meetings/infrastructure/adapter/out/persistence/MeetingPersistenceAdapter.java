package com.mycomplex.meetings.infrastructure.adapter.out.persistence;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.meetings.application.port.out.MeetingPersistencePort;
import com.mycomplex.meetings.domain.model.Meeting;
import com.mycomplex.meetings.infrastructure.adapter.out.persistence.entity.MeetingEntity;
import com.mycomplex.meetings.infrastructure.adapter.out.persistence.mapper.MeetingPersistenceMapper;
import com.mycomplex.meetings.infrastructure.adapter.out.persistence.repository.MeetingJpaRepository;

@Component
public class MeetingPersistenceAdapter
        implements MeetingPersistencePort {

    private final MeetingJpaRepository repository;
    private final MeetingPersistenceMapper mapper;
    private final Clock clock;

    public MeetingPersistenceAdapter(
            MeetingJpaRepository repository,
            MeetingPersistenceMapper mapper,
            @Qualifier("utcClock") Clock clock
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Meeting save(Meeting meeting) {
        MeetingEntity entity = mapper.toEntity(meeting);
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public Meeting update(Meeting meeting) {
        MeetingEntity entity = repository
                .findByIdAndPropertyIdAndDeletedAtIsNull(
                        meeting.id(),
                        meeting.propertyId()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Meeting disappeared during update"
                        )
                );

        entity.applyChanges(meeting);
        repository.flush();
        return mapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Meeting> loadActiveByIdAndPropertyId(
            UUID meetingId,
            UUID propertyId
    ) {
        return repository
                .findByIdAndPropertyIdAndDeletedAtIsNull(
                        meetingId,
                        propertyId
                )
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> loadActiveByPropertyId(UUID propertyId) {
        return repository.findActiveByPropertyId(
                propertyId,
                clock.instant()
        ).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public List<Meeting> loadDueForReminder(
            Instant now,
            Instant reminderWindowEnd
    ) {
        return repository.findDueForReminder(
                now,
                reminderWindowEnd
        ).stream().map(mapper::toDomain).toList();
    }
}
