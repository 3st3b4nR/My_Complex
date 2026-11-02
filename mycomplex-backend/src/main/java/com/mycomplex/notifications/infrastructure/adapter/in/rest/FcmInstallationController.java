package com.mycomplex.notifications.infrastructure.adapter.in.rest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mycomplex.notifications.application.port.in.ManageFcmInstallationUseCase;
import com.mycomplex.notifications.infrastructure.adapter.in.rest.dto.FcmInstallationResponse;
import com.mycomplex.notifications.infrastructure.adapter.in.rest.dto.RegisterFcmInstallationRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/me/fcm-installations")
public class FcmInstallationController {

    private final ManageFcmInstallationUseCase useCase;

    public FcmInstallationController(ManageFcmInstallationUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FcmInstallationResponse register(
            @Valid @RequestBody RegisterFcmInstallationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var installation = useCase.register(
                jwt.getSubject(), request.installationId()
        );
        return new FcmInstallationResponse(installation.id(), "ACTIVE");
    }

    @DeleteMapping("/{installationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID installationId,
            @AuthenticationPrincipal Jwt jwt) {
        useCase.revoke(jwt.getSubject(), installationId);
    }
}
