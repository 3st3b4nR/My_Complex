package com.mycomplex.notifications.infrastructure.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mycomplex.notifications.application.port.in.ManagePushInstallationUseCase;
import com.mycomplex.notifications.application.port.in.RegisterPushInstallationCommand;
import com.mycomplex.notifications.application.port.in.RevokePushInstallationCommand;
import com.mycomplex.notifications.infrastructure.adapter.in.rest.dto.PushInstallationRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/me/push-installations")
public class PushInstallationController {

    private final ManagePushInstallationUseCase useCase;

    public PushInstallationController(
            ManagePushInstallationUseCase useCase
    ) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PushInstallationRequest request
    ) {
        useCase.register(new RegisterPushInstallationCommand(
                jwt.getSubject(),
                request.installationId()
        ));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PushInstallationRequest request
    ) {
        useCase.revoke(new RevokePushInstallationCommand(
                jwt.getSubject(),
                request.installationId()
        ));
    }
}
