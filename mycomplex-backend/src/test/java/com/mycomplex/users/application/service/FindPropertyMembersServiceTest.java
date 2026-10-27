package com.mycomplex.users.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.domain.model.UserStatus;

class FindPropertyMembersServiceTest {

    private static final UUID PROPERTY_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001"
    );
    private static final String AUTH0_SUBJECT = "auth0|administrator";

    @Test
    void returnsMembersForActivePropertyAdministrator() {
        PropertyMember expectedMember = new PropertyMember(
                UUID.fromString(
                        "20000000-0000-0000-0000-000000000001"
                ),
                "John Esteban",
                "Roldan Marin",
                "admin@mycomplex.com",
                PropertyRole.PROPERTY_ADMIN,
                UserStatus.ACTIVE,
                MembershipStatus.ACTIVE,
                List.of()
        );

        LoadPropertyMembersPort port = new StubPort(
                true,
                List.of(expectedMember)
        );

        FindPropertyMembersService service =
                new FindPropertyMembersService(port);

        assertEquals(
                List.of(expectedMember),
                service.findByPropertyId(
                        PROPERTY_ID,
                        AUTH0_SUBJECT
                )
        );
    }

    @Test
    void deniesUserWithoutActiveAdministratorAccess() {
        FindPropertyMembersService service =
                new FindPropertyMembersService(
                        new StubPort(false, List.of())
                );

        assertThrows(
                PropertyMembersAccessDeniedException.class,
                () -> service.findByPropertyId(
                        PROPERTY_ID,
                        AUTH0_SUBJECT
                )
        );
    }

    @Test
    void rejectsMissingPropertyId() {
        FindPropertyMembersService service =
                new FindPropertyMembersService(
                        new StubPort(true, List.of())
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.findByPropertyId(
                        null,
                        AUTH0_SUBJECT
                )
        );

        assertEquals(
                "propertyId must not be null",
                exception.getMessage()
        );
    }

    @Test
    void rejectsBlankAuth0Subject() {
        FindPropertyMembersService service =
                new FindPropertyMembersService(
                        new StubPort(true, List.of())
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.findByPropertyId(PROPERTY_ID, " ")
        );

        assertEquals(
                "auth0Subject must not be blank",
                exception.getMessage()
        );
    }

    private record StubPort(
            boolean hasAccess,
            List<PropertyMember> members
    ) implements LoadPropertyMembersPort {

        @Override
        public boolean hasActiveAdministratorAccess(
                UUID propertyId,
                String auth0Subject
        ) {
            assertEquals(PROPERTY_ID, propertyId);
            assertEquals(AUTH0_SUBJECT, auth0Subject);
            return hasAccess;
        }

        @Override
        public List<PropertyMember> loadByPropertyId(
                UUID propertyId
        ) {
            assertEquals(PROPERTY_ID, propertyId);
            return members;
        }
    }
}
