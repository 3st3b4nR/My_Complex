package com.mycomplex.news.infrastructure.adapter.in.rest;

import java.io.UncheckedIOException;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.mycomplex.news.application.exception.NewsImageNotFoundException;
import com.mycomplex.news.application.exception.NewsNotFoundException;
import com.mycomplex.news.application.exception.NewsPublishingForbiddenException;
import com.mycomplex.news.application.exception.NewsVersionConflictException;
import com.mycomplex.news.application.exception.NewsViewingForbiddenException;

@RestControllerAdvice(
        assignableTypes = NewsController.class
)
public class NewsRestExceptionHandler {

    @ExceptionHandler(
            NewsPublishingForbiddenException.class
    )
    ProblemDetail handleForbidden(
            NewsPublishingForbiddenException exception
    ) {
        return problem(
                HttpStatus.FORBIDDEN,
                "News operation forbidden",
                exception.getMessage()
        );
    }

    @ExceptionHandler(
            NewsViewingForbiddenException.class
    )
    ProblemDetail handleViewingForbidden(
            NewsViewingForbiddenException exception
    ) {
        return problem(
                HttpStatus.FORBIDDEN,
                "News viewing forbidden",
                exception.getMessage()
        );
    }

    @ExceptionHandler(NewsNotFoundException.class)
    ProblemDetail handleNotFound(
            NewsNotFoundException exception
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "News not found",
                exception.getMessage()
        );
    }

    @ExceptionHandler(NewsImageNotFoundException.class)
    ProblemDetail handleImageNotFound(
            NewsImageNotFoundException exception
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "News image not found",
                exception.getMessage()
        );
    }

    @ExceptionHandler({
            NewsVersionConflictException.class,
            OptimisticLockingFailureException.class
    })
    ProblemDetail handleVersionConflict(
            RuntimeException exception
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "News version conflict",
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidData(
            IllegalArgumentException exception
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid news data",
                exception.getMessage()
        );
    }

    @ExceptionHandler(UncheckedIOException.class)
    ProblemDetail handleUnreadableImage(
            UncheckedIOException exception
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid image",
                exception.getMessage()
        );
    }

    @ExceptionHandler(
            MaxUploadSizeExceededException.class
    )
    ProblemDetail handleOversizedImage() {
        return problem(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Image too large",
                "Image must not exceed 5 MB"
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        status,
                        detail
                );

        problem.setTitle(title);

        return problem;
    }
}
