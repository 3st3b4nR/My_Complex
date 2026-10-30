package com.mycomplex.properties.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mycomplex.properties.application.port.in.FindAvailableResidentialUnitsUseCase;
import com.mycomplex.properties.application.port.in.ResolveResidentialUnitsUseCase;
import com.mycomplex.properties.application.port.out.LoadAvailableResidentialUnitsPort;
import com.mycomplex.properties.application.service.FindAvailableResidentialUnitsService;
import com.mycomplex.properties.application.service.ResolveResidentialUnitsService;

@Configuration(proxyBeanMethods = false)
public class PropertyModuleConfig {

    @Bean
    FindAvailableResidentialUnitsUseCase findAvailableResidentialUnitsUseCase(
            LoadAvailableResidentialUnitsPort loadUnitsPort
    ) {
        return new FindAvailableResidentialUnitsService(loadUnitsPort);
    }

    @Bean
    ResolveResidentialUnitsUseCase resolveResidentialUnitsUseCase(
            LoadAvailableResidentialUnitsPort loadUnitsPort
    ) {
        return new ResolveResidentialUnitsService(loadUnitsPort);
    }
}
