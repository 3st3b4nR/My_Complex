package com.mycomplex.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
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
            currentExchange.getResponse().setStatusCode(HttpStatus.OK);
            return currentExchange.getResponse().setComplete();
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
        assertThat(exchange.getResponse().getHeaders()
                .get(CorrelationIdFilter.HEADER_NAME)).hasSize(1);

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
            return currentExchange.getResponse().setComplete();
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

    @Test
    void replacesInvalidCorrelationIdBeforeForwardingOrLogging() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/me")
                        .header(
                                CorrelationIdFilter.HEADER_NAME,
                                "invalid\r\nforged"
                        )
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, current -> {
            forwarded.set(current);
            return current.getResponse().setComplete();
        }).block();

        String value = forwarded.get().getRequest().getHeaders()
                .getFirst(CorrelationIdFilter.HEADER_NAME);
        assertThat(UUID.fromString(value).toString()).isEqualTo(value);
        assertThat(value).doesNotContain("\r", "\n", "invalid");
    }
}
