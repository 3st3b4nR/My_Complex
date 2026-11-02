package com.mycomplex.tasks.application.exception;

public final class TaskAccessDeniedException extends RuntimeException {
    public TaskAccessDeniedException() {
        super("Administrative task access denied");
    }
}
