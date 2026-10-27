package com.mycomplex.users.infrastructure.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.port.in.FindPropertyMembersUseCase;
import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.domain.model.UserStatus;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.mapper.PropertyMemberRestMapper;

class PropertyMemberControllerTest {

    @Test
    void usesJwtSubjectAndDoesNotExposeItInResponse() {
        UUID propertyId = UUID.fromString(
                "10000000-0000-0000-0000-000000000001"
        );
        String auth0Subject = "auth0|administrator";
        AtomicReference<String> receivedSubject =
                new AtomicReference<>();

        FindPropertyMembersUseCase useCase =
                (receivedPropertyId, subject) -> {
                    assertEquals(propertyId, receivedPropertyId);
                    receivedSubject.set(subject);
                    return List.of(new PropertyMember(
                            UUID.fromString(
                                    "20000000-0000-0000-0000-000000000001"
                            ),
                            "John Esteban",
                            "Roldan Marin",
                            "admin@mycomplex.com",
                            PropertyRole.PROPERTY_ADMIN,
                            UserStatus.ACTIVE,
                            MembershipStatus.ACTIVE
                    ));
                };

        PropertyMemberRestMapper mapper = Mappers.getMapper(
                PropertyMemberRestMapper.class
        );
        PropertyMemberController controller =
                new PropertyMemberController(useCase, mapper);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(auth0Subject)
                .build();

        List<PropertyMemberResponse> response =
                controller.findPropertyMembers(propertyId, jwt);

        assertEquals(auth0Subject, receivedSubject.get());
        assertEquals(1, response.size());
        assertFalse(Arrays.stream(
                        PropertyMemberResponse.class
                                .getRecordComponents()
                )
                .anyMatch(component -> component.getName()
                        .equals("auth0Subject")));
    }

    @Test
    void mapsAccessDeniedToForbidden() {
        UserRestExceptionHandler handler =
                new UserRestExceptionHandler();

        assertEquals(
                HttpStatus.FORBIDDEN,
                handler.handlePropertyMembersAccessDenied(
                        new PropertyMembersAccessDeniedException()
                ).getStatusCode()
        );
    }
}
