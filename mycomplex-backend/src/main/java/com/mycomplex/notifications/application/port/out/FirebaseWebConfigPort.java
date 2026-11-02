package com.mycomplex.notifications.application.port.out;

public interface FirebaseWebConfigPort {

    PublicFirebaseWebConfig load();

    record PublicFirebaseWebConfig(
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
