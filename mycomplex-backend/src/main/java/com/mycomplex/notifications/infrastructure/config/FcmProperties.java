package com.mycomplex.notifications.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fcm")
public class FcmProperties {
    private boolean enabled;
    private String projectId = "";
    private int maxRetries = 3;
    private Duration retryDelay = Duration.ofMinutes(1);
    private Duration dispatchInterval = Duration.ofSeconds(5);
    private String webApiKey = "";
    private String webAuthDomain = "";
    private String webProjectId = "";
    private String webStorageBucket = "";
    private String webMessagingSenderId = "";
    private String webAppId = "";
    private String webVapidKey = "";

    public boolean hasWebConfiguration() {
        return hasText(webApiKey) && hasText(webAuthDomain)
                && hasText(webProjectId) && hasText(webMessagingSenderId)
                && hasText(webAppId) && hasText(webVapidKey);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public Duration getRetryDelay() { return retryDelay; }
    public void setRetryDelay(Duration retryDelay) { this.retryDelay = retryDelay; }
    public Duration getDispatchInterval() { return dispatchInterval; }
    public void setDispatchInterval(Duration value) { dispatchInterval = value; }
    public String getWebApiKey() { return webApiKey; }
    public void setWebApiKey(String value) { webApiKey = value; }
    public String getWebAuthDomain() { return webAuthDomain; }
    public void setWebAuthDomain(String value) { webAuthDomain = value; }
    public String getWebProjectId() { return webProjectId; }
    public void setWebProjectId(String value) { webProjectId = value; }
    public String getWebStorageBucket() { return webStorageBucket; }
    public void setWebStorageBucket(String value) { webStorageBucket = value; }
    public String getWebMessagingSenderId() { return webMessagingSenderId; }
    public void setWebMessagingSenderId(String value) { webMessagingSenderId = value; }
    public String getWebAppId() { return webAppId; }
    public void setWebAppId(String value) { webAppId = value; }
    public String getWebVapidKey() { return webVapidKey; }
    public void setWebVapidKey(String value) { webVapidKey = value; }
}
