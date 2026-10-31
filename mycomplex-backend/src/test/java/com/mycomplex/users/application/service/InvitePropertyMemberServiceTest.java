package com.mycomplex.users.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.users.application.exception.ExternalIdentityException;
import com.mycomplex.users.application.port.out.ExternalIdentityPort;
import com.mycomplex.users.application.port.out.UserInvitationPersistencePort;
import com.mycomplex.users.domain.model.ExternalUserIdentity;
import com.mycomplex.users.domain.model.InvitationClaim;
import com.mycomplex.users.domain.model.PropertyMemberInvitation;
import com.mycomplex.users.domain.model.UserRegistrationStatus;

class InvitePropertyMemberServiceTest {

    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void provisionsAndInvitesAPendingUser() {
        FakePersistence persistence = new FakePersistence(
                claim(InvitationClaim.Action.PROVISION,
                        UserRegistrationStatus.PROVISIONING)
        );
        FakeIdentity identity = new FakeIdentity();
        InvitePropertyMemberService service = service(persistence, identity);

        PropertyMemberInvitation result = service.invite(
                PROPERTY_ID, MEMBERSHIP_ID, "auth0|administrator"
        );

        assertEquals("auth0|new-user", persistence.provisionedSubject);
        assertEquals(1, identity.createCalls);
        assertEquals(1, identity.invitationCalls);
        assertEquals(UserRegistrationStatus.INVITED,
                result.registrationStatus());
    }

    @Test
    void reconcilesAnOwnedIdentityWithoutCreatingADuplicate() {
        FakePersistence persistence = new FakePersistence(
                claim(InvitationClaim.Action.PROVISION,
                        UserRegistrationStatus.PROVISIONING)
        );
        FakeIdentity identity = new FakeIdentity();
        identity.existing = Optional.of(new ExternalUserIdentity(
                "auth0|existing", "user@example.com", USER_ID
        ));

        service(persistence, identity).invite(
                PROPERTY_ID, MEMBERSHIP_ID, "auth0|administrator"
        );

        assertEquals(0, identity.createCalls);
        assertEquals("auth0|existing", persistence.provisionedSubject);
    }

    @Test
    void rejectsAnIdentityOwnedByAnotherLocalUser() {
        FakePersistence persistence = new FakePersistence(
                claim(InvitationClaim.Action.PROVISION,
                        UserRegistrationStatus.PROVISIONING)
        );
        FakeIdentity identity = new FakeIdentity();
        identity.existing = Optional.of(new ExternalUserIdentity(
                "auth0|foreign", "user@example.com", UUID.randomUUID()
        ));

        ExternalIdentityException exception = assertThrows(
                ExternalIdentityException.class,
                () -> service(persistence, identity).invite(
                        PROPERTY_ID, MEMBERSHIP_ID,
                        "auth0|administrator"
                )
        );

        assertEquals("AUTH0_IDENTITY_CONFLICT", exception.code());
        assertEquals("Existing Auth0 identity ownership mismatch",
                persistence.provisioningError);
        assertEquals(0, identity.invitationCalls);
    }

    @Test
    void keepsProvisionedStateWhenInvitationRequestFails() {
        FakePersistence persistence = new FakePersistence(
                claim(InvitationClaim.Action.SEND_INVITATION,
                        UserRegistrationStatus.PROVISIONED)
        );
        FakeIdentity identity = new FakeIdentity();
        identity.invitationFailure = new ExternalIdentityException(
                "AUTH0_UNAVAILABLE", "unavailable", "HTTP timeout"
        );

        ExternalIdentityException exception = assertThrows(
                ExternalIdentityException.class,
                () -> service(persistence, identity).invite(
                        PROPERTY_ID, MEMBERSHIP_ID,
                        "auth0|administrator"
                )
        );

        assertEquals("AUTH0_INVITATION_UNAVAILABLE", exception.code());
        assertEquals("HTTP timeout", persistence.invitationError);
        assertEquals(0, persistence.markInvitedCalls);
    }

    @Test
    void doesNotResendAnInvitedIdentityWithoutExplicitResend() {
        FakePersistence persistence = new FakePersistence(
                claim(InvitationClaim.Action.NONE,
                        UserRegistrationStatus.INVITED)
        );
        FakeIdentity identity = new FakeIdentity();

        PropertyMemberInvitation result = service(persistence, identity)
                .invite(PROPERTY_ID, MEMBERSHIP_ID,
                        "auth0|administrator");

        assertEquals(0, identity.invitationCalls);
        assertEquals(UserRegistrationStatus.INVITED,
                result.registrationStatus());
    }

    private InvitePropertyMemberService service(
            FakePersistence persistence,
            FakeIdentity identity
    ) {
        return new InvitePropertyMemberService(
                persistence, identity, Duration.ofMinutes(5),
                Duration.ofMinutes(5)
        );
    }

    private InvitationClaim claim(
            InvitationClaim.Action action,
            UserRegistrationStatus status
    ) {
        return new InvitationClaim(
                MEMBERSHIP_ID, USER_ID, "user@example.com",
                "Ana", "Prueba", status, null, null, action
        );
    }

    private static final class FakeIdentity
            implements ExternalIdentityPort {
        private Optional<ExternalUserIdentity> existing = Optional.empty();
        private ExternalIdentityException invitationFailure;
        private int createCalls;
        private int invitationCalls;

        @Override
        public Optional<ExternalUserIdentity> findByEmail(String email) {
            return existing;
        }

        @Override
        public ExternalUserIdentity create(
                UUID mycomplexUserId,
                String email,
                String firstNames,
                String lastNames
        ) {
            createCalls++;
            return new ExternalUserIdentity(
                    "auth0|new-user", email, mycomplexUserId
            );
        }

        @Override
        public void requestInvitation(String email) {
            invitationCalls++;
            if (invitationFailure != null) {
                throw invitationFailure;
            }
        }
    }

    private static final class FakePersistence
            implements UserInvitationPersistencePort {
        private final InvitationClaim claim;
        private String provisionedSubject;
        private String provisioningError;
        private String invitationError;
        private int markInvitedCalls;

        private FakePersistence(InvitationClaim claim) {
            this.claim = claim;
        }

        @Override
        public InvitationClaim claim(
                UUID propertyId,
                UUID membershipId,
                String administratorSubject,
                boolean resend,
                Duration provisioningTimeout,
                Duration resendCooldown
        ) {
            return claim;
        }

        @Override
        public void markProvisioned(UUID userId, String auth0Subject) {
            provisionedSubject = auth0Subject;
        }

        @Override
        public PropertyMemberInvitation markInvited(
                UUID userId,
                UUID membershipId
        ) {
            markInvitedCalls++;
            return new PropertyMemberInvitation(
                    membershipId, userId, UserRegistrationStatus.INVITED,
                    OffsetDateTime.parse("2026-10-27T15:00:00Z"),
                    "accepted"
            );
        }

        @Override
        public void recordProvisioningFailure(
                UUID userId,
                String safeError
        ) {
            provisioningError = safeError;
        }

        @Override
        public void recordInvitationFailure(
                UUID userId,
                String safeError
        ) {
            invitationError = safeError;
        }

        @Override
        public boolean linkAuthenticatedUser(String auth0Subject) {
            return false;
        }
    }
}
