package com.mycomplex.meetings.infrastructure.adapter.in.rest.dto;

import java.net.URI;
import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMeetingRequest(
        @NotBlank
        @Size(max = 180)
        String title,

        @NotBlank
        String description,

        @NotNull
        URI meetingUrl,

        @NotNull
        @Future
        Instant scheduledAt
) {
}
