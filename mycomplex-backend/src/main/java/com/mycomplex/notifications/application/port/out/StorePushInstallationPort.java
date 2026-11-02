package com.mycomplex.notifications.application.port.out;

public interface StorePushInstallationPort {

    boolean register(String auth0Subject, String installationId);

    void revoke(String auth0Subject, String installationId);
}
