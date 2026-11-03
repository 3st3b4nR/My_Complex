package com.mycomplex.meetings.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.mycomplex.meetings.application.exception.MeetingOperationForbiddenException;
import com.mycomplex.meetings.application.port.in.CreateMeetingCommand;
import com.mycomplex.meetings.application.port.in.CreateMeetingUseCase;
import com.mycomplex.meetings.application.port.out.LoadAuthorizedMeetingOrganizerPort;
import com.mycomplex.meetings.application.port.out.MeetingPersistencePort;
import com.mycomplex.meetings.application.port.out.PublishMeetingEventPort;
import com.mycomplex.meetings.domain.model.Meeting;

public final class CreateMeetingService
        implements CreateMeetingUseCase {

    private final LoadAuthorizedMeetingOrganizerPort organizerPort;
    private final MeetingPersistencePort persistencePort;
    private final PublishMeetingEventPort eventPort;
    private final Clock clock;

    public CreateMeetingService(
            LoadAuthorizedMeetingOrganizerPort organizerPort,
            MeetingPersistencePort persistencePort,
            PublishMeetingEventPort eventPort,
            Clock clock
    ) {
        this.organizerPort = Objects.requireNonNull(organizerPort);
        this.persistencePort = Objects.requireNonNull(persistencePort);
        this.eventPort = Objects.requireNonNull(eventPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Meeting create(CreateMeetingCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        UUID creatorId = organizerPort.findActiveAdministratorId(
                command.propertyId(),
                command.auth0Subject()
        ).orElseThrow(() ->
                new MeetingOperationForbiddenException(
                        command.propertyId()
                )
        );

        Instant now = clock.instant();
        Meeting meeting = Meeting.schedule(
                UUID.randomUUID(),
                command.propertyId(),
                creatorId,
                command.title(),
                command.description(),
                command.meetingUrl(),
                command.scheduledAt(),
                now
        );

        if (meeting.isReminderDue(now)) {
            meeting = meeting.markReminderSent(now);
        }

        Meeting saved = persistencePort.save(meeting);
        eventPort.publishCreated(saved);
        return saved;
    }
}
