package com.mycomplex.properties.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mycomplex.properties.application.port.in.FindAvailableResidentialUnitsUseCase;
import com.mycomplex.properties.application.port.out.LoadAvailableResidentialUnitsPort;
import com.mycomplex.properties.application.service.FindAvailableResidentialUnitsService;

@Configuration(proxyBeanMethods = false)
public class PropertyModuleConfig {

    @Bean
    FindAvailableResidentialUnitsUseCase findAvailableResidentialUnitsUseCase(
            LoadAvailableResidentialUnitsPort loadUnitsPort
    ) {
        return new FindAvailableResidentialUnitsService(loadUnitsPort);
    }
}
