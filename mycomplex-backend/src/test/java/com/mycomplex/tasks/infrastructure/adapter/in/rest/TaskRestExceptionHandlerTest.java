package com.mycomplex.tasks.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import com.mycomplex.observability.api.CorrelationIds;
import com.mycomplex.tasks.application.exception.InvalidTaskException;
import com.mycomplex.tasks.application.exception.TaskAccessDeniedException;
import com.mycomplex.tasks.application.exception.TaskConflictException;
import com.mycomplex.tasks.application.exception.TaskNotFoundException;

class TaskRestExceptionHandlerTest {

    private final TaskRestExceptionHandler handler =
            new TaskRestExceptionHandler();

    @Test
    void mapsExpectedTaskErrors() {
        assertThat(handler.accessDenied(new TaskAccessDeniedException())
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.notFound(new TaskNotFoundException())
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.conflict(new TaskConflictException())
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var invalid = handler.invalid(new InvalidTaskException(
                "Mensaje seguro"
        ));
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody().message()).isEqualTo("Mensaje seguro");
    }

    @Test
    void returnsCorrelationIdForUnexpectedFailure() {
        MDC.put(CorrelationIds.MDC_KEY, "correlation-id");
        try {
            var response = handler.unexpected(new IllegalStateException());
            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().correlationId())
                    .isEqualTo("correlation-id");
        } finally {
            MDC.remove(CorrelationIds.MDC_KEY);
        }
    }
}
