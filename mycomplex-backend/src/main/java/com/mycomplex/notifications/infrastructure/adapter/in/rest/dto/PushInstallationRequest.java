package com.mycomplex.notifications.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushInstallationRequest(
        @NotBlank
        @Size(max = 512)
        String installationId
) {
}
