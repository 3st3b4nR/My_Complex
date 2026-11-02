package com.mycomplex.notifications.application.port.in;

import com.mycomplex.notifications.domain.FcmInstallation;

public interface ManageFcmInstallationUseCase {
    FcmInstallation register(String auth0Subject, String installationId);
    void revoke(String auth0Subject, java.util.UUID installationId);
}
