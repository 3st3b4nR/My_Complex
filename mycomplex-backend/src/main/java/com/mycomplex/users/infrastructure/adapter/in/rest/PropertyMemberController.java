package com.mycomplex.users.infrastructure.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mycomplex.users.application.port.in.FindPropertyMembersUseCase;
import com.mycomplex.users.application.port.in.FindPropertyMemberDetailsUseCase;
import com.mycomplex.users.application.port.in.UpdatePropertyMemberUseCase;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberDetailsResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.UpdatePropertyMemberRequest;
import com.mycomplex.users.infrastructure.adapter.in.rest.mapper.PropertyMemberRestMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/properties/{propertyId}/members")
public class PropertyMemberController {

    private final FindPropertyMembersUseCase useCase;
    private final FindPropertyMemberDetailsUseCase detailsUseCase;
    private final UpdatePropertyMemberUseCase updateUseCase;
    private final PropertyMemberRestMapper mapper;

    public PropertyMemberController(
            FindPropertyMembersUseCase useCase,
            FindPropertyMemberDetailsUseCase detailsUseCase,
            UpdatePropertyMemberUseCase updateUseCase,
            PropertyMemberRestMapper mapper
    ) {
        this.useCase = useCase;
        this.detailsUseCase = detailsUseCase;
        this.updateUseCase = updateUseCase;
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

    @GetMapping("/{membershipId}")
    public PropertyMemberDetailsResponse findPropertyMember(
            @PathVariable UUID propertyId,
            @PathVariable UUID membershipId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mapper.toResponse(detailsUseCase.find(
                propertyId,
                membershipId,
                jwt.getSubject()
        ));
    }

    @PutMapping("/{membershipId}")
    public PropertyMemberDetailsResponse updatePropertyMember(
            @PathVariable UUID propertyId,
            @PathVariable UUID membershipId,
            @Valid @RequestBody UpdatePropertyMemberRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mapper.toResponse(updateUseCase.update(
                mapper.toDomain(propertyId, membershipId, request),
                jwt.getSubject()
        ));
    }
}
