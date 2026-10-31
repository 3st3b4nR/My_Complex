package com.mycomplex.observability.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.application.port.out.AppendSecurityAuditEventPort;
import com.mycomplex.observability.application.port.out.CurrentRequestContextPort;
import com.mycomplex.observability.application.port.out.ObservabilityMetricsPort;
import com.mycomplex.observability.application.service.SecurityAuditService;

@Configuration(proxyBeanMethods = false)
public class ObservabilityModuleConfig {

    @Bean
    Clock securityAuditClock() {
        return Clock.systemUTC();
    }

    @Bean
    RecordSecurityAuditUseCase recordSecurityAuditUseCase(
            AppendSecurityAuditEventPort appendPort,
            CurrentRequestContextPort requestContextPort,
            ObservabilityMetricsPort metricsPort,
            Clock securityAuditClock
    ) {
        return new SecurityAuditService(
                appendPort, requestContextPort, metricsPort,
                securityAuditClock
        );
    }
}
