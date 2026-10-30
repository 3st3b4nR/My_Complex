package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PropertyMemberInvitationResponse(
        UUID membershipId,
        UUID userId,
        String registrationStatus,
        OffsetDateTime invitationSentAt,
        String message
) {
}
