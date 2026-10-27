package com.mycomplex.users.application.exception;

public class LastPropertyAdministratorException extends RuntimeException {

    public LastPropertyAdministratorException() {
        super("The last active property administrator cannot be changed");
    }
}
