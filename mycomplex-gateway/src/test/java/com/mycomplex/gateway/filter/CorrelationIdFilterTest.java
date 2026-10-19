package com.mycomplex.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.mycomplex.gateway.filter.CorrelationIdFilter;

import reactor.core.publisher.Mono;

class CorrelationIdFilterTest {

    @Test
    void generatesCorrelationIdWhenRequestDoesNotContainOne() {
        CorrelationIdFilter filter = new CorrelationIdFilter();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/me").build()
        );

        AtomicReference<ServerWebExchange> forwardedExchange =
                new AtomicReference<>();

        GatewayFilterChain chain = currentExchange -> {
            forwardedExchange.set(currentExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String requestCorrelationId = forwardedExchange.get()
                .getRequest()
                .getHeaders()
                .getFirst(CorrelationIdFilter.HEADER_NAME);

        String responseCorrelationId = exchange
                .getResponse()
                .getHeaders()
                .getFirst(CorrelationIdFilter.HEADER_NAME);

        assertThat(requestCorrelationId).isNotBlank();
        assertThat(responseCorrelationId)
                .isEqualTo(requestCorrelationId);

        assertThat(UUID.fromString(requestCorrelationId).toString())
                .isEqualTo(requestCorrelationId);
    }

    @Test
    void preservesValidCorrelationIdFromRequest() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        String existingCorrelationId = UUID.randomUUID().toString();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/me")
                        .header(
                                CorrelationIdFilter.HEADER_NAME,
                                existingCorrelationId
                        )
                        .build()
        );

        AtomicReference<ServerWebExchange> forwardedExchange =
                new AtomicReference<>();

        GatewayFilterChain chain = currentExchange -> {
            forwardedExchange.set(currentExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String forwardedCorrelationId = forwardedExchange.get()
                .getRequest()
                .getHeaders()
                .getFirst(CorrelationIdFilter.HEADER_NAME);

        String responseCorrelationId = exchange
                .getResponse()
                .getHeaders()
                .getFirst(CorrelationIdFilter.HEADER_NAME);

        assertThat(forwardedCorrelationId)
                .isEqualTo(existingCorrelationId);

        assertThat(responseCorrelationId)
                .isEqualTo(existingCorrelationId);
    }
}