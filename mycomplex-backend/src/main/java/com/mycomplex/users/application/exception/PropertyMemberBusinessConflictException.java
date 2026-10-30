package com.mycomplex.users.application.exception;

public class PropertyMemberBusinessConflictException
        extends RuntimeException {

    private final UserErrorCode errorCode;

    public PropertyMemberBusinessConflictException(
            String code,
            String message
    ) {
        super(message);
        this.errorCode = UserErrorCode.from(code);
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
