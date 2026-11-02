package com.mycomplex.notifications.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mycomplex.notifications.application.port.out.SendEmailNotificationPort;
import com.mycomplex.notifications.infrastructure.adapter.out.email.ResendEmailNotificationAdapter;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ResendEmailProperties.class)
@ConditionalOnProperty(
        prefix = "mycomplex.notifications.email.resend",
        name = "enabled",
        havingValue = "true"
)
public class ResendEmailConfig {

    @Bean
    SendEmailNotificationPort resendEmailNotificationPort(
            ResendEmailProperties properties
    ) {
        return new ResendEmailNotificationAdapter(
                properties.getBaseUrl(),
                properties.getApiKey(),
                properties.getFrom()
        );
    }
}
