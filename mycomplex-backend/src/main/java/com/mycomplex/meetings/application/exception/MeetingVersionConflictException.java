package com.mycomplex.meetings.application.exception;

import java.util.UUID;

public class MeetingVersionConflictException
        extends RuntimeException {

    public MeetingVersionConflictException(
            UUID meetingId,
            long expected,
            long received
    ) {
        super(
                "Meeting %s has version %d but received %d"
                        .formatted(meetingId, expected, received)
        );
    }
}
