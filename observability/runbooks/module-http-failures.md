# News or meetings HTTP failures

- **Signal:** `MyComplexNewsHttpFailures` or `MyComplexMeetingHttpFailures`.
- **Dashboard:** MyComplex Overview → News and meetings HTTP 5xx and p95 latency.
- **Safe checks:** inspect `docker compose ps`; query Prometheus by templated `uri` and `status`; use Tempo with a trace ID and Loki `{service_name="mycomplex-backend"} | json | level="ERROR"`.
- **Likely causes:** PostgreSQL unavailable, validation translated incorrectly to 5xx, Resend/Firebase/Blob dependency failure, or a deployment regression.
- **Recovery:** restore the failed dependency or roll back the approved application change, then repeat one reversible request and confirm the error rate returns to zero.
- **Escalate:** failures affect writes, cross-property authorization, or persist for more than ten minutes.
- **Do not:** do not log request bodies, JWTs, user/property identifiers, meeting links, news text, or credentials; do not weaken authorization or delete persistent volumes.
