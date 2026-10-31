package com.mycomplex.observability.infrastructure.adapter.in.http;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mycomplex.observability.api.CorrelationIds;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolve(
                request.getHeader(CorrelationIds.HEADER_NAME)
        );
        response.setHeader(CorrelationIds.HEADER_NAME, correlationId);
        MDC.put(CorrelationIds.MDC_KEY, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationIds.MDC_KEY);
        }
    }

    private String resolve(String value) {
        if (value != null && !value.isBlank()) {
            try {
                UUID parsed = UUID.fromString(value);
                if (parsed.toString().equalsIgnoreCase(value)) {
                    return parsed.toString();
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid external values are replaced, never logged.
            }
        }
        return UUID.randomUUID().toString();
    }
}
