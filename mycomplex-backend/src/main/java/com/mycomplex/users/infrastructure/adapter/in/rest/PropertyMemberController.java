package com.mycomplex.users.infrastructure.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycomplex.users.application.port.in.FindPropertyMembersUseCase;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.mapper.PropertyMemberRestMapper;

@RestController
@RequestMapping("/api/properties/{propertyId}/members")
public class PropertyMemberController {

    private final FindPropertyMembersUseCase useCase;
    private final PropertyMemberRestMapper mapper;

    public PropertyMemberController(
            FindPropertyMembersUseCase useCase,
            PropertyMemberRestMapper mapper
    ) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<PropertyMemberResponse> findPropertyMembers(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mapper.toResponseList(
                useCase.findByPropertyId(
                        propertyId,
                        jwt.getSubject()
                )
        );
    }
}
