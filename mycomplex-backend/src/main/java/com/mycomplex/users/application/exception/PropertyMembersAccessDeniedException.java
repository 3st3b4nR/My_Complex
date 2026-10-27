package com.mycomplex.users.application.exception;

public final class PropertyMembersAccessDeniedException
        extends RuntimeException {

    public PropertyMembersAccessDeniedException() {
        super("Access denied to property members");
    }
}
