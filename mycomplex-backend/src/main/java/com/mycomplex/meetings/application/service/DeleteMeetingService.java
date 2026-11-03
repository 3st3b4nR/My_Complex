package com.mycomplex.meetings.application.service;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import com.mycomplex.meetings.application.exception.MeetingNotFoundException;
import com.mycomplex.meetings.application.exception.MeetingOperationForbiddenException;
import com.mycomplex.meetings.application.exception.MeetingVersionConflictException;
import com.mycomplex.meetings.application.port.in.DeleteMeetingCommand;
import com.mycomplex.meetings.application.port.in.DeleteMeetingUseCase;
import com.mycomplex.meetings.application.port.out.LoadAuthorizedMeetingOrganizerPort;
import com.mycomplex.meetings.application.port.out.MeetingPersistencePort;
import com.mycomplex.meetings.domain.model.Meeting;

public final class DeleteMeetingService
        implements DeleteMeetingUseCase {

    private final LoadAuthorizedMeetingOrganizerPort organizerPort;
    private final MeetingPersistencePort persistencePort;
    private final Clock clock;

    public DeleteMeetingService(
            LoadAuthorizedMeetingOrganizerPort organizerPort,
            MeetingPersistencePort persistencePort,
            Clock clock
    ) {
        this.organizerPort = Objects.requireNonNull(organizerPort);
        this.persistencePort = Objects.requireNonNull(persistencePort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void delete(DeleteMeetingCommand command) {
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

        persistencePort.update(
                current.delete(editorId, clock.instant())
        );
    }
}
