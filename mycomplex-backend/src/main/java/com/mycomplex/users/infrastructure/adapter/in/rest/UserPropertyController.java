package com.mycomplex.users.infrastructure.adapter.in.rest;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycomplex.users.application.port.in.FindUserPropertiesUseCase;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.AssignedPropertyResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.mapper.AssignedPropertyRestMapper;

@RestController
@RequestMapping("/api/me/properties")
public class UserPropertyController {

    private final FindUserPropertiesUseCase useCase;
    private final AssignedPropertyRestMapper mapper;

    public UserPropertyController(
            FindUserPropertiesUseCase useCase,
            AssignedPropertyRestMapper mapper
    ) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<AssignedPropertyResponse> findMyProperties(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mapper.toResponseList(
                useCase.findByAuth0Subject(jwt.getSubject())
        );
    }
}