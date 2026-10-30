package com.mycomplex.users.infrastructure.adapter.out.auth0;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycomplex.users.application.exception.ExternalIdentityException;
import com.mycomplex.users.application.port.out.ExternalIdentityPort;
import com.mycomplex.users.domain.model.ExternalUserIdentity;

@Component
public class Auth0ExternalIdentityAdapter implements ExternalIdentityPort {

    private final Auth0ManagementProperties properties;
    private final RestClient client;
    private final SecureTemporaryPasswordGenerator passwordGenerator =
            new SecureTemporaryPasswordGenerator();
    private final Object tokenMonitor = new Object();
    private volatile CachedToken cachedToken;

    public Auth0ExternalIdentityAdapter(
            Auth0ManagementProperties properties
    ) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getResponseTimeout());
        this.client = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<ExternalUserIdentity> findByEmail(String email) {
        requireConfiguration();
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        Auth0UserResponse[] users = managementRequest(token -> client.get()
                .uri(UriComponentsBuilder
                        .fromUriString(properties.baseUrl())
                        .path("/api/v2/users-by-email")
                        .queryParam("email", normalizedEmail)
                        .build().encode().toUri())
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleStatus)
                .body(Auth0UserResponse[].class));
        if (users == null || users.length == 0) {
            return Optional.empty();
        }
        Auth0UserResponse[] exact = Arrays.stream(users)
                .filter(user -> user.email() != null
                        && user.email().equalsIgnoreCase(normalizedEmail))
                .toArray(Auth0UserResponse[]::new);
        if (exact.length != 1) {
            throw conflict("Auth0 exact email lookup returned an ambiguous result");
        }
        Auth0UserResponse user = exact[0];
        boolean databaseIdentity = user.identities() != null
                && Arrays.stream(user.identities()).anyMatch(identity ->
                        "auth0".equals(identity.provider())
                        && properties.getDatabaseConnection().equals(
                                identity.connection()));
        if (!databaseIdentity) {
            throw conflict("Existing Auth0 identity is not from the configured database connection");
        }
        return Optional.of(toDomain(user));
    }

    @Override
    public ExternalUserIdentity create(
            UUID mycomplexUserId,
            String email,
            String firstNames,
            String lastNames
    ) {
        requireConfiguration();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("connection", properties.getDatabaseConnection());
        payload.put("email", email.strip().toLowerCase(Locale.ROOT));
        payload.put("email_verified", false);
        payload.put("given_name", firstNames);
        payload.put("family_name", lastNames);
        payload.put("name", firstNames + " " + lastNames);
        payload.put("password", passwordGenerator.generate());
        payload.put("app_metadata", Map.of(
                "needsInvitation", true,
                "mycomplexUserId", mycomplexUserId.toString()
        ));
        try {
            Auth0UserResponse response = managementRequest(token ->
                    client.post()
                            .uri(properties.baseUrl() + "/api/v2/users")
                            .headers(headers -> headers.setBearerAuth(token))
                            .body(payload)
                            .retrieve()
                            .onStatus(HttpStatusCode::isError,
                                    this::handleStatus)
                            .body(Auth0UserResponse.class));
            if (response == null || response.userId() == null) {
                throw unavailable("Auth0 create-user returned no subject");
            }
            return toDomain(response);
        } catch (ExternalIdentityException exception) {
            if ("AUTH0_IDENTITY_ALREADY_EXISTS".equals(exception.code())) {
                return findByEmail(email).orElseThrow(() -> exception);
            }
            throw exception;
        }
    }

    @Override
    public void requestInvitation(String email) {
        requireConfiguration();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", email.strip().toLowerCase(Locale.ROOT));
        payload.put("connection", properties.getDatabaseConnection());
        if (properties.getApplicationClientId() != null
                && !properties.getApplicationClientId().isBlank()) {
            payload.put("client_id", properties.getApplicationClientId());
        }
        try {
            client.post()
                    .uri(properties.baseUrl()
                            + "/dbconnections/change_password")
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::handleStatus)
                    .toBodilessEntity();
        } catch (ResourceAccessException exception) {
            throw unavailable("Auth0 invitation request timed out");
        } catch (RestClientException exception) {
            throw unavailable("Auth0 invitation response was invalid");
        }
    }

    private <T> T managementRequest(Function<String, T> request) {
        String token = accessToken();
        try {
            return request.apply(token);
        } catch (UnauthorizedException exception) {
            String refreshedToken = refreshAfterUnauthorized(token);
            try {
                return request.apply(refreshedToken);
            } catch (UnauthorizedException repeated) {
                throw new ExternalIdentityException(
                        "AUTH0_PERMISSION_DENIED",
                        "La integración con Auth0 no tiene los permisos requeridos.",
                        "Auth0 returned HTTP 401 after token renewal"
                );
            }
        } catch (ResourceAccessException exception) {
            throw unavailable("Auth0 Management API request timed out");
        } catch (RestClientException exception) {
            throw unavailable("Auth0 Management API response was invalid");
        }
    }

    private String accessToken() {
        CachedToken current = cachedToken;
        if (isUsable(current)) {
            return current.value();
        }
        synchronized (tokenMonitor) {
            current = cachedToken;
            if (isUsable(current)) {
                return current.value();
            }
            return requestNewToken();
        }
    }

    private String refreshAfterUnauthorized(String rejectedToken) {
        synchronized (tokenMonitor) {
            CachedToken current = cachedToken;
            if (isUsable(current)
                    && !current.value().equals(rejectedToken)) {
                return current.value();
            }
            cachedToken = null;
            return requestNewToken();
        }
    }

    private String requestNewToken() {
        TokenResponse response;
        try {
            response = client.post()
                    .uri(properties.baseUrl() + "/oauth/token")
                    .body(Map.of(
                            "grant_type", "client_credentials",
                            "client_id", properties.getClientId(),
                            "client_secret", properties.getClientSecret(),
                            "audience", properties.getAudience()
                    ))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            this::handleTokenStatus)
                    .body(TokenResponse.class);
        } catch (ResourceAccessException exception) {
            throw unavailable("Auth0 token request timed out");
        } catch (RestClientException exception) {
            throw unavailable("Auth0 token response could not be read");
        }
        if (response == null || response.accessToken() == null
                || response.expiresIn() <= 0) {
            throw unavailable("Auth0 token response was invalid");
        }
        cachedToken = new CachedToken(
                response.accessToken(),
                Instant.now().plusSeconds(response.expiresIn())
        );
        return cachedToken.value();
    }

    private boolean isUsable(CachedToken token) {
        return token != null && Instant.now().plus(
                properties.getTokenExpiryMargin()
        ).isBefore(token.expiresAt());
    }

    private void handleStatus(
            org.springframework.http.HttpRequest request,
            org.springframework.http.client.ClientHttpResponse response
    ) throws IOException {
        int status = response.getStatusCode().value();
        if (status == 401) {
            throw new UnauthorizedException();
        }
        if (status == 409) {
            throw new ExternalIdentityException(
                    "AUTH0_IDENTITY_ALREADY_EXISTS",
                    "El correo ya existe en el proveedor de identidad.",
                    "Auth0 returned HTTP 409"
            );
        }
        if (status == 429) {
            String retryAfter = response.getHeaders().getFirst("Retry-After");
            throw new ExternalIdentityException(
                    "AUTH0_RATE_LIMITED",
                    "Auth0 limitó temporalmente las solicitudes. Inténtalo más tarde.",
                    "Auth0 returned HTTP 429; Retry-After="
                            + safeRetryAfter(retryAfter)
            );
        }
        if (status == 403) {
            throw new ExternalIdentityException(
                    "AUTH0_PERMISSION_DENIED",
                    "La integración con Auth0 no tiene los permisos requeridos.",
                    "Auth0 returned HTTP 403"
            );
        }
        throw unavailable("Auth0 returned HTTP " + status);
    }

    private void handleTokenStatus(
            org.springframework.http.HttpRequest request,
            org.springframework.http.client.ClientHttpResponse response
    ) throws IOException {
        throw new ExternalIdentityException(
                "AUTH0_CONFIGURATION_ERROR",
                "La integración con Auth0 no está configurada correctamente.",
                "Auth0 token endpoint returned HTTP "
                        + response.getStatusCode().value()
        );
    }

    private ExternalUserIdentity toDomain(Auth0UserResponse user) {
        UUID localUserId = null;
        if (user.appMetadata() != null) {
            Object value = user.appMetadata().get("mycomplexUserId");
            if (value instanceof String identifier) {
                try {
                    localUserId = UUID.fromString(identifier);
                } catch (IllegalArgumentException ignored) {
                    localUserId = null;
                }
            }
        }
        return new ExternalUserIdentity(
                user.userId(), user.email(), localUserId
        );
    }

    private void requireConfiguration() {
        if (!properties.isConfigured()) {
            throw new ExternalIdentityException(
                    "AUTH0_NOT_CONFIGURED",
                    "La integración con Auth0 aún no está configurada.",
                    "Auth0 Management integration is not configured"
            );
        }
    }

    private ExternalIdentityException conflict(String detail) {
        return new ExternalIdentityException(
                "AUTH0_IDENTITY_CONFLICT",
                "El correo ya pertenece a una identidad que requiere revisión manual.",
                detail
        );
    }

    private ExternalIdentityException unavailable(String detail) {
        return new ExternalIdentityException(
                "AUTH0_UNAVAILABLE",
                "Auth0 no está disponible temporalmente.",
                detail
        );
    }

    private String safeRetryAfter(String value) {
        if (value == null || !value.matches("[0-9]{1,8}")) {
            return "unspecified";
        }
        return value;
    }

    private record CachedToken(String value, Instant expiresAt) { }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) { }

    private record Auth0Identity(
            String provider,
            String connection
    ) { }

    private record Auth0UserResponse(
            @JsonProperty("user_id") String userId,
            String email,
            @JsonProperty("app_metadata") Map<String, Object> appMetadata,
            Auth0Identity[] identities
    ) { }

    private static final class UnauthorizedException
            extends RuntimeException {
    }
}
