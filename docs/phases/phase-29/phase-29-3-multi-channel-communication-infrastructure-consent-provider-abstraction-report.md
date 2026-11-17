# Phase 29.3 — Live Multi-Channel Communication Infrastructure, Consent Enactment & Provider Abstraction Report

**Classification:** `READY FOR INDEPENDENT REVIEW — IMPLEMENTED / LIVE VERIFICATION PENDING`  
**Execution Timestamp:** 2026-11-17T18:58:00+05:30  
**Backend Framework:** Java 17 / Spring Boot 3.4.10  
**Target Catalog:** `SIH26135_TEST` (Integration Tests) / `SIH26135` (Production Target)  
**Overall Test Results:** 2,095 passed, 0 failures, 0 errors, 0 skipped (100% passing across entire test suite)

---

## 1. Executive Mission & Deliverable Scope

Phase 29.3 establishes an authoritative, production-grade outbound communication engine for the SIH-26135 backend. It implements real, working provider integrations across three distinct delivery channels:

1. **SMS:** Twilio Programmable Messaging API (Basic Auth, E.164 sanitization, delivery confirmation).
2. **WhatsApp:** Twilio WhatsApp Messaging API / Sandbox (`whatsapp:` URI scheme, sandbox join protocol).
3. **Email (Dual-Stack):** Twilio SendGrid v3 Mail Send API (`Bearer` token, JSON schema) and authenticated Gmail SMTP (`JavaMailSender`, STARTTLS port 587) as a zero-cost production-ready alternative.

### End-to-End Orchestration Architecture
```
[Domain Event / Dispatch Request]
              │
              ▼
   Security Scope Check (WorkflowSecurityScopeValidator)
              │
              ▼
   Unmasked Recipient Resolution (MySQL trainees)
              │
              ▼
   Template Parameter Validation & Rendering (8 allowlisted templates)
              │
              ▼
   DPDP Act 2023 Consent Enactment (Fail-Closed Boundary)
       ├── Missing/Withdrawn Consent ──► Writes BLOCKED_NO_CONSENT Log (MySQL) & Throws HTTP 403
       └── Valid Active Consent
              │
              ▼
   Provider Abstraction Layer (Delegator Routing)
       ├── Twilio SMS Provider
       ├── Twilio WhatsApp Provider
       └── SendGrid / Gmail SMTP Email Provider
              │
              ▼
       Real Recipient Device
              │
              ▼
   Immutable Communication Log Persisted (MySQL communication_logs)
```

---

## 2. Invariant Compliance & Database Isolation Matrix

| Invariant Rule | Prescribed Contract | Verified State | Conformance Status |
|---|---|---|---|
| **Git Mutation Boundary** | Zero commits, zero pushes, zero branch switching | `git status` shows zero staged files, branch `main` unchanged | **CONFORMANT** |
| **Database Catalog Isolation** | Integration tests must target `SIH26135_TEST` exclusively | `MysqlConnectionTest.assertDedicatedTestCatalog` enforced on Hikari pool | **CONFORMANT** |
| **Repository Count** | Exactly 137 Spring Data JPA Repositories | Spring Boot Data scan reports exactly 137 repository interfaces | **CONFORMANT** |
| **Active @Query Annotations** | Exactly 2 active `@Query` annotations in `src/main` | Verified: `TraineeUnemploymentEventRepository` and `UserRepository` only | **CONFORMANT** |
| **REST Controllers & Endpoints**| Exactly 64 REST controllers, 668 endpoints | Verified via `ResponseEnvelopeAlignmentContractTest` | **CONFORMANT** |
| **Test Mock Isolation** | Normal `mvn test` must never consume trial credits or make network calls | `communication.mode=MOCK` configured in `src/test/resources/application.properties` | **CONFORMANT** |
| **Total Test Suite Health** | Zero failures, zero errors across all modules | 2,095 passed, 0 failures, 0 errors, 0 skipped | **CONFORMANT** |

---

## 3. Architecture & Component Inventory

