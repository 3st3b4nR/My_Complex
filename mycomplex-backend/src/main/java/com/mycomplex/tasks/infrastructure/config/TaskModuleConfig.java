package com.mycomplex.tasks.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mycomplex.notifications.api.TaskNotificationOutbox;
import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.api.TaskOperationalMetrics;
import com.mycomplex.tasks.application.port.in.FindAdministrativeTasksUseCase;
import com.mycomplex.tasks.application.port.in.ManageAdministrativeTasksUseCase;
import com.mycomplex.tasks.application.port.out.AdministrativeTaskPort;
import com.mycomplex.tasks.application.service.FindAdministrativeTasksService;
import com.mycomplex.tasks.application.service.ManageAdministrativeTasksService;
import com.mycomplex.users.api.TaskAccessDirectory;

@Configuration(proxyBeanMethods = false)
public class TaskModuleConfig {

    @Bean
    FindAdministrativeTasksUseCase findAdministrativeTasksUseCase(
            AdministrativeTaskPort taskPort,
            TaskAccessDirectory accessDirectory,
            TaskNotificationOutbox notificationOutbox
    ) {
        return new FindAdministrativeTasksService(
                taskPort, accessDirectory, notificationOutbox
        );
    }

    @Bean
    ManageAdministrativeTasksUseCase manageAdministrativeTasksUseCase(
            AdministrativeTaskPort taskPort,
            TaskAccessDirectory accessDirectory,
            TaskNotificationOutbox notificationOutbox,
            RecordSecurityAuditUseCase auditUseCase,
            TaskOperationalMetrics metrics
    ) {
        var service = new ManageAdministrativeTasksService(
                taskPort, accessDirectory, notificationOutbox,
                auditUseCase, metrics
        );
        return new TransactionalManageAdministrativeTasksUseCase(service);
    }
}
