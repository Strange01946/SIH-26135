# Phase 26.2 — Validation Framework Foundation

## Objective

Implement the foundational Bean Validation infrastructure for the SIH-26135 Spring Boot backend.

Phase 26.1 completed an API validation, pagination, and exception-handling audit. This phase must use that audit as its baseline and establish a reliable validation framework without prematurely applying validation rules across all request DTOs.

**Primary goals:**
- Make Bean Validation an explicit, correctly configured backend dependency.
- Establish a consistent mechanism for activating request validation at the controller boundary.
- Ensure validation failures are handled centrally and return a consistent, safe API error response.
- Preserve existing API contracts and business behavior wherever possible.
- Prepare the codebase for the DTO-specific validation rollout planned for Phase 26.5.

This is an implementation phase, not another broad audit. Do not spend the phase repeating Phase 26.1 investigations unless a specific finding needs verification.

---

## 1. Mandatory Initial Reconnaissance

Before modifying anything, inspect the existing repository and understand the current implementation.

Repository:
- Root: `E:\project`
- Backend: `E:\project\backend`
- Branch: `main`

Inspect at minimum:

1. `backend/pom.xml`
2. Existing Spring Boot version and Java version.
3. Current Bean Validation dependencies and their resolved versions.
4. Existing controller structure and request-handling patterns.
5. Existing `GlobalExceptionHandler.java`.
6. Existing `ApiResponse<T>` and `ErrorResponse` definitions.
7. Current exception-handling conventions and response formats.
8. Existing request DTOs and their package structure.
9. Existing validation annotations, if any.
10. Existing tests for controller validation and exception handling.
11. Current test configuration and database targeting.
12. Existing application configuration, security filters, and request-processing behavior.

Use the Phase 26.1 audit report:

`phase-26-1-api-validation-pagination-audit-report.md`

Read the report from the repository root. If it is unavailable, report that fact and use the Phase 26.1 baseline below as the available reference. Do not invent report contents.

### Phase 26.1 baseline

The audit reported:

- 54 REST controllers; none had class-level `@Validated`.
- 613 REST endpoints; none used `@Valid`.
- 106 POST/PUT request-body endpoints lacked `@Valid`.
- 116 request DTOs existed under `dto.request`; only one validation annotation was found.
- `LoginRequest` and `RefreshTokenRequest` had no validation annotations.
- Hibernate Validator 8.0.3.Final and Jakarta Validation API 3.0.2 were available transitively.
- `spring-boot-starter-validation` was not an explicit first-class dependency.
- `GlobalExceptionHandler.java` was 1114 lines and handled 14 exception types.
- Existing error handling included anti-echo/sensitive-data sanitation and fail-closed allowlists.
- `ApiResponse<T>` and `ErrorResponse` did not include a correlation ID.
- The reported full test run completed with 645 tests, zero failures/errors/skips, and BUILD SUCCESS.

These are audit findings, not independently verified current-state guarantees. Verify relevant implementation details before relying on them.

Do not reopen unrelated pagination, correlation-ID, or DTO-rule work in this phase.

---

## 2. Scope of Phase 26.2

### 2.1 Explicit Validation Dependency

Review the existing Maven dependency tree and Spring Boot dependency management.

- Add `spring-boot-starter-validation` as an explicit dependency if it is not already present.
- Use the dependency version managed by the existing Spring Boot parent/BOM wherever applicable.
- Avoid unnecessary direct version overrides.
- Avoid duplicate or conflicting Jakarta Validation and Hibernate Validator dependencies.
- Confirm the application uses the correct Jakarta namespace for its current Spring Boot generation.
- Do not upgrade Spring Boot, Java, Hibernate Validator, or unrelated dependencies as part of this phase.

The dependency must be explicit and maintainable, not merely available transitively.

### 2.2 Controller-Boundary Validation Foundation

Establish the standard mechanism through which request DTO validation will be activated.

- Confirm the correct Spring MVC validation mechanism for the project's current Spring Boot and Spring Framework versions.
- Establish and document the intended controller request-body validation convention.
- Confirm that invalid request-body input can be rejected before the relevant controller business logic executes.
- Ensure validation activation does not unintentionally change authentication, authorization, or security-filter behavior.
- Preserve existing endpoint signatures and response contracts unless a change is necessary and explicitly justified.

