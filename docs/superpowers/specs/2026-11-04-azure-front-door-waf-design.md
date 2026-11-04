# Azure Front Door Premium WAF Design

## Intent

Protect the public MyComplex frontend and API with Azure Front Door Premium and
Microsoft-managed WAF rules for a one-day academic validation, while preventing
out-of-pocket charges.

## Goals

- Route `/*` to Azure Static Web Apps and `/api/*` to the public API Gateway.
- Enable the Microsoft Default Rule Set and Bot Manager in `Detection` first.
- Validate Angular, Auth0, API calls, uploads, notifications, and WAF telemetry.
- Move to `Prevention` only after legitimate flows work.
- Delete Front Door and its WAF resources after today's evaluation.

## Non-goals

- Replace JWT validation or authorization in the Gateway and backend.
- Make the origins permanently private during this one-day validation.
- Purchase or configure a custom domain.
- Change the internal backend or PostgreSQL exposure model.

## Constraints

- Subscription `2b52c645-1c20-49db-bf81-6dd8e2fe3564` is a Free Trial with
  spending limit `On`.
- The existing monthly budget is USD 30; budget alerts do not stop resources.
- Front Door Premium is temporary and must be removed today.
- Existing Azure resources must remain intact during cleanup.

## Approaches

- Cloudflare Free plus a purchased domain: lowest recurring cost, but rejected
  for this validation because the team selected an Azure-only demonstration.
- Front Door Standard: lower cost, but only custom WAF rules and no Microsoft
  managed rule sets.
- Front Door Premium: selected because it provides managed WAF rules, bot
  protection, integrated diagnostics, and a generated Azure endpoint.

## Architecture

```text
Internet -> Azure Front Door Premium + WAF
             |-- /*      -> Azure Static Web Apps
             `-- /api/*  -> Container Apps API Gateway -> internal backend
```

The Angular production build must use the Front Door origin for API calls so
normal browser traffic does not bypass the WAF. Auth0 must allow the generated
Front Door origin as callback, logout, and web origin before login is tested.

## Safety and rollback

Deployment starts in `Detection`. The current Azure URLs remain available as a
rollback path. Cleanup removes only the Front Door profile, WAF policy, security
policy, diagnostic setting, and the temporary frontend/API configuration change
if the endpoint will not remain in use.

