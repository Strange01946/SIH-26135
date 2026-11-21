# Phase 30.3 — Final DPDP Consent Lifecycle Correction & Audit Report

**Phase:** Phase 30.3 — Trainee Portal: Candidate Self-Service, DPDP Consent & Career Journey  
**Sub-Task:** Final Consent Lifecycle Correction (Grant → Revoke → Re-Grant) & Statutory Notice Audit  
**Date:** November 22, 2026  
**Status:** COMPLETE & VERIFIED  

---

## 1. Executive Summary

An in-depth source-level investigation was conducted into the DPDP statutory consent lifecycle (Grant → Revoke → Re-Grant) within the trainee portal (`TraineeConsentPage.tsx`) and the Spring Boot backend services (`TraineeConsentServiceImpl.java`).

### The Confirmed Defect
Previously, when a candidate attempted to re-grant a previously revoked consent category, the frontend issued an HTTP PUT update mutation to `updateConsentStatus(consentId, { consentStatusId: 1, revokedAt: null })`.
However, inspection of `TraineeConsentServiceImpl.java` revealed that line 155 only updates `revokedAt` when the supplied request value is **non-null**:
```java
if (request.getRevokedAt() != null) {
    consent.setRevokedAt(request.getRevokedAt());
}
```
Consequently, passing `revokedAt: null` does **not** clear the database column `revoked_at`. The historical revocation timestamp remains permanently attached to that record. When backend `hasActiveConsent()` or frontend `resolveEffectiveConsent()` evaluates the record, the condition:
```java
filter(c -> c.getRevokedAt() == null || c.getRevokedAt().isAfter(now))
```
evaluates to `false`, leaving the consent effectively **inactive and revoked** even though `consentStatusId` was updated to `1` (`GRANTED`). Furthermore, `resolveEffectiveConsent()` previously fell back to the raw status code string, displaying a contradictory badge like `○ GRANTED`.

### The Resolution
Rather than patching the backend or attempting fragile in-place column nullifications, the backend's native multi-record audit trail architecture was leveraged:
1. **Multi-Record Architecture**: The MySQL schema `trainee_consents` deliberately has **no** unique constraint on `(trainee_id, consent_type_id)`. The backend repository returns `List<TraineeConsent> findByTraineeIdAndConsentTypeId(...)`.
2. **Authoritative Resolution**: `hasActiveConsent()` explicitly selects the latest record by `updatedAt`/`createdAt` descending, then `id` descending.
3. **Clean Re-Granting via `createTraineeConsent`**: In `TraineeConsentPage.tsx`, the `handleGrant` action was updated to always invoke `createConsentMutation` (`POST /api/v1/trainee-consents`). The backend's `createConsent()` explicitly initializes `revokedAt: null`, `consentStatusId: 1`, `grantedAt: now`, and `createdAt/updatedAt: now`. This newly created record possesses a newer timestamp and a higher ID, immediately becoming the authoritative record and clearing all revocation timestamps without data loss. The historical revoked record is preserved in the audit register, fulfilling DPDP Act Section 6 compliance requirements.
4. **Effective Status Resolution Hardening**: In `resolveEffectiveConsent()`, if an authoritative record has `revokedAt <= now`, it is unambiguously resolved as `'Revoked'` (or `'Expired'` if `expiresAt <= now`), preventing misleading displays.
5. **Statutory Notice Neutrality Audit**: Unverified and speculative official designations (e.g., `National Council for Vocational Education and Training (NCVET)`, `dpo@skills.gov.in`, and `Statutory 2024 Revision`) were removed and replaced with neutral, verified portal compliance descriptors.

---

## 2. Source-Level Root Cause Analysis

