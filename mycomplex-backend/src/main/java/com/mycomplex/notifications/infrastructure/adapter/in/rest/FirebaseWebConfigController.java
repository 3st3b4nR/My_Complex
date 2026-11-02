package com.mycomplex.notifications.infrastructure.adapter.in.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycomplex.notifications.application.port.in.FindFirebaseWebConfigUseCase;
import com.mycomplex.notifications.infrastructure.adapter.in.rest.dto.FirebaseWebConfigResponse;

@RestController
@RequestMapping("/api/public/firebase-config")
public class FirebaseWebConfigController {

    private final FindFirebaseWebConfigUseCase useCase;

    public FirebaseWebConfigController(FindFirebaseWebConfigUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public FirebaseWebConfigResponse find() {
        var config = useCase.find();
        return new FirebaseWebConfigResponse(
                config.enabled(), config.apiKey(), config.authDomain(),
                config.projectId(), config.storageBucket(),
                config.messagingSenderId(), config.appId(), config.vapidKey()
        );
    }
}
