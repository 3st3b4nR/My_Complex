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
import com.mycomplex.users.application.port.out.LoadLocalUserIdPort;
import com.mycomplex.users.application.service.FindPropertyMemberDetailsService;
import com.mycomplex.users.application.service.FindPropertyMembersService;
import com.mycomplex.users.application.service.FindUserPropertiesService;
import com.mycomplex.users.application.service.UpdatePropertyMemberService;
import com.mycomplex.users.application.service.FindPropertyMemberCandidateService;
import com.mycomplex.users.application.service.RegisterPropertyMemberService;
import com.mycomplex.users.application.service.UnlinkPropertyMemberService;
import com.mycomplex.users.application.service.InvitePropertyMemberService;
import com.mycomplex.users.application.service.LinkAuthenticatedUserService;
import com.mycomplex.users.application.service.AuditedRegisterPropertyMemberUseCase;
import com.mycomplex.users.application.service.AuditedUpdatePropertyMemberUseCase;
import com.mycomplex.users.application.service.AuditedUnlinkPropertyMemberUseCase;
import com.mycomplex.users.application.service.AuditedInvitePropertyMemberUseCase;
import com.mycomplex.users.application.service.AuditedLinkAuthenticatedUserUseCase;
import com.mycomplex.observability.api.RecordSecurityAuditUseCase;
import com.mycomplex.observability.api.SecurityEventReporter;
import com.mycomplex.properties.application.port.in.FindAvailableResidentialUnitsUseCase;
import com.mycomplex.properties.application.port.in.ResolveResidentialUnitsUseCase;
import com.mycomplex.users.application.service.PropertyMemberResidentialUnitEnricher;
import com.mycomplex.users.application.validation.ResidentialAssignmentValidator;
import com.mycomplex.users.infrastructure.adapter.in.security.AuthenticatedUserLinkingFilter;
import com.mycomplex.users.infrastructure.adapter.out.auth0.Auth0ManagementProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Auth0ManagementProperties.class)
public class UserModuleConfig {

    @Bean
    PropertyMemberResidentialUnitEnricher propertyMemberUnitEnricher(
            ResolveResidentialUnitsUseCase resolveUnitsUseCase
    ) {
        return new PropertyMemberResidentialUnitEnricher(
                resolveUnitsUseCase
        );
    }

    @Bean
    ResidentialAssignmentValidator residentialAssignmentValidator() {
        return new ResidentialAssignmentValidator();
    }

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
            LoadPropertyMembersPort loadPropertyMembersPort,
            PropertyMemberResidentialUnitEnricher unitEnricher
    ) {
        return new FindPropertyMembersService(
                loadPropertyMembersPort,
                unitEnricher
        );
    }

    @Bean
    FindPropertyMemberDetailsUseCase findPropertyMemberDetailsUseCase(
            LoadPropertyMembersPort loadPropertyMembersPort,
            FindAvailableResidentialUnitsUseCase findUnitsUseCase,
            PropertyMemberResidentialUnitEnricher unitEnricher
    ) {
        return new FindPropertyMemberDetailsService(
                loadPropertyMembersPort,
                findUnitsUseCase,
                unitEnricher
        );
    }

    @Bean
    UpdatePropertyMemberUseCase updatePropertyMemberUseCase(
            UpdatePropertyMemberPort updatePropertyMemberPort,
            LoadPropertyMembersPort loadPropertyMembersPort,
            LoadLocalUserIdPort loadLocalUserIdPort,
            FindAvailableResidentialUnitsUseCase findUnitsUseCase,
            PropertyMemberResidentialUnitEnricher unitEnricher,
            ResidentialAssignmentValidator assignmentValidator,
            RecordSecurityAuditUseCase auditUseCase,
            SecurityEventReporter securityReporter
    ) {
        UpdatePropertyMemberUseCase service = new UpdatePropertyMemberService(
                updatePropertyMemberPort,
                loadPropertyMembersPort,
                findUnitsUseCase,
                unitEnricher,
                assignmentValidator
        );
        UpdatePropertyMemberUseCase audited =
                new AuditedUpdatePropertyMemberUseCase(
                        service, loadPropertyMembersPort,
                        loadLocalUserIdPort, auditUseCase, securityReporter
                );
        return new TransactionalUpdatePropertyMemberUseCase(audited);
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
            LoadLocalUserIdPort loadLocalUserIdPort,
            ManagePropertyMembersPort managePropertyMembersPort,
            FindAvailableResidentialUnitsUseCase findUnitsUseCase,
            PropertyMemberResidentialUnitEnricher unitEnricher,
            ResidentialAssignmentValidator assignmentValidator,
            RecordSecurityAuditUseCase auditUseCase,
            SecurityEventReporter securityReporter
    ) {
        RegisterPropertyMemberUseCase service =
                new RegisterPropertyMemberService(
                loadPropertyMembersPort,
                managePropertyMembersPort,
                findUnitsUseCase,
                unitEnricher,
                assignmentValidator
        );
        RegisterPropertyMemberUseCase audited =
                new AuditedRegisterPropertyMemberUseCase(
                        service, loadLocalUserIdPort,
                        auditUseCase, securityReporter
                );
        return new TransactionalRegisterPropertyMemberUseCase(audited);
    }

    @Bean
    UnlinkPropertyMemberUseCase unlinkPropertyMemberUseCase(
            LoadPropertyMembersPort loadPropertyMembersPort,
            LoadLocalUserIdPort loadLocalUserIdPort,
            ManagePropertyMembersPort managePropertyMembersPort,
            RecordSecurityAuditUseCase auditUseCase,
            SecurityEventReporter securityReporter
    ) {
        UnlinkPropertyMemberUseCase service = new UnlinkPropertyMemberService(
                loadPropertyMembersPort, managePropertyMembersPort
        );
        UnlinkPropertyMemberUseCase audited =
                new AuditedUnlinkPropertyMemberUseCase(
                        service, loadPropertyMembersPort,
                        loadLocalUserIdPort, auditUseCase, securityReporter
                );
        return new TransactionalUnlinkPropertyMemberUseCase(audited);
    }

    @Bean
    InvitePropertyMemberUseCase invitePropertyMemberUseCase(
            UserInvitationPersistencePort persistencePort,
            ExternalIdentityPort externalIdentityPort,
            Auth0ManagementProperties properties,
            LoadLocalUserIdPort loadLocalUserIdPort,
            RecordSecurityAuditUseCase auditUseCase,
            SecurityEventReporter securityReporter
    ) {
        InvitePropertyMemberUseCase service = new InvitePropertyMemberService(
                persistencePort,
                externalIdentityPort,
                properties.getProvisioningTimeout(),
                properties.getResendCooldown()
        );
        return new AuditedInvitePropertyMemberUseCase(
                service, loadLocalUserIdPort, auditUseCase, securityReporter
        );
    }

    @Bean
    LinkAuthenticatedUserUseCase linkAuthenticatedUserUseCase(
            UserInvitationPersistencePort persistencePort,
            LoadLocalUserIdPort loadLocalUserIdPort,
            RecordSecurityAuditUseCase auditUseCase
    ) {
        LinkAuthenticatedUserUseCase service =
                new LinkAuthenticatedUserService(persistencePort);
        LinkAuthenticatedUserUseCase audited =
                new AuditedLinkAuthenticatedUserUseCase(
                        service, loadLocalUserIdPort, auditUseCase
                );
        return new TransactionalLinkAuthenticatedUserUseCase(audited);
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
