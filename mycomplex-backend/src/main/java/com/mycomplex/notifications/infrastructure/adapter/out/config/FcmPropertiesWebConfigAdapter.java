package com.mycomplex.notifications.infrastructure.adapter.out.config;

import org.springframework.stereotype.Component;

import com.mycomplex.notifications.application.port.out.FirebaseWebConfigPort;
import com.mycomplex.notifications.infrastructure.config.FcmProperties;

@Component
public class FcmPropertiesWebConfigAdapter implements FirebaseWebConfigPort {

    private final FcmProperties properties;

    public FcmPropertiesWebConfigAdapter(FcmProperties properties) {
        this.properties = properties;
    }

    @Override
    public PublicFirebaseWebConfig load() {
        return new PublicFirebaseWebConfig(
                properties.isEnabled() && properties.hasWebConfiguration(),
                properties.getWebApiKey(),
                properties.getWebAuthDomain(),
                properties.getWebProjectId(),
                properties.getWebStorageBucket(),
                properties.getWebMessagingSenderId(),
                properties.getWebAppId(),
                properties.getWebVapidKey()
        );
    }
}
