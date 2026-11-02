package com.mycomplex.notifications.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.mycomplex.notifications.api.TaskNotificationOutbox;
import com.mycomplex.notifications.application.port.in.FindFirebaseWebConfigUseCase;
import com.mycomplex.notifications.application.port.in.ManageFcmInstallationUseCase;
import com.mycomplex.notifications.application.port.out.FcmDeliveryPort;
import com.mycomplex.notifications.application.port.out.FcmInstallationPort;
import com.mycomplex.notifications.application.port.out.FcmOutboxPort;
import com.mycomplex.notifications.application.service.FindFirebaseWebConfigService;
import com.mycomplex.notifications.application.service.ManageFcmInstallationService;
import com.mycomplex.notifications.application.service.TaskNotificationOutboxService;
import com.mycomplex.users.api.TaskAccessDirectory;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(FcmProperties.class)
public class NotificationModuleConfig {

    @Bean
    ManageFcmInstallationUseCase manageFcmInstallationUseCase(
            TaskAccessDirectory users,
            FcmInstallationPort installations
    ) {
        return new ManageFcmInstallationService(users, installations);
    }

    @Bean
    TaskNotificationOutbox taskNotificationOutbox(FcmOutboxPort outbox) {
        return new TaskNotificationOutboxService(outbox);
    }

    @Bean
    FindFirebaseWebConfigUseCase findFirebaseWebConfigUseCase(
            FcmProperties properties,
            FcmDeliveryPort delivery
    ) {
        return new FindFirebaseWebConfigService(properties, delivery);
    }
}
