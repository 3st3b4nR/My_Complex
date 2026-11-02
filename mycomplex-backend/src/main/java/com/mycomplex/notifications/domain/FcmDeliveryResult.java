package com.mycomplex.notifications.domain;

public record FcmDeliveryResult(
        Outcome outcome,
        String externalMessageId,
        String safeReason
) {
    public enum Outcome {
        SENT,
        RETRYABLE_FAILURE,
        PERMANENT_FAILURE,
        INVALID_DESTINATION,
        NOT_CONFIGURED
    }

    public static FcmDeliveryResult sent(String messageId) {
        return new FcmDeliveryResult(Outcome.SENT, messageId, null);
    }

    public static FcmDeliveryResult failed(Outcome outcome, String reason) {
        return new FcmDeliveryResult(outcome, null, reason);
    }
}
