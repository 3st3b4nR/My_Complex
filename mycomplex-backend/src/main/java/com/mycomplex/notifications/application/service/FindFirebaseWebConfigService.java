package com.mycomplex.notifications.application.service;

import com.mycomplex.notifications.application.port.in.FindFirebaseWebConfigUseCase;
import com.mycomplex.notifications.application.port.out.FcmDeliveryPort;
import com.mycomplex.notifications.application.port.out.FirebaseWebConfigPort;

public final class FindFirebaseWebConfigService
        implements FindFirebaseWebConfigUseCase {

    private final FirebaseWebConfigPort configPort;
    private final FcmDeliveryPort deliveryPort;

    public FindFirebaseWebConfigService(
            FirebaseWebConfigPort configPort,
            FcmDeliveryPort deliveryPort
    ) {
        this.configPort = configPort;
        this.deliveryPort = deliveryPort;
    }

    @Override
    public FirebaseWebConfig find() {
        var config = configPort.load();
        boolean enabled = config.enabled() && deliveryPort.isConfigured();
        return new FirebaseWebConfig(
                enabled,
                enabled ? config.apiKey() : "",
                enabled ? config.authDomain() : "",
                enabled ? config.projectId() : "",
                enabled ? config.storageBucket() : "",
                enabled ? config.messagingSenderId() : "",
                enabled ? config.appId() : "",
                enabled ? config.vapidKey() : ""
        );
    }
}
