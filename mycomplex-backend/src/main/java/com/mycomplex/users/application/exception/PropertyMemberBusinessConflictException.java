package com.mycomplex.users.application.exception;

public class PropertyMemberBusinessConflictException
        extends RuntimeException {

    private final String code;

    public PropertyMemberBusinessConflictException(
            String code,
            String message
    ) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
