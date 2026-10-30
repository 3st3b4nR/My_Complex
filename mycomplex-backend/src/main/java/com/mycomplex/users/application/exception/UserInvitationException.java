package com.mycomplex.users.application.exception;

public class UserInvitationException extends RuntimeException {

    private final String code;

    public UserInvitationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
