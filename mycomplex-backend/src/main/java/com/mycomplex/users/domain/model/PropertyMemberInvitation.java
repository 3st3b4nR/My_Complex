package com.mycomplex.users.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PropertyMemberInvitation(
        UUID membershipId,
        UUID userId,
        UserRegistrationStatus registrationStatus,
        OffsetDateTime invitationSentAt,
        String message
) {
}
