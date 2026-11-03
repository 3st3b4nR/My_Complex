package com.mycomplex.notifications.application.port.in;

public interface ManagePushInstallationUseCase {

    void register(RegisterPushInstallationCommand command);

    void revoke(RevokePushInstallationCommand command);
}
