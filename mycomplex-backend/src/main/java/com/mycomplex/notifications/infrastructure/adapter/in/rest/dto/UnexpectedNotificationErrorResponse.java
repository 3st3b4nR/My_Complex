package com.mycomplex.notifications.infrastructure.adapter.in.rest.dto;

public record UnexpectedNotificationErrorResponse(
        String code,
        String message,
        String correlationId
) { }
