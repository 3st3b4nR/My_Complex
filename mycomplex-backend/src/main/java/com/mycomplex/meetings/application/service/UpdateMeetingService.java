package com.mycomplex.meetings.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.mycomplex.meetings.application.exception.MeetingNotFoundException;
import com.mycomplex.meetings.application.exception.MeetingOperationForbiddenException;
import com.mycomplex.meetings.application.exception.MeetingVersionConflictException;
import com.mycomplex.meetings.application.port.in.UpdateMeetingCommand;
import com.mycomplex.meetings.application.port.in.UpdateMeetingUseCase;
import com.mycomplex.meetings.application.port.out.LoadAuthorizedMeetingOrganizerPort;
import com.mycomplex.meetings.application.port.out.MeetingPersistencePort;
import com.mycomplex.meetings.application.port.out.PublishMeetingEventPort;
import com.mycomplex.meetings.domain.model.Meeting;

public final class UpdateMeetingService
        implements UpdateMeetingUseCase {

    private final LoadAuthorizedMeetingOrganizerPort organizerPort;
    private final MeetingPersistencePort persistencePort;
    private final PublishMeetingEventPort eventPort;
    private final Clock clock;

    public UpdateMeetingService(
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
    public Meeting update(UpdateMeetingCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        UUID editorId = organizerPort.findActiveAdministratorId(
                command.propertyId(),
                command.auth0Subject()
        ).orElseThrow(() ->
                new MeetingOperationForbiddenException(
                        command.propertyId()
                )
        );

        Meeting current = persistencePort
                .loadActiveByIdAndPropertyId(
                        command.meetingId(),
                        command.propertyId()
                )
                .orElseThrow(() ->
                        new MeetingNotFoundException(command.meetingId())
                );

        if (current.version() != command.version()) {
            throw new MeetingVersionConflictException(
                    current.id(),
                    current.version(),
                    command.version()
            );
        }

        Instant now = clock.instant();
        Meeting updated = current.update(
                command.title(),
                command.description(),
                command.meetingUrl(),
                command.scheduledAt(),
                editorId,
                now
        );

        if (updated.isReminderDue(now)) {
            updated = updated.markReminderSent(now);
        }

        Meeting saved = persistencePort.update(updated);
        eventPort.publishUpdated(saved);
        return saved;
    }
}
