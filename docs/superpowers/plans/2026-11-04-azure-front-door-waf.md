# Azure Front Door Premium WAF Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Protect the deployed MyComplex frontend and API through a temporary Azure Front Door Premium WAF endpoint.

**Architecture:** One Front Door endpoint routes `/api/*` to the external Container Apps Gateway and all other paths to Azure Static Web Apps. A Premium WAF policy starts in Detection, emits diagnostics to the existing Log Analytics workspace, and moves to Prevention after functional verification.

**Tech Stack:** Azure Front Door Premium, Azure WAF, Azure CLI, Azure Static Web Apps, Azure Container Apps, Log Analytics, Angular, Auth0.

**Spec:** `docs/superpowers/specs/2026-11-04-azure-front-door-waf-design.md`

## Global Constraints

- Keep the Azure Free Trial spending limit `On` and the USD 30 monthly budget unchanged.
- Do not create Marketplace services or remove existing Azure resources.
- Use profile `afd-mycomplex-dev`, endpoint prefix `mycomplex-dev`, and policy `waf-mycomplex-dev`.
- Start WAF in `Detection`; switch to `Prevention` only after legitimate flows pass.
- Delete all Front Door/WAF resources today after explicit user confirmation.
- Store timestamps in UTC and never log tokens, credentials, or request bodies.

## Review Focus

- `/api/*` must route to Gateway while Angular routes continue to reach Static Web Apps.
- Auth0 callback/logout/web origins must accept the generated Front Door hostname.
- Multipart news image uploads must not be rejected as false positives.
- Front Door diagnostics must reach the existing Log Analytics workspace without sensitive bodies.
- Direct origin URLs remain a documented bypass during this temporary validation.

---

### Task 1: Preflight and cost guard

**Files:**
- Read: Azure subscription, budget, Front Door provider, current origin resources.

**Interfaces:**
- Consumes: Existing subscription and `rg-mycomplex-dev` resources.
- Produces: Confirmed spending limit, current budget, origin hostnames, and unique Front Door names.

- [ ] Verify the subscription reports `FreeTrial` and `SpendingLimit: On`.
- [ ] Verify budget `budget-mycomplex` remains USD 30 monthly.
- [ ] Register `Microsoft.Cdn` only if it is not already registered.
- [ ] Capture the Static Web Apps hostname, Gateway FQDN, and Log Analytics resource ID.
- [ ] Confirm no resource named `afd-mycomplex-dev` or `waf-mycomplex-dev` already exists.

### Task 2: Create the Front Door routing layer

**Files:**
- No repository files changed.

**Interfaces:**
- Consumes: Frontend and Gateway hostnames from Task 1.
- Produces: Front Door profile, endpoint, two origin groups, two origins, and routes for `/*` and `/api/*`.

- [ ] Create Premium profile `afd-mycomplex-dev` and endpoint prefix `mycomplex-dev`.
- [ ] Create `frontend-origin-group` with the Static Web Apps origin and HTTPS origin host header.
- [ ] Create `gateway-origin-group` with the Gateway origin and `/actuator/health` probe.
- [ ] Create HTTPS-only route `api-route` for `/api/*` to the Gateway.
- [ ] Create HTTPS-only route `frontend-route` for `/*` to Static Web Apps.
- [ ] Verify the Front Door endpoint serves Angular and returns the expected unauthenticated status for an API request.

### Task 3: Attach managed WAF protection and diagnostics

**Files:**
- No repository files changed.

**Interfaces:**
- Consumes: Front Door endpoint from Task 2.
- Produces: Detection-mode WAF policy, endpoint security association, and diagnostic settings.

- [ ] Create `waf-mycomplex-dev` with SKU `Premium_AzureFrontDoor` in Detection mode.
- [ ] Add the latest supported Microsoft Default Rule Set and Bot Manager Rule Set.
- [ ] Associate the policy with the Front Door endpoint through a security policy.
- [ ] Send access, health probe, and WAF logs to `log-mycomplex-dev`.
- [ ] Send a harmless SQL-injection-shaped request and confirm a WAF detection event without exposing secrets.

### Task 4: Route the application through Front Door

**Files:**
- Modify: `mycomplex-frontend/src/environments/environment.production.ts`
- Test: Existing frontend test suite and production build.

**Interfaces:**
- Consumes: Generated Front Door HTTPS origin.
- Produces: Angular production API URL pointing to the protected endpoint.

- [ ] Change only `environment.apiUrl` from the direct Gateway URL to the Front Door origin.
- [ ] Add the Front Door origin to Auth0 Allowed Callback URLs, Allowed Logout URLs, and Allowed Web Origins.
- [ ] Run `npm run test:ci` and `npm run build` from `mycomplex-frontend`.
- [ ] Deploy the frontend and verify login, property selection, tasks, news, meetings, image upload, email, and push flows.

### Task 5: Enable blocking and demonstrate protection

**Files:**
- No repository files changed.

**Interfaces:**
- Consumes: Verified Detection-mode policy from Task 3 and legitimate flows from Task 4.
- Produces: Prevention-mode WAF with evidence of allowed and blocked traffic.

- [ ] Change `waf-mycomplex-dev` to Prevention mode.
- [ ] Confirm a normal frontend/API request remains allowed.
- [ ] Confirm the controlled malicious request receives a WAF block response.
- [ ] Confirm the event appears in Log Analytics with timestamp, rule, action, and correlation data only.

### Task 6: Same-day cost cleanup

**Files:**
- Modify: `mycomplex-frontend/src/environments/environment.production.ts` only if Front Door is removed before the deployed frontend is restored.

**Interfaces:**
- Consumes: Explicit user confirmation after the demonstration.
- Produces: No remaining Front Door Premium/WAF billing resources and a functional direct Azure deployment.

- [ ] Record the Front Door profile, policy, endpoint, and evidence needed for the evaluation.
- [ ] Obtain explicit confirmation before deletion.
- [ ] Restore the direct Gateway API URL and redeploy Angular if Front Door will be removed.
- [ ] Delete only the Front Door profile and WAF policy resources.
- [ ] Verify the frontend, Gateway, backend, database, Storage, Key Vault, and Log Analytics still exist.
- [ ] Verify no Front Door or WAF resource remains in `rg-mycomplex-dev`.

