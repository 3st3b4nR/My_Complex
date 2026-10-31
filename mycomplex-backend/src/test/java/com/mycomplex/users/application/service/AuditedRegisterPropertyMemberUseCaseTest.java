package com.mycomplex.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.mycomplex.observability.api.SecurityAuditCommand;
import com.mycomplex.observability.api.SecurityAuditEventType;
import com.mycomplex.observability.api.SecurityEventReporter;
import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberDetails;
import com.mycomplex.users.domain.model.PropertyMemberRegistration;
import com.mycomplex.users.domain.model.PropertyMemberRegistrationMode;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.domain.model.UserRegistrationStatus;
import com.mycomplex.users.domain.model.UserStatus;

class AuditedRegisterPropertyMemberUseCaseTest {

    @Test
    void recordsSuccessfulMemberRegistrationWithoutPiiMetadata() {
        UUID actorId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        PropertyMemberDetails details = new PropertyMemberDetails(
                new PropertyMember(
                        UUID.randomUUID(), targetId, "Target", "User",
                        "target@example.test", PropertyRole.RESIDENT,
                        UserStatus.ACTIVE, UserRegistrationStatus.PENDING,
                        MembershipStatus.ACTIVE, 0, List.of()
                ),
                List.of()
        );
        AtomicReference<SecurityAuditCommand> recorded =
                new AtomicReference<>();
        AuditedRegisterPropertyMemberUseCase useCase =
                new AuditedRegisterPropertyMemberUseCase(
                        (registration, subject) -> details,
                        subject -> java.util.Optional.of(actorId),
                        recorded::set,
                        new NoOpSecurityReporter()
                );
        PropertyMemberRegistration registration =
                new PropertyMemberRegistration(
                        propertyId,
                        PropertyMemberRegistrationMode.CREATE_PENDING_USER,
                        null, null, "Target", "User",
                        "target@example.test", null, null,
                        PropertyRole.RESIDENT, MembershipStatus.ACTIVE,
                        List.of()
                );

        PropertyMemberDetails result = useCase.register(
                registration, "auth0|administrator"
        );

        assertThat(result).isSameAs(details);
        assertThat(recorded.get().type())
                .isEqualTo(SecurityAuditEventType.MEMBER_REGISTERED);
        assertThat(recorded.get().actorUserId()).isEqualTo(actorId);
        assertThat(recorded.get().propertyId()).isEqualTo(propertyId);
        assertThat(recorded.get().targetUserId()).isEqualTo(targetId);
        assertThat(recorded.get().metadata())
                .containsOnlyKeys("registrationMode")
                .doesNotContainKeys("email", "document", "auth0Subject");
    }

    private static final class NoOpSecurityReporter
            implements SecurityEventReporter {
        @Override
        public void administrativeAccessDenied(String operation) {
        }

        @Override
        public void invitationFailed(String operation, String reasonCode) {
        }
    }
}
