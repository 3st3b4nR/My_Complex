package com.mycomplex.users.infrastructure.adapter.in.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mycomplex.users.application.exception.UserInvitationException;
import com.mycomplex.users.application.port.in.LinkAuthenticatedUserUseCase;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthenticatedUserLinkingFilter extends OncePerRequestFilter {

    private final LinkAuthenticatedUserUseCase useCase;

    public AuthenticatedUserLinkingFilter(
            LinkAuthenticatedUserUseCase useCase
    ) {
        this.useCase = useCase;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Jwt jwt) {
            try {
                useCase.link(jwt.getSubject());
            } catch (UserInvitationException exception) {
                response.sendError(
                        HttpServletResponse.SC_CONFLICT,
                        "La identidad autenticada requiere revisión."
                );
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
