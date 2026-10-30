package com.mycomplex.users.infrastructure.adapter.out.auth0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mycomplex.users.domain.model.ExternalUserIdentity;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class Auth0ExternalIdentityAdapterTest {

    private HttpServer server;
    private Auth0ManagementProperties properties;
    private final AtomicInteger tokenRequests = new AtomicInteger();
    private final AtomicInteger lookupRequests = new AtomicInteger();
    private final AtomicReference<String> createBody = new AtomicReference<>();
    private final AtomicReference<String> invitationBody =
            new AtomicReference<>();
    private final AtomicBoolean rejectFirstManagementToken =
            new AtomicBoolean();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oauth/token", this::token);
        server.createContext("/api/v2/users-by-email", this::lookup);
        server.createContext("/api/v2/users", this::create);
        server.createContext("/dbconnections/change_password", this::invite);
        server.start();

        properties = new Auth0ManagementProperties();
        properties.setDomain("http://127.0.0.1:"
                + server.getAddress().getPort());
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");
        properties.setAudience("https://tenant.test/api/v2/");
        properties.setDatabaseConnection("Username-Password-Authentication");
        properties.setApplicationClientId("spa-client");
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setResponseTimeout(Duration.ofSeconds(2));
        properties.setTokenExpiryMargin(Duration.ofSeconds(5));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void obtainsAndCachesTheManagementToken() {
        Auth0ExternalIdentityAdapter adapter =
                new Auth0ExternalIdentityAdapter(properties);

        adapter.findByEmail("User@Example.com");
        adapter.findByEmail("user@example.com");

        assertEquals(1, tokenRequests.get());
        assertEquals(2, lookupRequests.get());
    }

    @Test
    void createsExpectedMetadataAndRequestsInvitationWithoutExposingPassword() {
        Auth0ExternalIdentityAdapter adapter =
                new Auth0ExternalIdentityAdapter(properties);
        UUID userId = UUID.randomUUID();

        ExternalUserIdentity identity = adapter.create(
                userId, "User@Example.com", "Ana", "Prueba"
        );
        adapter.requestInvitation("User@Example.com");

        assertEquals("auth0|created", identity.subject());
        assertEquals(userId, identity.mycomplexUserId());
        assertTrue(createBody.get().contains("\"email_verified\":false"));
        assertTrue(createBody.get().contains("\"needsInvitation\":true"));
        assertTrue(createBody.get().contains(userId.toString()));
        assertTrue(createBody.get().contains("\"password\":"));
        assertFalse(invitationBody.get().contains("password"));
        assertTrue(invitationBody.get().contains("spa-client"));
    }

    @Test
    void renewsTokenOnlyOnceAfterUnauthorizedResponse() {
        rejectFirstManagementToken.set(true);
        Auth0ExternalIdentityAdapter adapter =
                new Auth0ExternalIdentityAdapter(properties);

        adapter.findByEmail("user@example.com");

        assertEquals(2, tokenRequests.get());
        assertEquals(2, lookupRequests.get());
    }

    private void token(HttpExchange exchange) throws IOException {
        int number = tokenRequests.incrementAndGet();
        respond(exchange, 200, """
                {"access_token":"management-token-%d","expires_in":3600}
                """.formatted(number));
    }

    private void lookup(HttpExchange exchange) throws IOException {
        int number = lookupRequests.incrementAndGet();
        if (rejectFirstManagementToken.get() && number == 1) {
            respond(exchange, 401, "{}");
            return;
        }
        respond(exchange, 200, "[]");
    }

    private void create(HttpExchange exchange) throws IOException {
        createBody.set(new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        ));
        String userId = extractLocalUserId(createBody.get());
        respond(exchange, 201, """
                {
                  "user_id":"auth0|created",
                  "email":"user@example.com",
                  "app_metadata":{"needsInvitation":true,"mycomplexUserId":"%s"},
                  "identities":[{"provider":"auth0","connection":"Username-Password-Authentication"}]
                }
                """.formatted(userId));
    }

    private void invite(HttpExchange exchange) throws IOException {
        invitationBody.set(new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        ));
        respond(exchange, 200, "We've just sent you an email");
    }

    private String extractLocalUserId(String body) {
        String marker = "\"mycomplexUserId\":\"";
        int start = body.indexOf(marker) + marker.length();
        return body.substring(start, body.indexOf('"', start));
    }

    private void respond(
            HttpExchange exchange,
            int status,
            String body
    ) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json"
        );
        exchange.sendResponseHeaders(status, 0);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
