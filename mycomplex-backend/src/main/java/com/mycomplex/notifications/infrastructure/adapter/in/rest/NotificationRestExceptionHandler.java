package com.mycomplex.notifications.infrastructure.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.mycomplex.notifications.application.exception.FcmInstallationException;
import com.mycomplex.notifications.infrastructure.adapter.in.rest.dto.NotificationErrorResponse;
import com.mycomplex.notifications.infrastructure.adapter.in.rest.dto.UnexpectedNotificationErrorResponse;
import com.mycomplex.observability.api.CorrelationIds;

@RestControllerAdvice(
        basePackages = "com.mycomplex.notifications.infrastructure.adapter.in.rest"
)
public class NotificationRestExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            NotificationRestExceptionHandler.class
    );

    @ExceptionHandler(FcmInstallationException.class)
    ResponseEntity<NotificationErrorResponse> handle(
            FcmInstallationException exception
    ) {
        HttpStatus status = switch (exception.code()) {
            case "FCM_INSTALLATION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "FCM_INSTALLATION_CONFLICT" -> HttpStatus.CONFLICT;
            case "FCM_USER_NOT_AVAILABLE" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(
                new NotificationErrorResponse(
                        exception.code(), exception.safeMessage()
                )
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<UnexpectedNotificationErrorResponse> unexpected(
            Exception exception
    ) {
        String correlationId = MDC.get(CorrelationIds.MDC_KEY);
        LOGGER.atError()
                .addKeyValue("error.type", exception.getClass().getName())
                .log("Unexpected notifications API failure");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new UnexpectedNotificationErrorResponse(
                        "UNEXPECTED_NOTIFICATION_ERROR",
                        "No fue posible completar la operación.",
                        correlationId
                )
        );
    }
}