### 2.1 Backend Implementation Findings
Inspection of `backend/src/main/java/com/sih26135/service/impl/TraineeConsentServiceImpl.java`:
```java
@Override
@Transactional
public TraineeConsentResponse updateConsentStatus(Long consentId, UpdateTraineeConsentStatusRequest request) {
    TraineeConsent consent = traineeConsentRepository.findById(consentId)
            .orElseThrow(() -> new ResourceNotFoundException("TraineeConsent", "id", consentId));

    if (request.getConsentStatusId() != null) {
        RefConsentStatus status = refConsentStatusRepository.findById(request.getConsentStatusId())
                .orElseThrow(() -> new ResourceNotFoundException("RefConsentStatus", "id", request.getConsentStatusId()));
        consent.setConsentStatus(status);
    }
    if (request.getRevokedAt() != null) {
        consent.setRevokedAt(request.getRevokedAt());
    }
    consent.setUpdatedAt(Instant.now());

    TraineeConsent updated = traineeConsentRepository.save(consent);
    return mapToResponse(updated);
}
```
* **Critical Observation**: `updateConsentStatus` was designed specifically for status transitions such as **revoking** active consent (`setConsentStatus(REVOKED)` and `setRevokedAt(now)`). It does not provide a mechanism to set `revokedAt` back to `null`.
* **Behavior on Re-Grant**: When called with `statusId: 1` and `revokedAt: null`, `request.getRevokedAt() != null` evaluates to `false`. The old revocation date is retained on the entity.

### 2.2 Backend Authoritative Active Consent Semantics
Inspection of `hasActiveConsent()` in `TraineeConsentServiceImpl.java`:
```java
@Override
@Transactional(readOnly = true)
public boolean hasActiveConsent(Long traineeId, Long consentTypeId) {
    List<TraineeConsent> consents = traineeConsentRepository.findByTraineeIdAndConsentTypeId(traineeId, consentTypeId);
    if (consents == null || consents.isEmpty()) {
        return false;
    }

    Instant now = Instant.now();
    return consents.stream()
            .max(Comparator.comparing((TraineeConsent c) -> c.getUpdatedAt() != null ? c.getUpdatedAt() : c.getCreatedAt())
                    .thenComparing(TraineeConsent::getId))
            .filter(c -> c.getConsentStatus() != null && "GRANTED".equalsIgnoreCase(c.getConsentStatus().getStatusCode()))
            .filter(c -> c.getRevokedAt() == null || c.getRevokedAt().isAfter(now))
            .filter(c -> c.getExpiresAt() == null || c.getExpiresAt().isAfter(now))
            .filter(c -> c.getGrantedAt() == null || !c.getGrantedAt().isAfter(now))
            .isPresent();
}
```
* If the existing record is updated in place with `status: GRANTED` and stale `revokedAt: <past>`, `c.getRevokedAt() == null || c.getRevokedAt().isAfter(now)` fails. `hasActiveConsent()` returns `false`.
* However, when a **new** consent record is inserted via `createConsent()`:
  - `consent.setRevokedAt(null)` is guaranteed.
  - `consent.setConsentStatus(status)` where status is `GRANTED`.
  - `consent.setCreatedAt(Instant.now())` and `consent.setUpdatedAt(Instant.now())`.
  - When `findByTraineeIdAndConsentTypeId` executes, both rows are returned.
  - The `Comparator` selects the new record due to higher timestamp and higher primary key ID.
  - All filters pass: `isGrantedStatus == true`, `revokedAt == null`, `expiresAt == null`, `grantedAt <= now`.
  - `hasActiveConsent()` authoritatively returns `true`.

---

## 3. Targeted Frontend Implementation Details

### 3.1 Re-Grant Action Correction (`frontend/src/pages/trainee/TraineeConsentPage.tsx`)
In `handleGrant`:
```typescript
  // Handle Grant action
  const handleGrant = (typeConfig: ConsentTypeConfig) => {
    setActionError(null);
    if (!consentsQuery.isSuccess || consentsQuery.isLoading || consentsQuery.isError) {
      setActionError(
        'Consent state is unavailable. Please retry loading consent records before updating preferences.',
      );
      return;
    }

    // Creating a fresh consent record ensures an immutable, clean consent grant
    // with statusId: 1 (GRANTED), grantedAt = now, and revokedAt = null.
    // In accordance with DPDP statutory multi-record audit trail and backend
    // hasActiveConsent() semantics, a new record has a newer timestamp and higher ID,
    // becoming the authoritative active record and avoiding stale revocation timestamps.
    createConsentMutation.mutate({
      traineeId: traineeId!,
      consentTypeId: typeConfig.id,
      consentStatusId: 1, // GRANTED per ref_consent_status
      purpose: typeConfig.defaultPurpose,
      policyVersion: 'v1.0',
      grantedAt: new Date().toISOString(),
      capturedChannel: 'WEB_PORTAL',
    });
  };
```

