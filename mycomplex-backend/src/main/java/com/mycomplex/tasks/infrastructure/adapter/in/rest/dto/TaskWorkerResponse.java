package com.mycomplex.tasks.infrastructure.adapter.in.rest.dto;

import java.util.UUID;

public record TaskWorkerResponse(
        UUID membershipId,
        String displayName
) { }
