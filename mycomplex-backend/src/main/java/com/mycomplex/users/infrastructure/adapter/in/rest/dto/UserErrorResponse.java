package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

public record UserErrorResponse(
        String code,
        String message
) {
}
