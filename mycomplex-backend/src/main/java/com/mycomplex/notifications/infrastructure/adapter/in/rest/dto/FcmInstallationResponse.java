package com.mycomplex.notifications.infrastructure.adapter.in.rest.dto;

import java.util.UUID;

public record FcmInstallationResponse(UUID id, String status) { }
