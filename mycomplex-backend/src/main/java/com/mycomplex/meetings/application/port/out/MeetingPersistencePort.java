package com.mycomplex.meetings.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mycomplex.meetings.domain.model.Meeting;

public interface MeetingPersistencePort {

    Meeting save(Meeting meeting);

    Meeting update(Meeting meeting);

    Optional<Meeting> loadActiveByIdAndPropertyId(
            UUID meetingId,
            UUID propertyId
    );

    List<Meeting> loadActiveByPropertyId(UUID propertyId);

    List<Meeting> loadDueForReminder(
            Instant now,
            Instant reminderWindowEnd
    );
}
