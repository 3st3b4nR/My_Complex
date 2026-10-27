package com.mycomplex.users.application.exception;

public class PropertyMemberConflictException extends RuntimeException {

    public PropertyMemberConflictException() {
        super("Property member was modified by another operation");
    }
}
