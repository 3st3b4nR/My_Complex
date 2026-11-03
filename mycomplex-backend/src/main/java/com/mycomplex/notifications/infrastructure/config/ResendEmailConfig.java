package com.mycomplex.notifications.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mycomplex.notifications.application.port.out.EmailDeliveryPort;
import com.mycomplex.notifications.application.port.out.EmailOutboxPort;
import com.mycomplex.notifications.application.port.out.SendEmailNotificationPort;
import com.mycomplex.notifications.application.service.QueueEmailNotificationService;
import com.mycomplex.notifications.infrastructure.adapter.out.email.ResendEmailNotificationAdapter;
import com.mycomplex.notifications.infrastructure.service.EmailOutboxDispatcher;

import io.micrometer.observation.ObservationRegistry;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ResendEmailProperties.class)
@ConditionalOnProperty(
        prefix = "mycomplex.notifications.email.resend",
        name = "enabled",
        havingValue = "true"
)
public class ResendEmailConfig {

    @Bean
    EmailDeliveryPort resendEmailDeliveryPort(
            ResendEmailProperties properties,
            ObservationRegistry observations
    ) {
        return new ResendEmailNotificationAdapter(
                properties.getBaseUrl(),
                properties.getApiKey(),
                properties.getFrom(),
                observations
        );
    }

    @Bean
    SendEmailNotificationPort queuedEmailNotificationPort(
            EmailOutboxPort outbox
    ) {
        return new QueueEmailNotificationService(outbox);
    }

    @Bean
    EmailOutboxDispatcher emailOutboxDispatcher(
            EmailOutboxPort outbox,
            EmailDeliveryPort delivery,
            ResendEmailProperties properties
    ) {
        return new EmailOutboxDispatcher(outbox, delivery, properties);
    }
}
