# Meeting reminder batch failures

- **Signal:** `MyComplexMeetingReminderFailures` and `mycomplex_meeting_reminder_batch_seconds_count{outcome="error"}`.
- **Dashboard:** MyComplex Overview → Meeting reminder batches and external integration errors.
- **Safe checks:** confirm backend health and scheduler activity; inspect the safe scheduler error log and correlated trace; check PostgreSQL plus aggregate Resend/Firebase outcomes.
- **Likely causes:** database query failure, notification recipient lookup failure, Resend/Firebase outage, or an application regression.
- **Recovery:** restore the failing dependency; allow the next scheduled batch to run; confirm a successful outcome before considering a controlled manual retry.
- **Escalate:** the reminder window is at risk, failures repeat across batches, or duplicate delivery is suspected.
- **Do not:** do not change meeting times, mark reminders as sent manually, expose meeting links or member identities, disable the scheduler, or delete outbox/persistent data.
