package com.mycomplex.meetings.infrastructure.adapter.in.rest;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mycomplex.meetings.application.exception.MeetingNotFoundException;
import com.mycomplex.meetings.application.exception.MeetingOperationForbiddenException;
import com.mycomplex.meetings.application.exception.MeetingVersionConflictException;
import com.mycomplex.meetings.application.exception.MeetingViewingForbiddenException;

@RestControllerAdvice(assignableTypes = MeetingController.class)
public class MeetingRestExceptionHandler {

    @ExceptionHandler({
            MeetingOperationForbiddenException.class,
            MeetingViewingForbiddenException.class
    })
    ProblemDetail handleForbidden(RuntimeException exception) {
        return problem(
                HttpStatus.FORBIDDEN,
                "Meeting operation forbidden",
                exception.getMessage()
        );
    }

    @ExceptionHandler(MeetingNotFoundException.class)
    ProblemDetail handleNotFound(
            MeetingNotFoundException exception
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Meeting not found",
                exception.getMessage()
        );
    }

    @ExceptionHandler({
            MeetingVersionConflictException.class,
            OptimisticLockingFailureException.class
    })
    ProblemDetail handleConflict(RuntimeException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "Meeting version conflict",
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidData(
            IllegalArgumentException exception
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid meeting data",
                exception.getMessage()
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
