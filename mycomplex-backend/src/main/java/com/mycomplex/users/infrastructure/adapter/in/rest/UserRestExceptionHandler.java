package com.mycomplex.users.infrastructure.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.exception.InvalidPropertyMemberUpdateException;
import com.mycomplex.users.application.exception.LastPropertyAdministratorException;
import com.mycomplex.users.application.exception.PropertyMemberConflictException;
import com.mycomplex.users.application.exception.PropertyMemberNotFoundException;
import com.mycomplex.users.application.exception.InvalidPropertyMemberRegistrationException;
import com.mycomplex.users.application.exception.PropertyMemberBusinessConflictException;
import com.mycomplex.users.application.exception.UserInvitationException;
import com.mycomplex.users.application.exception.UserErrorCode;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.UserErrorResponse;

@RestControllerAdvice(
        basePackages = "com.mycomplex.users.infrastructure.adapter.in.rest"
)
public class UserRestExceptionHandler {

    @ExceptionHandler(PropertyMembersAccessDeniedException.class)
    public ResponseEntity<Void> handlePropertyMembersAccessDenied(
            PropertyMembersAccessDeniedException exception
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(PropertyMemberNotFoundException.class)
    public ResponseEntity<UserErrorResponse> handleNotFound(
            PropertyMemberNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new UserErrorResponse(
                        UserErrorCode.PROPERTY_MEMBER_NOT_FOUND.value(),
                        "La vinculación solicitada no está disponible."
                )
        );
    }

    @ExceptionHandler(PropertyMemberConflictException.class)
    public ResponseEntity<UserErrorResponse> handleConflict(
            PropertyMemberConflictException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new UserErrorResponse(
                        UserErrorCode.PROPERTY_MEMBER_VERSION_CONFLICT.value(),
                        "La vinculación fue modificada por otro administrador."
                )
        );
    }

    @ExceptionHandler(LastPropertyAdministratorException.class)
    public ResponseEntity<UserErrorResponse> handleLastAdministrator(
            LastPropertyAdministratorException exception
    ) {
        return ResponseEntity.badRequest().body(
                new UserErrorResponse(
                        UserErrorCode.LAST_PROPERTY_ADMINISTRATOR.value(),
                        "La copropiedad debe conservar un administrador activo."
                )
        );
    }

    @ExceptionHandler(InvalidPropertyMemberUpdateException.class)
    public ResponseEntity<UserErrorResponse> handleInvalidUpdate(
            InvalidPropertyMemberUpdateException exception
    ) {
        return ResponseEntity.badRequest().body(
                new UserErrorResponse(
                        UserErrorCode.INVALID_PROPERTY_MEMBER_UPDATE.value(),
                        exception.safeMessage()
                )
        );
    }

    @ExceptionHandler(InvalidPropertyMemberRegistrationException.class)
    public ResponseEntity<UserErrorResponse> handleInvalidRegistration(
            InvalidPropertyMemberRegistrationException exception
    ) {
        return ResponseEntity.badRequest().body(new UserErrorResponse(
                UserErrorCode.INVALID_PROPERTY_MEMBER_REGISTRATION.value(),
                exception.safeMessage()
        ));
    }

    @ExceptionHandler(PropertyMemberBusinessConflictException.class)
    public ResponseEntity<UserErrorResponse> handleBusinessConflict(
            PropertyMemberBusinessConflictException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new UserErrorResponse(
                        exception.code(),
                        exception.safeMessage()
                )
        );
    }

    @ExceptionHandler(UserInvitationException.class)
    public ResponseEntity<UserErrorResponse> handleInvitation(
            UserInvitationException exception
    ) {
        HttpStatus status = switch (exception.errorCode()) {
            case AUTH0_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case AUTH0_NOT_CONFIGURED, AUTH0_UNAVAILABLE,
                    AUTH0_INVITATION_UNAVAILABLE ->
                    HttpStatus.SERVICE_UNAVAILABLE;
            case AUTH0_CONFIGURATION_ERROR, AUTH0_PERMISSION_DENIED ->
                    HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(
                new UserErrorResponse(exception.code(), exception.safeMessage())
        );
    }
}
