package com.mycomplex.users.infrastructure.adapter.in.rest;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @GetMapping("/me")
    public AuthenticationResponse getAuthenticatedUser(
            @AuthenticationPrincipal Jwt jwt) {

        return new AuthenticationResponse(
            jwt.getSubject(),
            jwt.getIssuer().toString(),
            jwt.getAudience()
        );
    }

    public record AuthenticationResponse(
        String subject,
        String issuer,
        List<String> audience
    ) {
    }
}