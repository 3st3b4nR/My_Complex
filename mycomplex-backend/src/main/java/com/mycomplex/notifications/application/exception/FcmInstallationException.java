package com.mycomplex.notifications.application.exception;

public class FcmInstallationException extends RuntimeException {
    private final String code;

    public FcmInstallationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }

    public String safeMessage() {
        return getMessage();
    }
}