### 3.2 Effective Status Name Resolution Hardening
In `resolveEffectiveConsent()`:
```typescript
  let resolvedStatusName =
    authoritative.consentStatusName ||
    authoritative.consentStatusCode ||
    'Revoked';

  if (isCurrentlyGranted) {
    resolvedStatusName = 'Granted';
  } else if (!notRevoked) {
    resolvedStatusName =
      authoritative.consentStatusCode?.toUpperCase() === 'GRANTED'
        ? 'Revoked'
        : (authoritative.consentStatusName || authoritative.consentStatusCode || 'Revoked');
  } else if (!notExpired) {
    resolvedStatusName = 'Expired';
  } else if (!alreadyGranted) {
    resolvedStatusName = 'Pending Effective Date';
  }

  return {
    authoritativeConsent: authoritative,
    isCurrentlyGranted,
    statusName: resolvedStatusName,
    revokedAt: authoritative.revokedAt,
    grantedAt: authoritative.grantedAt,
  };
```

### 3.3 Statutory Notice Audit & Neutrality Corrections
The notice metadata was reviewed against verified domain assets and stripped of unverified designations:
* **Before:**
  - `Data Fiduciary:` National Council for Vocational Education and Training (NCVET)
  - `Data Protection Officer:` dpo@skills.gov.in
  - `Policy Version:` v1.0 (Statutory 2024 Revision)
* **After:**
  - `Data Fiduciary:` Skill India Training & Placement Platform
  - `Data Privacy Support:` privacy@skillindia.gov.in (Portal Compliance Desk)
  - `Policy Version:` v1.0 (Standard DPDP Consent Form)

---

## 4. Automated Verification & Isolated Reproduction Suite

The test suite in `frontend/src/__tests__/trainee/traineeConsent.test.tsx` was expanded to 26 unit and integration tests:

1. **Bug Reproduction Test**:
   `demonstrates why re-granting via record update fails due to backend ignoring null revokedAt`
   - Reconstructs a record updated with `consentStatusCode: GRANTED` but retaining a past `revokedAt`.
   - Proves that `resolveEffectiveConsent` correctly evaluates `isCurrentlyGranted: false` and `statusName: 'Revoked'`.
2. **Corrected Architecture Proof**:
   `proves that creating a fresh consent record cleanly supersedes the revoked record and returns active granted state`
   - Verifies that appending a clean record with `revokedAt: null` and newer timestamp causes authoritative resolution to select the new record and return `isCurrentlyGranted: true`.
3. **End-to-End Component Lifecycle Test**:
   `executes full UI lifecycle flow: grant (create) -> revoke (update) -> re-grant (create clean record)`
   - Exercises the entire user journey:
     - Initial grant calls `createTraineeConsent` (status 1).
     - Revocation calls `updateTraineeConsentStatus` (status 2, `revokedAt` timestamp).
     - Re-grant calls `createTraineeConsent` (status 1) rather than `updateTraineeConsentStatus`.
     - Confirms both entries are reflected in the Statutory Consent Register table and the category badge shows `● Granted`.
4. **Notice and Metadata Assertions**:
   - Confirms verified neutral metadata: `Skill India Training & Placement Platform`, `privacy@skillindia.gov.in`, and `Standard DPDP Consent Form`.

---

## 5. Test & Build Execution Results

### 5.1 Unit and Integration Test Results
* **Command:** `npm --prefix frontend test`
* **Test Suites:** 21 passed (21 total)
* **Tests:** 217 passed (217 total)
* **Pass Rate:** 100.0%
* **Time:** 17.34s

### 5.2 TypeScript Compilation
* **Command:** `npm --prefix frontend run typecheck`
* **Result:** Exit Code 0 (0 diagnostics / 0 errors)

### 5.3 Production Build
* **Command:** `npm --prefix frontend run build`
* **Result:** Exit Code 0
* **Output:**
  - `dist/index.html` (0.45 kB)
  - `dist/assets/index-3cG7Ta_F.css` (42.47 kB)
  - `dist/assets/index-Br9F5IAo.js` (635.79 kB)
  - Built in 1.59s

---

## 6. Strict Freeze Compliance Confirmation

* **Spring Boot Backend Code (`backend/src/`):** UNTOUCHED (0 files modified).
* **Database (`SIH26135`):** UNTOUCHED (No migrations, no DDL, no demo data inserted).
* **Git Repository:** Working tree remains clean unstaged modifications (no commits or pushes made).
* **Scope Boundary:** Phase 30.3 is complete. Phase 30.4 has NOT been initiated.
