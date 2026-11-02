package com.mycomplex.notifications.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterFcmInstallationRequest(
        @NotBlank @Size(min = 10, max = 512) String installationId
) { }
