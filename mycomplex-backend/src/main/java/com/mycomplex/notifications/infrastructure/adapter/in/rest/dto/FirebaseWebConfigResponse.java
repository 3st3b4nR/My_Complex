package com.mycomplex.notifications.infrastructure.adapter.in.rest.dto;

public record FirebaseWebConfigResponse(
        boolean enabled,
        String apiKey,
        String authDomain,
        String projectId,
        String storageBucket,
        String messagingSenderId,
        String appId,
        String vapidKey
) { }
