# FCM delivery failures

## Scope

This alert indicates repeated failures while dispatching durable task assignment
notifications. Task creation and assignment remain committed independently of
push delivery.

## Triage

1. Confirm `mycomplex-backend` is healthy and inspect structured logs using the
   correlation identifier. Logs never include FIDs or credentials.
2. Check `mycomplex_fcm_deliveries_total` by result and
   `mycomplex_fcm_delivery_failures_total` by safe reason.
3. Verify the configured Firebase project and that Application Default
   Credentials are available to the backend process.
4. Query only aggregate outbox status counts. Do not expose installation
   identifiers or external message identifiers.
5. For `invalid_destination`, the installation is revoked automatically. The
   user must explicitly activate notifications again in the web application.

Do not delete outbox rows or disable the alert to hide a delivery problem.
