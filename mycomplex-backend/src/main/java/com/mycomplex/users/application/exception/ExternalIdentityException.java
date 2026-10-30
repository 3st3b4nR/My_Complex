package com.mycomplex.users.application.exception;

public class ExternalIdentityException extends UserInvitationException {

    private final String safeTechnicalMessage;

    public ExternalIdentityException(
            String code,
            String message,
            String safeTechnicalMessage
    ) {
        super(code, message);
        this.safeTechnicalMessage = safeTechnicalMessage;
    }

    public String safeTechnicalMessage() {
        return safeTechnicalMessage;
    }
}
