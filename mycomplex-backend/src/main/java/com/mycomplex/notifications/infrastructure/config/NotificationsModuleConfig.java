package com.mycomplex.notifications.infrastructure.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mycomplex.notifications.application.port.in.NotifyMeetingChangedUseCase;
import com.mycomplex.notifications.application.port.in.NotifyNewsChangedUseCase;
import com.mycomplex.notifications.application.port.in.ManagePushInstallationUseCase;
import com.mycomplex.notifications.application.port.out.LoadMeetingNotificationRecipientsPort;
import com.mycomplex.notifications.application.port.out.LoadNewsNotificationRecipientsPort;
import com.mycomplex.notifications.application.port.out.SendEmailNotificationPort;
import com.mycomplex.notifications.application.port.out.SendPushNotificationPort;
import com.mycomplex.notifications.application.port.out.StorePushInstallationPort;
import com.mycomplex.notifications.application.service.ManagePushInstallationService;
import com.mycomplex.notifications.application.service.NotifyMeetingChangedService;
import com.mycomplex.notifications.application.service.NotifyNewsChangedService;

@Configuration(proxyBeanMethods = false)
public class NotificationsModuleConfig {

    @Bean
    ManagePushInstallationUseCase managePushInstallationUseCase(
            StorePushInstallationPort storePort
    ) {
        return new ManagePushInstallationService(storePort);
    }

    @Bean
    NotifyNewsChangedUseCase notifyNewsChangedUseCase(
            LoadNewsNotificationRecipientsPort recipientsPort,
            ObjectProvider<SendEmailNotificationPort> emailPorts,
            ObjectProvider<SendPushNotificationPort> pushPorts
    ) {
        return new NotifyNewsChangedService(
                recipientsPort,
                emailPorts.orderedStream().toList(),
                pushPorts.orderedStream().toList()
        );
    }

    @Bean
    NotifyMeetingChangedUseCase notifyMeetingChangedUseCase(
            LoadMeetingNotificationRecipientsPort recipientsPort,
            ObjectProvider<SendEmailNotificationPort> emailPorts,
            ObjectProvider<SendPushNotificationPort> pushPorts
    ) {
        return new NotifyMeetingChangedService(
                recipientsPort,
                emailPorts.orderedStream().toList(),
                pushPorts.orderedStream().toList()
        );
    }
}
