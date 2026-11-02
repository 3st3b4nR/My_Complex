package com.mycomplex.tasks.infrastructure.adapter.in.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotNull String priority,
        @Size(max = 180) String location,
        @NotNull @Future OffsetDateTime dueAt,
        @Size(max = 1000) String administrativeNote,
        @NotNull UUID assignedMembershipId
) { }
