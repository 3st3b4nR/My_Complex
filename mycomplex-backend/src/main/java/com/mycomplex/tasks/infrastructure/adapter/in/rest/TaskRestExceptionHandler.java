package com.mycomplex.tasks.infrastructure.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.mycomplex.tasks.application.exception.InvalidTaskException;
import com.mycomplex.tasks.application.exception.TaskAccessDeniedException;
import com.mycomplex.tasks.application.exception.TaskConflictException;
import com.mycomplex.tasks.application.exception.TaskNotFoundException;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.TaskErrorResponse;
import com.mycomplex.tasks.infrastructure.adapter.in.rest.dto.UnexpectedTaskErrorResponse;
import com.mycomplex.observability.api.CorrelationIds;

@RestControllerAdvice(
        basePackages = "com.mycomplex.tasks.infrastructure.adapter.in.rest"
)
public class TaskRestExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            TaskRestExceptionHandler.class
    );

    @ExceptionHandler(TaskAccessDeniedException.class)
    ResponseEntity<Void> accessDenied(TaskAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(TaskNotFoundException.class)
    ResponseEntity<TaskErrorResponse> notFound(TaskNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND",
                "La tarea solicitada no está disponible.");
    }

    @ExceptionHandler(TaskConflictException.class)
    ResponseEntity<TaskErrorResponse> conflict(TaskConflictException exception) {
        return response(HttpStatus.CONFLICT, "TASK_VERSION_CONFLICT",
                "La tarea fue modificada por otro administrador.");
    }

    @ExceptionHandler(InvalidTaskException.class)
    ResponseEntity<TaskErrorResponse> invalid(InvalidTaskException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_TASK",
                exception.safeMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<UnexpectedTaskErrorResponse> unexpected(
            Exception exception
    ) {
        String correlationId = MDC.get(CorrelationIds.MDC_KEY);
        LOGGER.atError()
                .addKeyValue("error.type", exception.getClass().getName())
                .log("Unexpected tasks API failure");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new UnexpectedTaskErrorResponse(
                        "UNEXPECTED_TASK_ERROR",
                        "No fue posible completar la operación.",
                        correlationId
                )
        );
    }

    private ResponseEntity<TaskErrorResponse> response(HttpStatus status,
            String code, String message) {
        return ResponseEntity.status(status).body(
                new TaskErrorResponse(code, message)
        );
    }
}