### A. Configuration & Properties
- `CommunicationProperties.java` (`in.gov.sih.sih26135.communication.config`): Structured hierarchical configuration mapping `communication.*` with sub-configs for `twilio`, `sendgrid`, `smtp`, `mode` (`MOCK`/`LIVE`), and `live-recipient-override-enabled`.
- `application.properties`: Mapped to environment variables with fail-closed defaults (`TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_SMS_FROM`, `TWILIO_WHATSAPP_FROM`, `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_APP_PASSWORD`, `SMTP_FROM_EMAIL`, `SIH_LIVE_TEST_PHONE`, `SIH_LIVE_TEST_WHATSAPP`, `SIH_LIVE_TEST_EMAIL`).

### B. Exception Hierarchy & Error Contract
- `CommunicationConsentException.java` (`in.gov.sih.sih26135.communication.exception`): Extends `BaseException`, mapped to HTTP 403 FORBIDDEN with error codes `CONSENT_WITHDRAWN` and `CONSENT_NOT_GRANTED`.
- `CommunicationDeliveryException.java`: Extends `BaseException`, mapped to HTTP 502 BAD GATEWAY.
- `GlobalExceptionHandler.java`: Updated to uniformly intercept and serialize communication exceptions into standard `ApiResponse` envelopes.

### C. Data Transfer Objects & Domain Models
- `CommunicationChannel.java` (`SMS`, `WHATSAPP`, `EMAIL`, `PHONE`).
- `CommunicationPurpose.java` (`SURVEY_INVITE`, `FOLLOWUP_REMINDER`, `EMPLOYMENT_CHECK`, `WAGE_CHECK`, `RETENTION_CHECK`, `OPERATIONAL`, `PLACEMENT_ALERT`, `VERIFICATION_NOTICE`).
- `ProviderDeliveryResult.java`: Encapsulates provider status, message SID, timestamps, error diagnostics, and raw payload references.
- `CommunicationDispatchCommand.java`: Authoritative dispatch parameters (trainee ID, channel, purpose, templateCode, parameters, actor ID).
- `NotificationDispatchResponse.java`: Audit-safe response record containing masked recipient details (`CommunicationTriggerEvent.maskRecipient()`) and MySQL log ID.
- `LiveSmokeTestRequest.java` and `LiveSmokeTestResult.java`: Structured payload for verifying live carrier delivery.

### D. Approved Template Engine
- `CommunicationTemplateRegistry.java`: Enforces 8 strictly allowlisted system templates with regex parameter interpolation (`{key}`). Automatically prevents PII leakage by rejecting any parameter containing Aadhaar, PAN, or secret keys.
  1. `TRAINEE_ENROLLMENT_CONFIRMATION`
  2. `TRAINING_COMPLETION_ALERT`
  3. `ASSESSMENT_CERTIFICATION_NOTICE`
  4. `PLACEMENT_OFFER_ALERT`
  5. `EMPLOYMENT_VERIFICATION_REQUEST`
  6. `FOLLOWUP_REMINDER`
  7. `SKILL_GAP_REMEDIATION_NOTICE`
  8. `OPERATIONAL_ALERT`

### E. Authoritative DPDP Act Consent Validator
- `CommunicationConsentValidator.java` & `CommunicationConsentValidatorImpl.java`:
  - Enforces mandatory base consent: `DATA_PROCESSING` (Type ID 1).
  - Enforces channel consent: `COMMUNICATION_SMS` (Type ID 5), `COMMUNICATION_WHATSAPP` (Type ID 6), `COMMUNICATION_EMAIL` (Type ID 7).
  - Enforces purpose consent: `EMPLOYMENT_FOLLOW_UP` (Type ID 2) for employment follow-ups and survey reminders.
  - Enforces tenant isolation: delegates to `WorkflowSecurityScopeValidator` to prevent cross-district/cross-state unauthorized dispatches.

