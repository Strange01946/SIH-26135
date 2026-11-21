# SIH-26135 Local Build, Test & Git Hygiene Runbook

**Document**: Local Developer Operational Runbook  
**Target Environment**: Windows 11 / Host Workstation (Local-PC Execution)  
**Applicability**: Backend Developers, Testers, Evaluators  
**Last Verified**: November 2026 (Phase 29.14)

---

## 1. Prerequisites & Environment Verification

Before building or launching the SIH-26135 backend, verify that host toolchain components meet baseline specifications.

### 1.1 Verify Java and Maven Versions

Open PowerShell and verify:

```powershell
# Verify Java version (Requires OpenJDK 17 LTS)
java -version
# Expected: openjdk version "17.0.x" (e.g., Eclipse Adoptium Temurin-17.0.19)

# Verify Maven version (Requires Maven 3.8+)
mvn -version
# Expected: Apache Maven 3.9.x
```

> [!IMPORTANT]
> The project build targets `<java.version>17</java.version>`. Using Java 21+ or Java 11 may cause bytecode incompatibility or annotation processing failures during compilation.

### 1.2 Verify MySQL 8.0 Local Service

The backend depends on a local MySQL 8.0 Community Server running on `localhost:3306`.

```powershell
# Check Windows MySQL Service status
Get-Service -Name "MySQL80"

# If stopped, start the service using Administrator PowerShell:
Start-Service -Name "MySQL80"
```

Verify client executables:
- Default installation directory: `C:\Program Files\MySQL\MySQL Server 8.0\bin`
- Verify binary versions:
  ```powershell
  & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --version
  # Expected: Ver 8.0.46 for Win64 on x86_64
  ```

---

## 2. Environment Variables & Profile Selection

The application uses standard 12-Factor externalized configuration. Sensitive credentials and environment overrides must be supplied via environment variables.

### 2.1 Essential Environment Variables

Set environment variables in your current PowerShell session:

```powershell
# Database connection parameters (Target local MySQL instance)
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "SIH26135"
$env:DB_USERNAME = "root"
# Set database password securely from prompt or session (never commit credentials):
$env:DB_PASSWORD = Read-Host "Enter DB password" # Prompt interactively without hardcoding in scripts

# Test Database connection parameters (for automated test battery)
$env:SIH_TEST_DB_HOST = "localhost"
$env:SIH_TEST_DB_PORT = "3306"
$env:SIH_TEST_DB_NAME = "SIH26135_TEST"
$env:SIH_TEST_DB_USERNAME = "root"
$env:SIH_TEST_DB_PASSWORD = $env:DB_PASSWORD

# JWT Secret Key (HMAC-SHA256: Must be >= 32 bytes / 256 bits). Prompt interactively or load from secure environment:
$env:JWT_SECRET = Read-Host "Enter JWT Secret (minimum 32 characters / 256 bits)"
# Or generate a random 256-bit secret for local testing:
# $env:JWT_SECRET = [System.Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))

# Optional: Enable local demo documentation (Swagger UI & OpenAPI)
$env:SWAGGER_UI_ENABLED = "true"
$env:API_DOCS_ENABLED = "true"
```

### 2.2 Profile Architecture

The repository provides two primary profiles:

| Profile | Command Argument | Purpose & Behavior |
|:---|:---|:---|
| **Default Profile** | *(None)* | Production-hardened baseline. Swagger UI and raw OpenAPI are **disabled** (`HTTP 404`). Strict CORS enabled. |
| **Local Demo Profile** | `--spring.profiles.active=local-demo` | Enables interactive Swagger UI (`/swagger-ui.html`) and raw OpenAPI 3.1 documentation (`/v3/api-docs`). |

---

## 3. Compilation & Build Procedures

### 3.1 Clean Compilation

Compile the Spring Boot backend source code without executing automated tests:

```powershell
mvn -f backend/pom.xml clean compile
```

Expected result: `BUILD SUCCESS` with compiled `.class` files generated in `backend/target/classes`.

### 3.2 Packaging Executable Fat JAR

Package the self-contained executable JAR:

```powershell
mvn -f backend/pom.xml clean package -DskipTests
```

Output artifact:
- `backend/target/sih26135-backend-0.1.0-SNAPSHOT.jar` (~70 MB fat JAR including all bundled dependencies).

---

## 4. Automated Testing Procedures & Database Safety

### 4.1 Strict Database Safety & Isolation Rules

> [!CAUTION]
> **NEVER RUN AUTOMATED TESTS AGAINST THE APPLICATION DATABASE (`SIH26135`).**
> Automated tests must exclusively target the dedicated test catalog `SIH26135_TEST`.

