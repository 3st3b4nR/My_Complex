package com.mycomplex.tasks.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeTaskStatusRequest(
        @NotBlank String status,
        @PositiveOrZero long version
) { }