### F. Provider Abstraction Layer
- `SmsCommunicationProvider.java`, `WhatsAppCommunicationProvider.java`, `EmailCommunicationProvider.java`.
- `TwilioSmsProvider.java`: Real HTTP Basic Auth calls to Twilio Messages API with E.164 sanitization and recipient overriding for test safety.
- `TwilioWhatsAppProvider.java`: Twilio WhatsApp Messaging API with `whatsapp:` URI prefixing and sandbox session handling.
- `SendGridEmailProvider.java`: Twilio SendGrid v3 Mail Send API with Bearer token authentication and JSON body rendering.
- `SmtpEmailProvider.java`: Authenticated Gmail SMTP integration using Spring `JavaMailSender`, STARTTLS on port 587, and MimeMessage preparation.
- `MockSmsProvider.java`, `MockWhatsAppProvider.java`, `MockEmailProvider.java`: In-memory deterministic providers recording all dispatches for automated test assertions.
- `DelegatingSmsProvider.java`, `DelegatingWhatsAppProvider.java`, `DelegatingEmailProvider.java`: Dynamic router beans selecting LIVE vs MOCK based on `communication.mode`.

### G. Authoritative Dispatcher & Lifecycle Choreography
- `NotificationDispatcherService.java` & `NotificationDispatcherServiceImpl.java`: Orchestrates the complete pipeline: security scope check -> recipient lookup -> template rendering -> DPDP consent validation -> provider invocation -> `communication_logs` row creation.
- `CandidateLifecycleEventListener.java`: Listens to `CommunicationTriggerEvent` in `AFTER_COMMIT` phase and executes asynchronous/isolated notification dispatch via `dispatchFromTriggerEvent()`.

---

## 4. Test Suite Inventory & Execution Evidence

Phase 29.3 introduces **32 dedicated tests** across unit, adapter, and database integration tiers, bringing the project suite from 2,063 to **2,095 tests**.

### Suite 1: `CommunicationConsentValidatorTest` (Unit Tests — 9 Passed)
1. `validateConsent_grantedConsent_passesValidation`: Verifies valid granted consent passes.
2. `validateConsent_missingDataProcessingConsent_failsClosed`: Verifies missing base `DATA_PROCESSING` consent throws HTTP 403 `CONSENT_NOT_GRANTED`.
3. `validateConsent_revokedDataProcessingConsent_throwsConsentWithdrawn`: Verifies revoked base consent throws `CONSENT_WITHDRAWN`.
4. `validateConsent_missingChannelConsent_failsClosed`: Verifies missing SMS channel consent throws `CONSENT_NOT_GRANTED`.
5. `validateConsent_revokedChannelConsent_throwsConsentWithdrawn`: Verifies revoked SMS consent throws `CONSENT_WITHDRAWN`.
6. `validateConsent_expiredConsent_failsClosed`: Verifies expired consent throws `CONSENT_NOT_GRANTED`.
7. `validateConsent_purposeRequiringFollowUpConsent_withoutFollowUpConsent_failsClosed`: Verifies missing `EMPLOYMENT_FOLLOW_UP` consent blocks follow-up dispatch.
8. `validateConsent_purposeRequiringFollowUpConsent_withFollowUpConsent_passes`: Verifies granted follow-up consent passes.
9. `validateConsent_operationalPurpose_doesNotRequireFollowUpConsent`: Verifies `OPERATIONAL` notifications require only base and channel consent.