**Important scope boundary:** Phase 26.2 establishes the framework. Do not mechanically add validation annotations to every DTO or every endpoint. The comprehensive DTO-specific validation rollout belongs to Phase 26.5.

Do not add class-level `@Validated` indiscriminately. Verify whether it is necessary for the chosen validation approach and whether it changes exception behavior or method-validation semantics.

### 2.3 Centralized Validation-Error Handling

Inspect the current `GlobalExceptionHandler` and integrate validation failures into its existing conventions.

Handle the relevant validation exception types generated by the chosen request-validation approach. Depending on the project's Spring version and implementation, these may include:

- `MethodArgumentNotValidException`
- `HandlerMethodValidationException`
- `ConstraintViolationException`, only where applicable to the framework behavior being enabled

Do not add exception handlers blindly. Confirm which exceptions can actually arise from the project's request-processing and validation paths.

The resulting behavior must:

- Return the existing API error response format.
- Use the project's established HTTP status and error-code conventions.
- Provide a useful, stable validation error message.
- Avoid exposing stack traces, internal class names, SQL, implementation details, or sensitive request values.
- Avoid echoing passwords, tokens, secrets, or full request bodies.
- Preserve existing exception-handling behavior for unrelated errors.
- Avoid duplicate or conflicting exception-handler mappings.
- Remain compatible with existing frontend/API consumers.

If multiple validation violations occur, provide a predictable and deterministic response. Do not expose an uncontrolled collection of raw validator messages.

Do not introduce a new error-response contract solely for this phase. If the current contract cannot represent validation errors safely, explain the limitation and propose the smallest compatible change before implementing it.

### 2.4 Framework-Level Test Coverage

Add focused tests that demonstrate the validation infrastructure works.

Tests must cover the actual framework behavior, not merely test a manually invoked validator.

At minimum, verify:

1. The explicit validation dependency is present and the application context can initialize.
2. A request DTO with a test-only constraint can be rejected at the controller boundary.
3. Invalid input is rejected before the controller's business logic is executed.
4. The validation exception is mapped to the expected API error response.
5. The response does not expose sensitive values or internal exception details.
6. Multiple violations produce deterministic output.
7. Existing non-validation exception handling remains intact.
8. Existing authentication/authorization behavior is not unintentionally bypassed or altered.

Use a dedicated test fixture/controller or an existing appropriate test endpoint where possible. Do not introduce production DTO validation rules merely to make these tests pass.

Test-only fixtures and mocks are allowed when isolated from production application behavior.

---

## 3. Mandatory Production-Grade Constraints

These rules apply to every implementation decision in this phase.

### 3.1 No Fake or Demo Domain Data

- Do not add demo, seed, or fabricated business data to the application.
- Do not create fake users, records, or domain entities to make validation appear functional.
- Test fixtures and mocks are permitted only in isolated tests.
- Do not introduce fallback application data.

### 3.2 No Fabricated Credentials or External Integrations

- Inspect configuration before making assumptions about credentials or integrations.
- Do not fabricate credentials, provider responses, or integration success.
- If an external service requires credentials, account access, or permissions that are unavailable, stop only the dependent work and report the exact blocker.
- Never request secrets to be pasted into source code or committed.
- Do not represent a mock or local substitute as a live integration.

This phase should not require a new external integration. Do not introduce one.

### 3.3 Security and API Integrity

- Preserve authentication and authorization controls.
- Preserve tenant isolation, ownership checks, and existing business invariants.
- Do not bypass security filters or weaken authorization to make tests pass.
- Do not expose sensitive values through validation messages.
- Do not introduce insecure defaults.
- Preserve existing API response conventions and compatibility.
- Do not silently alter business logic.

### 3.4 Database and Schema Protection

The production database is strictly off-limits.

- Never connect to, query, or write to production database `SIH26135`.
- Do not use production data for testing.
- Do not change the frozen database schema.
- Preserve `spring.jpa.hibernate.ddl-auto=validate`.
- Do not introduce schema changes, migrations, or DDL.
- If a test requires a database, verify that its configuration targets only `SIH26135_TEST` before running it.
- If the target cannot be verified, do not run the database-backed test.

### 3.5 Git Safety

- Do not stage, commit, push, reset, stash, or clean.
- Do not discard or overwrite pre-existing modifications.
- Do not modify unrelated files.
- Preserve the existing branch and working-tree state.
- Report all changed files and any pre-existing changes encountered.

### 3.6 Evidence and Truthfulness

