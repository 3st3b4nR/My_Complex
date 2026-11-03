package com.mycomplex.notifications.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "mycomplex.notifications.push.firebase"
)
public record FirebasePushProperties(
        boolean enabled,
        String projectId,
        String frontendBaseUrl
) {
}
