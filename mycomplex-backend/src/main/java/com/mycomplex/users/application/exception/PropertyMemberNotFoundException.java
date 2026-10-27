package com.mycomplex.users.application.exception;

public class PropertyMemberNotFoundException extends RuntimeException {

    public PropertyMemberNotFoundException() {
        super("Property member was not found");
    }
}
