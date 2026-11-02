package com.mycomplex.notifications.infrastructure.adapter.out.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.mycomplex.notifications.infrastructure.config.FcmProperties;

class FcmPropertiesWebConfigAdapterTest {

    @Test
    void exposesOnlyConfiguredPublicWebValues() {
        FcmProperties properties = configuredProperties();

        var config = new FcmPropertiesWebConfigAdapter(properties).load();

        assertThat(config.enabled()).isTrue();
        assertThat(config.apiKey()).isEqualTo("public-api-key");
        assertThat(config.projectId()).isEqualTo("project");
        assertThat(config.vapidKey()).isEqualTo("public-vapid-key");
    }

    @Test
    void reportsDisabledWhenRequiredPublicValuesAreIncomplete() {
        FcmProperties properties = configuredProperties();
        properties.setWebVapidKey("");

        var config = new FcmPropertiesWebConfigAdapter(properties).load();

        assertThat(config.enabled()).isFalse();
    }

    private FcmProperties configuredProperties() {
        FcmProperties properties = new FcmProperties();
        properties.setEnabled(true);
        properties.setWebApiKey("public-api-key");
        properties.setWebAuthDomain("project.firebaseapp.com");
        properties.setWebProjectId("project");
        properties.setWebStorageBucket("project.appspot.com");
        properties.setWebMessagingSenderId("sender");
        properties.setWebAppId("app");
        properties.setWebVapidKey("public-vapid-key");
        return properties;
    }
}
