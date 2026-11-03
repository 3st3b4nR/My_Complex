package com.mycomplex.meetings.infrastructure.adapter.in.rest.dto;

import java.net.URI;
import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMeetingRequest(
        @NotBlank
        @Size(max = 180)
        String title,

        @NotBlank
        String description,

        @NotNull
        URI meetingUrl,

        @NotNull
        @Future
        Instant scheduledAt,

        @Min(0)
        long version
) {
}
