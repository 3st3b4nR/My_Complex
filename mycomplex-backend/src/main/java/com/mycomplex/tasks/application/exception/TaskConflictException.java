package com.mycomplex.tasks.application.exception;

public final class TaskConflictException extends RuntimeException {
    public TaskConflictException() {
        super("Task version conflict");
    }
}
