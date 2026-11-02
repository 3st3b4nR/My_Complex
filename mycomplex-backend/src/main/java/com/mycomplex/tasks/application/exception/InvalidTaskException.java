package com.mycomplex.tasks.application.exception;

public final class InvalidTaskException extends RuntimeException {
    private final String safeMessage;

    public InvalidTaskException(String safeMessage) {
        super("Invalid administrative task");
        this.safeMessage = safeMessage;
    }

    public String safeMessage() {
        return safeMessage;
    }
}
