package com.mycomplex.users.infrastructure.adapter.in.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.mycomplex.users.application.port.in.LinkAuthenticatedUserUseCase;

import jakarta.servlet.FilterChain;

class AuthenticatedUserLinkingFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void linksUsingOnlyTheValidatedJwtSubject() throws Exception {
        LinkAuthenticatedUserUseCase useCase =
                mock(LinkAuthenticatedUserUseCase.class);
        FilterChain chain = mock(FilterChain.class);
        Jwt jwt = new Jwt(
                "token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), Map.of("sub", "auth0|member")
        );
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(
                        jwt,
                        null,
                        java.util.List.of()
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        new AuthenticatedUserLinkingFilter(useCase).doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                chain
        );

        verify(useCase).link("auth0|member");
        verify(chain).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void skipsLinkingWithoutJwtAuthentication() throws Exception {
        LinkAuthenticatedUserUseCase useCase =
                mock(LinkAuthenticatedUserUseCase.class);
        FilterChain chain = mock(FilterChain.class);

        new AuthenticatedUserLinkingFilter(useCase).doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                chain
        );

        verify(useCase, never()).link(
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(chain).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}
