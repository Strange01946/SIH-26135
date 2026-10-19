# PHASE 25.8 — Cross-Domain Transaction Integrity

## 1. Project Context

- **Project:** SIH-26135
- **Repository root:** `E:\project`
- **Backend root:** `E:\project\backend`
- **Java base package:** `in.gov.sih.sih26135`
- **Branch:** `main`
- **Database catalog:** `SIH26135`
- **Test database catalog:** `SIH26135_TEST`
- **Schema status:** Frozen — 144 base tables and 21 views
- **Hibernate configuration:** `ddl-auto=validate`
- **Baseline Commit:** `fad1b6c` ("Complete skill gap remediation workflow and recommendation decisions phase 25.7")

## 2. Phase Objective

Perform an evidence-based review of cross-domain workflow transaction integrity and make only justified, narrowly scoped corrections.

The review addresses:
1. Multi-step workflows and their transaction boundaries.
2. Partial database updates when an intermediate operation fails.
3. Whether related records must be persisted atomically, based on actual business semantics.
4. Unnecessarily broad transactions.
5. Circular service dependencies and transaction orchestration risks.

The goal is to ensure that each business operation has the correct atomicity and failure behavior without unnecessarily expanding transactions, introducing architectural changes, or changing existing business rules.

## 3. Mandatory Constraints

1. Inspect actual source before proposing or implementing changes.
2. Treat existing code, entity relationships, service contracts, repository operations, database constraints, tests, and documented business rules as evidence.
3. Do not infer that every multi-step workflow must be one large transaction.
4. Do not infer that every service call must have its own transaction.
5. Do not change business semantics merely to make transaction tests pass.
6. Do not add, remove, or relocate `@Transactional` annotations without explaining the specific correctness problem and intended transaction boundary.
7. Do not swallow database, validation, authorization, or state-transition failures.
8. Do not return success-shaped responses after a required operation fails.
9. Do not introduce broad `catch (Exception)` or `catch (Throwable)` blocks, catch-and-ignore logic, fake results, or silent fallback behavior.
10. Do not weaken authentication, authorization, consent, validation, or audit requirements.
11. Do not disable tests, remove assertions, or weaken existing test expectations to obtain a passing build.
12. Do not introduce new dependencies or frameworks unless a demonstrated requirement cannot be met using existing project capabilities.
13. Do not change the database schema, DDL, migrations, seed/reference data, entity mappings, or database contents.
14. Preserve the existing `SIH26135` production catalog. Do not execute write operations against it.
15. Use only the dedicated `SIH26135_TEST` catalog for any database-backed tests that require database writes. Respect the existing fail-closed database-isolation safeguards.
16. Do not reset, clean, stash, discard, or overwrite pre-existing working-tree changes.
17. Do not commit, push, stage, or perform Git history changes. The user performs the Git checkpoint only after independent review and `FINAL PASS`.
18. Do not ask the user to repeat the project workflow or already established constraints.
19. If a required business rule, credential, external service, or permission is unavailable, do not fabricate it or bypass the requirement. Report the exact blocker.
20. Do not expand this phase into a general refactor or unrelated cleanup.

## 4. Workflows in Scope

The six cross-domain workflow services:
1. `TrainingOutcomeWorkflowService` / `TrainingOutcomeWorkflowServiceImpl`
2. `PlacementEmploymentWorkflowService` / `PlacementEmploymentWorkflowServiceImpl`
3. `EmploymentLifecycleWorkflowService` / `EmploymentLifecycleWorkflowServiceImpl`
4. `VerificationWorkflowService` / `VerificationWorkflowServiceImpl`
5. `SkillGapRemediationWorkflowService` / `SkillGapRemediationWorkflowServiceImpl`
6. `FollowupEngagementWorkflowService` / `FollowupEngagementWorkflowServiceImpl`