- **Test Configuration**: [backend/src/test/resources/application.properties](file:///e:/project/backend/src/test/resources/application.properties#L8-L10) hardcodes `SIH26135_TEST` as the default target.
- **Fail-Closed Safeguard**: The test configuration uses `spring.jpa.hibernate.ddl-auto=validate`. If tables do not match the expected schema, the test context fails immediately without modifying database state.
- **Dedicated Test Secret**: Automated tests use `SIH_TEST_JWT_SECRET` and never interact with production secret material.
- **Mock Communication Provider**: `communication.mode=MOCK` is enforced in tests, ensuring zero external network egress or SMS/email credit consumption.

### 4.2 Running Fast Unit Tests

Execute unit tests that do not require database connectivity:

```powershell
mvn -f backend/pom.xml test "-Dtest=*UnitTest,*Test" "-Dtest=!*IntegrationTest,!*DaoTest,!*RepositoryTest"
```

### 4.3 Running Database-Backed Integration Tests

Ensure `SIH26135_TEST` is accessible, then execute focused integration tests:

```powershell
# Set test database password (from session or prompt)
$env:SIH_TEST_DB_PASSWORD = Read-Host "Enter test DB password"

# Run targeted Actuator, Security, and Metrics integration tests
mvn -f backend/pom.xml test "-Dtest=ActuatorSecurityAndHealthTest,CustomObservabilityMetricsTest,WorkflowAopIntegrationTest,StructuredLoggingTest,DocumentationAccessControlTest"
```

### 4.4 Inspecting Test Reports

Maven Surefire stores test execution reports in:
- Directory: `backend/target/surefire-reports/`
- Text summaries: `*.txt`
- XML test results: `TEST-*.xml`

Inspect failures quickly in PowerShell:
```powershell
Get-ChildItem -Path "backend\target\surefire-reports\*.txt" | Select-String -Pattern "FAILURE|ERROR"
```

---

## 5. Local Application Startup & Verification

### 5.1 Starting Spring Boot

You can start the backend using either Maven or the executable JAR.

#### Method A: Maven Plugin (Recommended for Development)
```powershell
# Start with the local-demo profile (enables Swagger UI)
mvn -f backend/pom.xml spring-boot:run "-Dspring-boot.run.profiles=local-demo"
```

#### Method B: Executable Fat JAR
```powershell
# Launch JAR with active local-demo profile
java -jar backend/target/sih26135-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=local-demo
```

The application starts on port `8080`. Look for the console banner:
```
Started Sih26135BackendApplication in X.XXX seconds
```

### 5.2 Checking Actuator Health

In a separate terminal, test high-level health probes:

```powershell
# Public Liveness Probe
curl -i http://localhost:8080/actuator/health/liveness
# Expected: HTTP 200 OK {"status":"UP"}

# Public Readiness Probe
curl -i http://localhost:8080/actuator/health/readiness
# Expected: HTTP 200 OK {"status":"UP"}
```

### 5.3 Accessing Interactive Swagger UI

Open a web browser and navigate to:
```
http://localhost:8080/swagger-ui.html
```
Or view the raw OpenAPI 3.1 specification:
```
http://localhost:8080/v3/api-docs
```

---

## 6. Local Git Hygiene & File Inspection

To ensure no unintended commits, conflict markers, or whitespace defects enter version control:

### 6.1 Check Whitespace & Conflict Markers
```powershell
git diff --check
```
If no output is returned, working files comply with Git hygiene standards.

### 6.2 Inspect Changed Files Without Making Commits
```powershell
# Show summary of all modified, untracked, and deleted files
git status --short

# View exact line-by-line diffs without committing
git diff

# View staged changes
git diff --staged
```

> [!IMPORTANT]
> Never run automated `git commit` or `git push` scripts. All file changes must be reviewed manually.

---

## 7. Dependency Inspection & Scanning

The project relies on Maven's native dependency tooling to maintain a lightweight build process without heavy third-party CVE scanner daemons.

### 7.1 Inspect Dependency Hierarchy
```powershell
mvn -f backend/pom.xml dependency:tree
```

### 7.2 Inspect Unused or Undeclared Dependencies
```powershell
mvn -f backend/pom.xml dependency:analyze-only
```

*Note: Heavy vulnerability scanners (such as OWASP Dependency-Check or Snyk) are not bundled in `pom.xml` to prevent build slowdowns on local workstations.*

---

## 8. Common Startup Failures & Quick Troubleshooting

| Symptom / Error | Root Cause | Immediate Remediation |
|:---|:---|:---|
| `Port 8080 was already in use` | Another process is holding port 8080. | Find PID: `netstat -ano \| findstr :8080`<br>Kill PID: `Stop-Process -Id <PID> -Force` |
| `Communications link failure` | MySQL service is not running or port 3306 blocked. | Run `Start-Service -Name MySQL80` and verify connection with `mysqladmin ping`. |
| `Access denied for user 'root'@'localhost'` | Incorrect `$env:DB_PASSWORD`. | Update `$env:DB_PASSWORD` in PowerShell session. |
| `JWT secret is not configured` / `insufficiently secure` | `$env:JWT_SECRET` is unset or < 32 bytes (256 bits). | Set `$env:JWT_SECRET` with a 32+ character random string. |
| `Schema-validation: missing table` | `spring.jpa.hibernate.ddl-auto=validate` failed. Target database lacks schema tables. | Initialize schema using SQL scripts in `database/` against `SIH26135` or `SIH26135_TEST`. |
