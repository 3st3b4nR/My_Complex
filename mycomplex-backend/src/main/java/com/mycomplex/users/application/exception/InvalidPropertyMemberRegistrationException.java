package com.mycomplex.users.application.exception;

public class InvalidPropertyMemberRegistrationException
        extends RuntimeException {

    public InvalidPropertyMemberRegistrationException(String message) {
        super(message);
    }

    public String safeMessage() {
        return getMessage();
    }
}
