package com.mycomplex.users.application.exception;

public class InvalidPropertyMemberUpdateException
        extends RuntimeException {

    public InvalidPropertyMemberUpdateException(String message) {
        super(message);
    }

    public String safeMessage() {
        return getMessage();
    }
}
