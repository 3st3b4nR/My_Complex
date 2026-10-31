package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

public record UnexpectedUserErrorResponse(
        String code,
        String message,
        String correlationId
) {
}
