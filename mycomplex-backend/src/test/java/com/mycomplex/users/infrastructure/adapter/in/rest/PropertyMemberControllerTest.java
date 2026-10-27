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
import com.mycomplex.users.application.exception.LastPropertyAdministratorException;
import com.mycomplex.users.application.exception.PropertyMemberConflictException;
import com.mycomplex.users.application.exception.PropertyMemberNotFoundException;
import com.mycomplex.users.application.port.in.FindPropertyMembersUseCase;
import com.mycomplex.users.application.port.in.FindPropertyMemberDetailsUseCase;
import com.mycomplex.users.application.port.in.UpdatePropertyMemberUseCase;
import com.mycomplex.properties.domain.model.PropertyStructureReference;
import com.mycomplex.properties.domain.model.PropertyStructureType;
import com.mycomplex.properties.domain.model.ResidentialUnitReference;
import com.mycomplex.properties.domain.model.ResidentialUnitType;
import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberUnit;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.domain.model.ResidentialRelationshipStatus;
import com.mycomplex.users.domain.model.ResidentialRelationshipType;
import com.mycomplex.users.domain.model.UserStatus;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberDetailsResponse;
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
                                    "30000000-0000-0000-0000-000000000001"
                            ),
                            UUID.fromString(
                                    "20000000-0000-0000-0000-000000000001"
                            ),
                            "John Esteban",
                            "Roldan Marin",
                            "admin@mycomplex.com",
                            PropertyRole.PROPERTY_ADMIN,
                            UserStatus.ACTIVE,
                            MembershipStatus.ACTIVE,
                            0,
                            List.of(new PropertyMemberUnit(
                                    UUID.fromString(
                                            "80000000-0000-0000-0000-000000000001"
                                    ),
                                    new ResidentialUnitReference(
                                            UUID.fromString(
                                                    "50000000-0000-0000-0000-000000000101"
                                            ),
                                            new PropertyStructureReference(
                                                    "Torre 1",
                                                    PropertyStructureType.TOWER
                                            ),
                                            "101",
                                            ResidentialUnitType.APARTMENT
                                    ),
                                    ResidentialRelationshipType.OWNER,
                                    true,
                                    ResidentialRelationshipStatus.ACTIVE
                            ))
                    ));
                };

        PropertyMemberRestMapper mapper = Mappers.getMapper(
                PropertyMemberRestMapper.class
        );
        FindPropertyMemberDetailsUseCase detailsUseCase =
                (currentPropertyId, membershipId, subject) -> null;
        UpdatePropertyMemberUseCase updateUseCase =
                (update, subject) -> null;
        PropertyMemberController controller =
                new PropertyMemberController(
                        useCase,
                        detailsUseCase,
                        updateUseCase,
                        mapper
                );
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(auth0Subject)
                .build();

        List<PropertyMemberResponse> response =
                controller.findPropertyMembers(propertyId, jwt);

        assertEquals(auth0Subject, receivedSubject.get());
        assertEquals(1, response.size());
        assertEquals(1, response.getFirst().units().size());
        assertEquals(
                "OWNER",
                response.getFirst().units().getFirst().relationshipType()
        );
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

    @Test
    void mapsManagementErrorsToSafeHttpStatuses() {
        UserRestExceptionHandler handler =
                new UserRestExceptionHandler();

        assertEquals(
                HttpStatus.NOT_FOUND,
                handler.handleNotFound(
                        new PropertyMemberNotFoundException()
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.CONFLICT,
                handler.handleConflict(
                        new PropertyMemberConflictException()
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                handler.handleLastAdministrator(
                        new LastPropertyAdministratorException()
                ).getStatusCode()
        );
        assertFalse(Arrays.stream(
                        PropertyMemberDetailsResponse.class
                                .getRecordComponents()
                )
                .anyMatch(component -> component.getName()
                        .equals("auth0Subject")));
    }
}
