package com.mycomplex.users.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mycomplex.users.application.port.in.FindUserPropertiesUseCase;
import com.mycomplex.users.application.port.out.LoadUserPropertiesPort;
import com.mycomplex.users.application.service.FindUserPropertiesService;

@Configuration(proxyBeanMethods = false)
public class UserModuleConfig {

    @Bean
    FindUserPropertiesUseCase findUserPropertiesUseCase(
            LoadUserPropertiesPort loadUserPropertiesPort
    ) {
        return new FindUserPropertiesService(
                loadUserPropertiesPort
        );
    }
}