package com.mycomplex.notifications.application.exception;

public final class NotificationDeliveryException
        extends RuntimeException {

    public NotificationDeliveryException(
            String message,
            RuntimeException cause
    ) {
        super(message, cause);
    }
}
