package com.mycomplex.notifications.domain;

public record EmailDeliveryResult(
        Outcome outcome,
        String externalMessageId,
        String safeReason
) {

    public enum Outcome {
        SENT,
        RETRYABLE_FAILURE,
        PERMANENT_FAILURE
    }

    public static EmailDeliveryResult sent(String externalMessageId) {
        return new EmailDeliveryResult(
                Outcome.SENT,
                externalMessageId,
                null
        );
    }

    public static EmailDeliveryResult retryable(String safeReason) {
        return failed(Outcome.RETRYABLE_FAILURE, safeReason);
    }

    public static EmailDeliveryResult permanent(String safeReason) {
        return failed(Outcome.PERMANENT_FAILURE, safeReason);
    }

    private static EmailDeliveryResult failed(
            Outcome outcome,
            String safeReason
    ) {
        return new EmailDeliveryResult(outcome, null, safeReason);
    }
}
