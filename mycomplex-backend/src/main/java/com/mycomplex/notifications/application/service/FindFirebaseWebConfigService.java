package com.mycomplex.notifications.application.service;

import com.mycomplex.notifications.application.port.in.FindFirebaseWebConfigUseCase;
import com.mycomplex.notifications.application.port.out.FcmDeliveryPort;
import com.mycomplex.notifications.infrastructure.config.FcmProperties;

public final class FindFirebaseWebConfigService
        implements FindFirebaseWebConfigUseCase {

    private final FcmProperties properties;
    private final FcmDeliveryPort deliveryPort;

    public FindFirebaseWebConfigService(
            FcmProperties properties,
            FcmDeliveryPort deliveryPort
    ) {
        this.properties = properties;
        this.deliveryPort = deliveryPort;
    }

    @Override
    public FirebaseWebConfig find() {
        boolean enabled = properties.isEnabled()
                && properties.hasWebConfiguration()
                && deliveryPort.isConfigured();
        return new FirebaseWebConfig(
                enabled,
                enabled ? properties.getWebApiKey() : "",
                enabled ? properties.getWebAuthDomain() : "",
                enabled ? properties.getWebProjectId() : "",
                enabled ? properties.getWebStorageBucket() : "",
                enabled ? properties.getWebMessagingSenderId() : "",
                enabled ? properties.getWebAppId() : "",
                enabled ? properties.getWebVapidKey() : ""
        );
    }
}
