package com.mycomplex.users.application.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.mycomplex.users.application.exception.ExternalIdentityException;
import com.mycomplex.users.application.port.in.InvitePropertyMemberUseCase;
import com.mycomplex.users.application.port.out.ExternalIdentityPort;
import com.mycomplex.users.application.port.out.UserInvitationPersistencePort;
import com.mycomplex.users.domain.model.ExternalUserIdentity;
import com.mycomplex.users.domain.model.InvitationClaim;
import com.mycomplex.users.domain.model.PropertyMemberInvitation;

public class InvitePropertyMemberService
        implements InvitePropertyMemberUseCase {

    private static final String ACCEPTED_MESSAGE =
            "Auth0 aceptó la solicitud de invitación.";
    private static final String RESENT_MESSAGE =
            "La invitación fue reenviada. Debe utilizarse el correo más reciente.";

    private final UserInvitationPersistencePort persistencePort;
    private final ExternalIdentityPort identityPort;
    private final Duration provisioningTimeout;
    private final Duration resendCooldown;

    public InvitePropertyMemberService(
            UserInvitationPersistencePort persistencePort,
            ExternalIdentityPort identityPort,
            Duration provisioningTimeout,
            Duration resendCooldown
    ) {
        this.persistencePort = persistencePort;
        this.identityPort = identityPort;
        this.provisioningTimeout = provisioningTimeout;
        this.resendCooldown = resendCooldown;
    }

    @Override
    public PropertyMemberInvitation invite(
            UUID propertyId,
            UUID membershipId,
            String administratorSubject
    ) {
        return execute(propertyId, membershipId, administratorSubject, false);
    }

    @Override
    public PropertyMemberInvitation resend(
            UUID propertyId,
            UUID membershipId,
            String administratorSubject
    ) {
        return execute(propertyId, membershipId, administratorSubject, true);
    }

    private PropertyMemberInvitation execute(
            UUID propertyId,
            UUID membershipId,
            String administratorSubject,
            boolean resend
    ) {
        InvitationClaim claim = persistencePort.claim(
                propertyId,
                membershipId,
                administratorSubject,
                resend,
                provisioningTimeout,
                resendCooldown
        );

        if (claim.action() == InvitationClaim.Action.NONE) {
            return new PropertyMemberInvitation(
                    claim.membershipId(),
                    claim.userId(),
                    claim.registrationStatus(),
                    claim.invitationSentAt(),
                    claim.registrationStatus()
                            == com.mycomplex.users.domain.model.UserRegistrationStatus.INVITED
                            ? ACCEPTED_MESSAGE
                            : "La identidad ya se encuentra vinculada."
            );
        }

        if (claim.action() == InvitationClaim.Action.PROVISION) {
            provision(claim);
        }

        try {
            identityPort.requestInvitation(claim.email());
        } catch (ExternalIdentityException exception) {
            persistencePort.recordInvitationFailure(
                    claim.userId(), exception.safeTechnicalMessage()
            );
            if ("AUTH0_UNAVAILABLE".equals(exception.code())) {
                throw new ExternalIdentityException(
                        "AUTH0_INVITATION_UNAVAILABLE",
                        "La identidad fue creada, pero no fue posible solicitar el correo. Puedes reintentarlo.",
                        exception.safeTechnicalMessage()
                );
            }
            throw exception;
        }

        PropertyMemberInvitation result = persistencePort.markInvited(
                claim.userId(), claim.membershipId()
        );
        return new PropertyMemberInvitation(
                result.membershipId(), result.userId(),
                result.registrationStatus(), result.invitationSentAt(),
                resend ? RESENT_MESSAGE : ACCEPTED_MESSAGE
        );
    }

    private void provision(InvitationClaim claim) {
        try {
            Optional<ExternalUserIdentity> existing =
                    identityPort.findByEmail(claim.email());
            ExternalUserIdentity identity = existing
                    .map(current -> validateOwnership(current, claim.userId()))
                    .orElseGet(() -> validateOwnership(
                            identityPort.create(
                                    claim.userId(), claim.email(),
                                    claim.firstNames(), claim.lastNames()
                            ),
                            claim.userId()
                    ));
            persistencePort.markProvisioned(
                    claim.userId(), identity.subject()
            );
        } catch (ExternalIdentityException exception) {
            persistencePort.recordProvisioningFailure(
                    claim.userId(), exception.safeTechnicalMessage()
            );
            throw exception;
        }
    }

    private ExternalUserIdentity validateOwnership(
            ExternalUserIdentity identity,
            UUID expectedUserId
    ) {
        if (!expectedUserId.equals(identity.mycomplexUserId())) {
            throw new ExternalIdentityException(
                    "AUTH0_IDENTITY_CONFLICT",
                    "El correo ya pertenece a una identidad que requiere revisión manual.",
                    "Existing Auth0 identity ownership mismatch"
            );
        }
        return identity;
    }
}
