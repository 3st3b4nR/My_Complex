package com.mycomplex.tasks.application.exception;

public final class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException() {
        super("Task not found");
    }
}
