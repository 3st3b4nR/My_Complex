package com.mycomplex.users.infrastructure.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;

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
}
