package com.mycomplex.users.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mycomplex.users.application.port.in.FindUserPropertiesUseCase;
import com.mycomplex.users.application.port.in.FindPropertyMembersUseCase;
import com.mycomplex.users.application.port.in.FindPropertyMemberDetailsUseCase;
import com.mycomplex.users.application.port.in.UpdatePropertyMemberUseCase;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.application.port.out.LoadUserPropertiesPort;
import com.mycomplex.users.application.port.out.UpdatePropertyMemberPort;
import com.mycomplex.users.application.service.FindPropertyMemberDetailsService;
import com.mycomplex.users.application.service.FindPropertyMembersService;
import com.mycomplex.users.application.service.FindUserPropertiesService;
import com.mycomplex.users.application.service.UpdatePropertyMemberService;
import com.mycomplex.properties.application.port.in.FindAvailableResidentialUnitsUseCase;

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

    @Bean
    FindPropertyMembersUseCase findPropertyMembersUseCase(
            LoadPropertyMembersPort loadPropertyMembersPort
    ) {
        return new FindPropertyMembersService(
                loadPropertyMembersPort
        );
    }

    @Bean
    FindPropertyMemberDetailsUseCase findPropertyMemberDetailsUseCase(
            LoadPropertyMembersPort loadPropertyMembersPort,
            FindAvailableResidentialUnitsUseCase findUnitsUseCase
    ) {
        return new FindPropertyMemberDetailsService(
                loadPropertyMembersPort,
                findUnitsUseCase
        );
    }

    @Bean
    UpdatePropertyMemberUseCase updatePropertyMemberUseCase(
            UpdatePropertyMemberPort updatePropertyMemberPort,
            LoadPropertyMembersPort loadPropertyMembersPort,
            FindAvailableResidentialUnitsUseCase findUnitsUseCase
    ) {
        return new UpdatePropertyMemberService(
                updatePropertyMemberPort,
                loadPropertyMembersPort,
                findUnitsUseCase
        );
    }
}
