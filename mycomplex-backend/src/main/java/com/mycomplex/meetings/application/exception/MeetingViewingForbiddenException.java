package com.mycomplex.meetings.application.exception;

import java.util.UUID;

public class MeetingViewingForbiddenException
        extends RuntimeException {

    public MeetingViewingForbiddenException(UUID propertyId) {
        super(
                "User is not an active member of property "
                        + propertyId
        );
    }
}
