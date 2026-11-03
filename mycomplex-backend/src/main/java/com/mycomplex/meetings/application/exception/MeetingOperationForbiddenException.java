package com.mycomplex.meetings.application.exception;

import java.util.UUID;

public class MeetingOperationForbiddenException
        extends RuntimeException {

    public MeetingOperationForbiddenException(UUID propertyId) {
        super(
                "User is not an active administrator of property "
                        + propertyId
        );
    }
}
