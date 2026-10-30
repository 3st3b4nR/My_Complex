package com.mycomplex.users.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import com.mycomplex.users.application.port.in.FindUserPropertiesUseCase;
import com.mycomplex.users.application.port.in.FindPropertyMembersUseCase;
import com.mycomplex.users.application.port.in.FindPropertyMemberDetailsUseCase;
import com.mycomplex.users.application.port.in.UpdatePropertyMemberUseCase;
import com.mycomplex.users.application.port.in.FindPropertyMemberCandidateUseCase;
import com.mycomplex.users.application.port.in.RegisterPropertyMemberUseCase;
import com.mycomplex.users.application.port.in.UnlinkPropertyMemberUseCase;
import com.mycomplex.users.application.port.in.InvitePropertyMemberUseCase;
import com.mycomplex.users.application.port.in.LinkAuthenticatedUserUseCase;
import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.application.port.out.LoadUserPropertiesPort;
import com.mycomplex.users.application.port.out.UpdatePropertyMemberPort;
import com.mycomplex.users.application.port.out.ManagePropertyMembersPort;
import com.mycomplex.users.application.port.out.UserInvitationPersistencePort;
import com.mycomplex.users.application.port.out.ExternalIdentityPort;
import com.mycomplex.users.application.service.FindPropertyMemberDetailsService;
import com.mycomplex.users.application.service.FindPropertyMembersService;
import com.mycomplex.users.application.service.FindUserPropertiesService;
import com.mycomplex.users.application.service.UpdatePropertyMemberService;
import com.mycomplex.users.application.service.FindPropertyMemberCandidateService;
import com.mycomplex.users.application.service.RegisterPropertyMemberService;
import com.mycomplex.users.application.service.UnlinkPropertyMemberService;
import com.mycomplex.users.application.service.InvitePropertyMemberService;
import com.mycomplex.users.application.service.LinkAuthenticatedUserService;
import com.mycomplex.properties.application.port.in.FindAvailableResidentialUnitsUseCase;
import com.mycomplex.users.infrastructure.adapter.in.security.AuthenticatedUserLinkingFilter;
import com.mycomplex.users.infrastructure.adapter.out.auth0.Auth0ManagementProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Auth0ManagementProperties.class)
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

    @Bean
    FindPropertyMemberCandidateUseCase findPropertyMemberCandidateUseCase(
            LoadPropertyMembersPort loadPropertyMembersPort,
            ManagePropertyMembersPort managePropertyMembersPort,
            FindAvailableResidentialUnitsUseCase findUnitsUseCase
    ) {
        return new FindPropertyMemberCandidateService(
                loadPropertyMembersPort, managePropertyMembersPort,
                findUnitsUseCase
        );
    }

    @Bean
    RegisterPropertyMemberUseCase registerPropertyMemberUseCase(
            LoadPropertyMembersPort loadPropertyMembersPort,
            ManagePropertyMembersPort managePropertyMembersPort,
            FindAvailableResidentialUnitsUseCase findUnitsUseCase
    ) {
        return new RegisterPropertyMemberService(
                loadPropertyMembersPort,
                managePropertyMembersPort,
                findUnitsUseCase
        );
    }

    @Bean
    UnlinkPropertyMemberUseCase unlinkPropertyMemberUseCase(
            LoadPropertyMembersPort loadPropertyMembersPort,
            ManagePropertyMembersPort managePropertyMembersPort
    ) {
        return new UnlinkPropertyMemberService(
                loadPropertyMembersPort, managePropertyMembersPort
        );
    }

    @Bean
    InvitePropertyMemberUseCase invitePropertyMemberUseCase(
            UserInvitationPersistencePort persistencePort,
            ExternalIdentityPort externalIdentityPort,
            Auth0ManagementProperties properties
    ) {
        return new InvitePropertyMemberService(
                persistencePort,
                externalIdentityPort,
                properties.getProvisioningTimeout(),
                properties.getResendCooldown()
        );
    }

    @Bean
    LinkAuthenticatedUserUseCase linkAuthenticatedUserUseCase(
            UserInvitationPersistencePort persistencePort
    ) {
        return new LinkAuthenticatedUserService(persistencePort);
    }

    @Bean
    AuthenticatedUserLinkingFilter authenticatedUserLinkingFilter(
            LinkAuthenticatedUserUseCase useCase
    ) {
        return new AuthenticatedUserLinkingFilter(useCase);
    }

    @Bean
    FilterRegistrationBean<AuthenticatedUserLinkingFilter>
            disableAuthenticatedUserLinkingFilterRegistration(
                    AuthenticatedUserLinkingFilter filter
            ) {
        FilterRegistrationBean<AuthenticatedUserLinkingFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
