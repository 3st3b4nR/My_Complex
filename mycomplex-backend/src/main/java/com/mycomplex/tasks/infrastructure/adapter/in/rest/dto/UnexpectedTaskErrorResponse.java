package com.mycomplex.tasks.infrastructure.adapter.in.rest.dto;

public record UnexpectedTaskErrorResponse(
        String code,
        String message,
        String correlationId
) { }