- Do not claim a test passed unless it was actually executed and passed.
- Distinguish unit tests, integration tests, and any live verification.
- Do not describe a mocked test as live integration verification.
- Report any skipped tests, environment limitations, or unverified behavior explicitly.

---

## 4. Implementation Sequence

Follow this order:

1. **Reconnaissance:** inspect dependencies, controllers, error handling, configuration, tests, and current working-tree state.
2. **Baseline confirmation:** identify the exact validation dependency and exception behavior currently present.
3. **Dependency implementation:** add the explicit validation starter if needed.
4. **Validation mechanism:** establish the appropriate controller-boundary validation convention.
5. **Exception integration:** add safe, centralized validation-error handling without disrupting existing mappings.
6. **Focused tests:** create tests for the framework behavior and error response.
7. **Regression tests:** run the relevant existing tests and then the full backend test suite if safe and feasible.
8. **Review:** inspect the final diff for unrelated changes, security regressions, and accidental contract changes.
9. **Report:** provide evidence and a clear verdict.

Do not begin broad DTO annotation work in Phase 26.2.

---

## 5. Testing and Verification

Use the repository's actual Maven configuration and test conventions.

At minimum:

- Verify the dependency tree for validation dependencies.
- Compile the backend.
- Run the focused validation tests.
- Run relevant existing exception-handler/controller tests.
- Run the full backend test suite, if feasible.
- Confirm database-backed tests target only `SIH26135_TEST`.
- Confirm the production database was not accessed.
- Run `git diff --check`.
- Review `git status --short`.
- Review the complete diff.

Expected full-suite command, subject to repository structure:

`mvn test -f backend/pom.xml`

Do not report BUILD SUCCESS if the command failed, was interrupted, or was not executed.

If a test fails, investigate the failure and distinguish a phase-introduced regression from a pre-existing issue. Do not change unrelated behavior merely to obtain a green test run.

---

## 6. Acceptance Criteria

Phase 26.2 is complete only when:

- [ ] Bean Validation is an explicit and correctly managed dependency.
- [ ] The application compiles with the existing Java and Spring Boot versions.
- [ ] The controller-boundary validation mechanism is established and documented.
- [ ] Framework-level tests demonstrate invalid request rejection before business logic.
- [ ] Validation failures use the existing API error response conventions.
- [ ] Validation responses are safe and do not leak sensitive data.
- [ ] Existing exception mappings and security behavior remain intact.
- [ ] No broad DTO annotation rollout was performed.
- [ ] No production database was accessed.
- [ ] No schema changes or migrations were introduced.
- [ ] No unrelated changes were made.
- [ ] No Git staging or commit actions were performed.
- [ ] Relevant tests and `git diff --check` have been executed and their outcomes recorded.
- [ ] A detailed phase report is created.

If any acceptance criterion cannot be satisfied, explain why and identify the remaining blocker. Do not mark the phase complete merely because the application compiles.

---

## 7. Required Phase Report

Create the report at the repository root:

`phase-26-2-validation-framework-foundation-report.md`

Include:

1. Phase objective and final verdict.
2. Initial working-tree state, including pre-existing modified/untracked files.
3. Reconnaissance findings.
4. Phase 26.1 findings confirmed or contradicted by current source.
5. Files changed and the reason for each change.
6. Dependency changes and resolved versions.
7. Validation mechanism selected and why it fits the current framework.
8. Validation exception types handled and response behavior.
9. Security and compatibility considerations.
10. Tests executed, exact commands, and results.
11. Database safety confirmation and test database target, where applicable.
12. `git diff --check` result.
13. Remaining limitations and deferred work.
14. Explicit confirmation that no Git stage/commit/push/reset/stash/clean action was performed.

Do not claim that Phase 26.5 DTO validation, pagination, correlation IDs, or unrelated audit items are complete.

---

## 8. Final Response and Stop Conditions

At completion, provide:

- A concise implementation summary.
- The report path.
- Changed files.
- Test evidence and any limitations.
- A clear final verdict.

Use one of these verdicts:

- `READY FOR INDEPENDENT REVIEW` — acceptance criteria are satisfied and evidence supports the result.
- `BLOCKED` — a required dependency, configuration, or access is unavailable.
- `NOT READY` — implementation or tests reveal unresolved issues.

Stop after reporting. Do not proceed to Phase 26.3 or any later phase without a new instruction.