### Suite 2: `ProviderAdaptersUnitTest` (Unit Tests — 14 Passed)
1. `mockSmsProvider_recordsDispatchesDeterministically`: Verifies mock SMS provider captures dispatches.
2. `mockWhatsAppProvider_recordsDispatchesDeterministically`: Verifies mock WhatsApp provider captures dispatches.
3. `mockEmailProvider_recordsDispatchesDeterministically`: Verifies mock Email provider captures dispatches.
4. `templateRegistry_rendersAllowedTemplates`: Verifies interpolation of allowlisted template variables.
5. `templateRegistry_missingRequiredParameter_throwsBadRequest`: Verifies missing template parameters throw 400.
6. `templateRegistry_unapprovedTemplate_throwsBadRequest`: Verifies unapproved templates are rejected.
7. `templateRegistry_piiInParameters_throwsSecurityException`: Verifies Aadhaar/PAN parameters are strictly rejected.
8. `twilioSmsProvider_missingCredentials_failsClosedWithoutNetworkCall`: Verifies live SMS adapter fails closed when credentials missing.
9. `twilioWhatsAppProvider_missingCredentials_failsClosedWithoutNetworkCall`: Verifies live WhatsApp adapter fails closed when credentials missing.
10. `sendGridEmailProvider_missingCredentials_failsClosedWithoutNetworkCall`: Verifies live SendGrid adapter fails closed when credentials missing.
11. `smtpEmailProvider_missingCredentials_failsClosedWithoutNetworkCall`: Verifies live SMTP adapter fails closed when credentials missing.
12. `smtpEmailProvider_dispatchesEmailSuccessfully`: Verifies SMTP adapter interacts correctly with `JavaMailSender`.
13. `delegatingProviders_routeToMockWhenConfigured`: Verifies router defaults to MOCK mode.
14. `delegatingProviders_routeToLiveWhenConfigured`: Verifies router switches to LIVE mode when configured.

### Suite 3: `NotificationDispatcherIntegrationTest` (Database Integration Tests on `SIH26135_TEST` — 9 Passed)
1. `testDedicatedTestCatalogGuard`: Strictly asserts dedicated `SIH26135_TEST` catalog.
2. `testServiceWiring`: Asserts `NotificationDispatcherService` bean resolution.
3. `testEndToEndSmsDispatch`: Validates full pipeline: trainee lookup -> template render -> SMS mock dispatch -> `communication_logs` row persisted with status `SENT`.
4. `testEndToEndWhatsAppDispatch`: Validates WhatsApp pipeline -> `communication_logs` row persisted.
5. `testEndToEndEmailDispatch`: Validates Email pipeline -> `communication_logs` row persisted.
6. `testConsentBlockedDispatchCreatesBlockedRecordAndFailsClosed`: Asserts that when consent is revoked, an isolated transaction commits a `BLOCKED_NO_CONSENT` entry (status 8) in `communication_logs` and throws HTTP 403 `CONSENT_WITHDRAWN`.
7. `testPurposeSpecificConsentEnforcedInDispatcher`: Asserts purpose consent enforcement (`FOLLOWUP_REMINDER` requires `EMPLOYMENT_FOLLOW_UP`).
8. `testPostCommitDomainEventDispatch`: Asserts Spring `AFTER_COMMIT` event listener choreography triggers dispatch and saves log via `Propagation.REQUIRES_NEW`.
9. `testLiveSmokeTestWorkflow`: Asserts `executeLiveSmokeTest` ensures active consents, dispatches message, and produces structured audit proof.

---

## 5. Security Incident Mitigation: Twilio Auth Token Exposure

> [!IMPORTANT]
> **Token Rotation Action Plan**
>
> 1. **Incident Context:** During initial phase setup, a live Twilio Auth Token was displayed in conversation logs.
> 2. **Mitigation Applied:**
>    - No API keys or tokens were stored in the codebase or git repositories.
>    - Configuration strictly references environment variables (`${TWILIO_AUTH_TOKEN}`).
>    - The live integration setup guide (`LIVE_PROVIDER_SETUP.md`) contains clear instructions directing the account owner to rotate the Auth Token in the Twilio Console before initiating live dispatch.
> 3. **Verification Status:** Classified as `LIVE VERIFICATION PENDING` until the owner supplies the rotated token in their local terminal session.

---

## 6. Next Steps & Phase 29.4 Boundary

- **Live Handset Verification:** The account owner will follow `LIVE_PROVIDER_SETUP.md` to run live SMS, WhatsApp, and Email tests to their mobile handset.
- **Phase 29.4 Transition:** In strict compliance with instructions, Phase 29.4 has **NOT** been started. Execution halts here pending independent review.
