package com.mycomplex.users.application.port.out;

import java.time.Duration;
import java.util.UUID;

import com.mycomplex.users.domain.model.InvitationClaim;
import com.mycomplex.users.domain.model.PropertyMemberInvitation;

public interface UserInvitationPersistencePort {

    InvitationClaim claim(
            UUID propertyId,
            UUID membershipId,
            String administratorSubject,
            boolean resend,
            Duration provisioningTimeout,
            Duration resendCooldown
    );

    void markProvisioned(UUID userId, String auth0Subject);

    PropertyMemberInvitation markInvited(UUID userId, UUID membershipId);

    void recordProvisioningFailure(UUID userId, String safeError);

    void recordInvitationFailure(UUID userId, String safeError);

    boolean linkAuthenticatedUser(String auth0Subject);
}
