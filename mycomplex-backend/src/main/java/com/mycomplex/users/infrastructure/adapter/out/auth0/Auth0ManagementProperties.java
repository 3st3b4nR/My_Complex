package com.mycomplex.users.infrastructure.adapter.out.auth0;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth0.management")
public class Auth0ManagementProperties {

    private String domain = "";
    private String clientId = "";
    private String clientSecret = "";
    private String audience = "";
    private String databaseConnection = "";
    private String applicationClientId = "";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration responseTimeout = Duration.ofSeconds(8);
    private Duration tokenExpiryMargin = Duration.ofSeconds(60);
    private Duration provisioningTimeout = Duration.ofMinutes(5);
    private Duration resendCooldown = Duration.ofMinutes(5);

    public boolean isConfigured() {
        return hasText(domain) && hasText(clientId)
                && hasText(clientSecret) && hasText(audience)
                && hasText(databaseConnection);
    }

    public String baseUrl() {
        String value = domain.strip();
        String withScheme = value.startsWith("http://")
                || value.startsWith("https://")
                ? value
                : "https://" + value;
        return withScheme.endsWith("/")
                ? withScheme.substring(0, withScheme.length() - 1)
                : withScheme;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public String getDatabaseConnection() { return databaseConnection; }
    public void setDatabaseConnection(String databaseConnection) { this.databaseConnection = databaseConnection; }
    public String getApplicationClientId() { return applicationClientId; }
    public void setApplicationClientId(String applicationClientId) { this.applicationClientId = applicationClientId; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getResponseTimeout() { return responseTimeout; }
    public void setResponseTimeout(Duration responseTimeout) { this.responseTimeout = responseTimeout; }
    public Duration getTokenExpiryMargin() { return tokenExpiryMargin; }
    public void setTokenExpiryMargin(Duration tokenExpiryMargin) { this.tokenExpiryMargin = tokenExpiryMargin; }
    public Duration getProvisioningTimeout() { return provisioningTimeout; }
    public void setProvisioningTimeout(Duration provisioningTimeout) { this.provisioningTimeout = provisioningTimeout; }
    public Duration getResendCooldown() { return resendCooldown; }
    public void setResendCooldown(Duration resendCooldown) { this.resendCooldown = resendCooldown; }
}
