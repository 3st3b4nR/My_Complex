package com.mycomplex.users.infrastructure.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mycomplex.users.application.port.in.FindPropertyMembersUseCase;
import com.mycomplex.users.application.port.in.FindPropertyMemberDetailsUseCase;
import com.mycomplex.users.application.port.in.UpdatePropertyMemberUseCase;
import com.mycomplex.users.application.port.in.FindPropertyMemberCandidateUseCase;
import com.mycomplex.users.application.port.in.RegisterPropertyMemberUseCase;
import com.mycomplex.users.application.port.in.UnlinkPropertyMemberUseCase;
import com.mycomplex.users.domain.model.PropertyMemberUnlink;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberCandidateResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.RegisterPropertyMemberRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberDetailsResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.UpdatePropertyMemberRequest;
import com.mycomplex.users.infrastructure.adapter.in.rest.mapper.PropertyMemberRestMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/properties/{propertyId}")
public class PropertyMemberController {

    private final FindPropertyMembersUseCase useCase;
    private final FindPropertyMemberDetailsUseCase detailsUseCase;
    private final UpdatePropertyMemberUseCase updateUseCase;
    private final FindPropertyMemberCandidateUseCase candidateUseCase;
    private final RegisterPropertyMemberUseCase registerUseCase;
    private final UnlinkPropertyMemberUseCase unlinkUseCase;
    private final PropertyMemberRestMapper mapper;

    public PropertyMemberController(
            FindPropertyMembersUseCase useCase,
            FindPropertyMemberDetailsUseCase detailsUseCase,
            UpdatePropertyMemberUseCase updateUseCase,
            FindPropertyMemberCandidateUseCase candidateUseCase,
            RegisterPropertyMemberUseCase registerUseCase,
            UnlinkPropertyMemberUseCase unlinkUseCase,
            PropertyMemberRestMapper mapper
    ) {
        this.useCase = useCase;
        this.detailsUseCase = detailsUseCase;
        this.updateUseCase = updateUseCase;
        this.candidateUseCase = candidateUseCase;
        this.registerUseCase = registerUseCase;
        this.unlinkUseCase = unlinkUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/members")
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

    @GetMapping("/members/{membershipId}")
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

    @PutMapping("/members/{membershipId}")
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

    @GetMapping("/member-candidates")
    public PropertyMemberCandidateResponse findCandidate(
            @PathVariable UUID propertyId,
            @RequestParam String email,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mapper.toResponse(candidateUseCase.find(
                propertyId, email, jwt.getSubject()
        ));
    }

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyMemberDetailsResponse registerPropertyMember(
            @PathVariable UUID propertyId,
            @Valid @RequestBody RegisterPropertyMemberRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mapper.toResponse(registerUseCase.register(
                mapper.toDomain(propertyId, request),
                jwt.getSubject()
        ));
    }

    @DeleteMapping("/members/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkPropertyMember(
            @PathVariable UUID propertyId,
            @PathVariable UUID membershipId,
            @RequestParam long version,
            @AuthenticationPrincipal Jwt jwt
    ) {
        unlinkUseCase.unlink(
                new PropertyMemberUnlink(propertyId, membershipId, version),
                jwt.getSubject()
        );
    }
}
