package com.mycomplex.gateway.filter;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER_NAME = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        String correlationId = resolveCorrelationId(
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(HEADER_NAME)
        );

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers ->
                        headers.set(HEADER_NAME, correlationId)
                )
                .build();

        ServerWebExchange updatedExchange = exchange
                .mutate()
                .request(request)
                .build();

        updatedExchange.getResponse()
                .getHeaders()
                .set(HEADER_NAME, correlationId);

        return chain.filter(updatedExchange);
    }

    private String resolveCorrelationId(String receivedValue) {
        if (isValidUuid(receivedValue)) {
            return receivedValue;
        }

        return UUID.randomUUID().toString();
    }

    private boolean isValidUuid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            UUID parsedUuid = UUID.fromString(value);
            return parsedUuid.toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
