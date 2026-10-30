package com.mycomplex.users.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvitationClaim(
        UUID membershipId,
        UUID userId,
        String email,
        String firstNames,
        String lastNames,
        UserRegistrationStatus registrationStatus,
        String auth0Subject,
        OffsetDateTime invitationSentAt,
        Action action
) {
    public enum Action {
        PROVISION,
        SEND_INVITATION,
        NONE
    }
}
