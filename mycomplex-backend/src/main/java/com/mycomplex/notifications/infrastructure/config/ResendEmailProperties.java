package com.mycomplex.notifications.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "mycomplex.notifications.email.resend"
)
public class ResendEmailProperties {

    private boolean enabled;
    private String apiKey;
    private String from;
    private String baseUrl = "https://api.resend.com";
    private int maxRetries = 5;
    private Duration retryDelay = Duration.ofSeconds(30);
    private Duration dispatchInterval = Duration.ofSeconds(5);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public Duration getDispatchInterval() {
        return dispatchInterval;
    }

    public void setDispatchInterval(Duration dispatchInterval) {
        this.dispatchInterval = dispatchInterval;
    }
}
