# Resend delivery failures

- **Signal:** `MyComplexResendDeliveryFailures` and `resend_email_send_seconds_count{outcome="error"}`.
- **Dashboard:** MyComplex Overview → Resend delivery outcomes, average duration, and external integration errors.
- **Safe checks:** confirm backend health; inspect aggregate error counts and the safe log message `External email delivery failed`; verify that `RESEND_ENABLED`, API key presence, sender authorization, and outbound connectivity are configured without printing their values.
- **Likely causes:** invalid/revoked API key, unverified sender/domain, Resend outage, rate limit, timeout, or DNS/network failure.
- **Recovery:** correct the secret or verified sender in the approved secret store, restore connectivity, and publish or update one controlled notification to confirm delivery.
- **Escalate:** repeated failures affect an entire property or continue after configuration is restored.
- **Do not:** do not print API keys, recipients, subjects, bodies, idempotency keys, or provider response bodies; do not resend notifications in bulk.
