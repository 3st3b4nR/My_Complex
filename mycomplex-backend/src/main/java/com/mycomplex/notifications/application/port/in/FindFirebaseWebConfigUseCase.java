package com.mycomplex.notifications.application.port.in;

public interface FindFirebaseWebConfigUseCase {
    FirebaseWebConfig find();

    record FirebaseWebConfig(
            boolean enabled,
            String apiKey,
            String authDomain,
            String projectId,
            String storageBucket,
            String messagingSenderId,
            String appId,
            String vapidKey
    ) { }
}
