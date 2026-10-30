package com.mycomplex.users.application.exception;

public class UserInvitationException extends RuntimeException {

    private final UserErrorCode errorCode;

    public UserInvitationException(String code, String message) {
        super(message);
        this.errorCode = UserErrorCode.from(code);
    }

    public UserInvitationException(
            UserErrorCode errorCode,
            String safeMessage
    ) {
        super(safeMessage);
        this.errorCode = errorCode;
    }

    public String code() {
        return errorCode.value();
    }

    public UserErrorCode errorCode() {
        return errorCode;
    }

    public String safeMessage() {
        return getMessage();
    }
}
