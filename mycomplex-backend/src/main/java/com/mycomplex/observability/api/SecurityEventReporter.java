package com.mycomplex.observability.api;

public interface SecurityEventReporter {

    void administrativeAccessDenied(String operation);

    void invitationFailed(String operation, String reasonCode);
}
