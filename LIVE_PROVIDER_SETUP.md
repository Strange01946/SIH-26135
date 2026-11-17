# Live Multi-Channel Communication Infrastructure Setup Guide (Phase 29.3)
**SIH-26135 Platform — Outbound SMS, WhatsApp & Email Integration**

---

## 1. Executive Summary & Architecture Overview

Phase 29.3 equips the SIH-26135 platform with an authoritative, production-grade outbound communication engine supporting **SMS**, **WhatsApp**, and **Email**. The architecture strictly enforces India's **Digital Personal Data Protection (DPDP) Act 2023** principles:

```
[Candidate Lifecycle Event / Direct Dispatch Command]
                         │
                         ▼
          Workflow Security Scope Guard
                         │
                         ▼
        Unmasked Recipient Resolution (MySQL)
                         │
                         ▼
        Approved Template Registry & Validation
                         │
                         ▼
    Authoritative DPDP Act Consent Enactment (Fail-Closed)
       ├── Missing/Withdrawn Consent ──► Writes BLOCKED_NO_CONSENT Log & Fails (HTTP 403)
       └── Valid Active Consent
                         │
                         ▼
         Provider Abstraction Layer (Delegator)
       ├── SMS: Twilio Programmable Messaging
       ├── WhatsApp: Twilio WhatsApp Messaging API / Sandbox
       └── Email: Twilio SendGrid API / Gmail Authenticated SMTP (JavaMailSender)
                         │
                         ▼
              Real Recipient Device
                         │
                         ▼
  Evidence Capture & Immutable Communication Log (MySQL)
```

By default, automated regression tests run in `MOCK` mode, consuming zero live network calls and zero trial credits. In `LIVE` mode, the backend connects directly to real carrier and provider APIs.

---

## 2. Mandatory Security & Credential Hygiene

> [!CAUTION]
> **IMMEDIATE ACTION REQUIRED — ROTATE TWILIO AUTH TOKEN**
>
> During initial setup, a Twilio Auth Token was exposed in conversation text. Because live tokens must never remain compromised:
> 1. Log in to the [Twilio Console](https://console.twilio.com/).
> 2. Navigate to **Account** -> **API Keys & Tokens**.
> 3. Under **Auth Tokens**, click **Rotate Token** or generate a secondary token and delete the primary.
> 4. Ensure the newly rotated token is stored *only* as an environment variable in your private shell session. Never write it into code, configuration files, or git commits.

### Zero-Git Invariant
The project operates under a frozen Git invariant. Never run `git add`, `git commit`, or `git push` with API keys or tokens. All credentials must be passed via environment variables.

---

## 3. Provider Configuration & Step-by-Step Setup

### A. Twilio SMS (Programmable Messaging)

1. **Obtain Account SID & Auth Token:**
   - In the Twilio Console dashboard, locate your **Account SID** (starts with `AC...`) and your newly rotated **Auth Token**.
2. **Obtain Active Twilio Phone Number:**
   - Navigate to **Phone Numbers** -> **Manage** -> **Active Numbers**.
   - Note down your Twilio trial phone number in E.164 format (e.g., `+18005550199` or `+1205...`).
3. **Verified Caller IDs (Trial Restriction):**
   - Twilio trial accounts can only send SMS messages to **Verified Caller IDs**.
   - Navigate to **Phone Numbers** -> **Manage** -> **Verified Caller IDs** and add the destination test phone number (e.g., your personal mobile number `+91XXXXXXXXXX`).

---

### B. Twilio WhatsApp (Sandbox Setup)

1. **Access WhatsApp Sandbox:**
   - In the Twilio Console, navigate to **Messaging** -> **Try it Out** -> **Send a WhatsApp Message**.
2. **Note Sandbox Number:**
   - The Twilio Sandbox sender number is universally `whatsapp:+14155238886`.
3. **Join the Sandbox from Destination Device:**
   - From your personal WhatsApp mobile app, send a message containing your sandbox code (e.g., `join <unique-two-words>`) to `+1 415 523 8886`.
   - Twilio will reply: *"You are all set! The sandbox is joined."*
   - Once joined, the sandbox session remains active for 72 hours of inactivity.
4. **Sender Format:**
   - The backend automatically ensures `whatsapp:` prefixes are applied to both sender and recipient.

---

### C. Email Integration: Dual-Stack Approach

The backend provides dual-stack support: **SendGrid API** (cloud provider) and **Authenticated Gmail SMTP** (production-tested, zero-cost alternative).

#### Option 1: Twilio SendGrid Email API
1. In Twilio SendGrid Console, navigate to **Settings** -> **API Keys**.
2. Click **Create API Key**, select **Full Access** or **Mail Send Restricted Access**, and save the key (`SG.xxxxxxxxxxxx`).
3. Under **Settings** -> **Sender Authentication**, complete **Single Sender Verification** for the `From` email address (e.g., `noreply@yourdomain.com` or your verified address).

#### Option 2: Gmail Authenticated SMTP (JavaMailSender / Recommended for Testing)
If SendGrid trial requires dedicated domain verification or credit card validation, Gmail SMTP can be configured in 60 seconds:
1. Log in to your Google Account.
2. Go to **Security** -> **2-Step Verification** (ensure it is ON).
3. Scroll to the bottom and select **App passwords** (or search "App Passwords" in Google Account search).
4. Enter name (e.g., `SIH Backend`) and click **Create**. Google displays a 16-character password (e.g., `abcd efgh ijkl mnop`).
5. Use this app password for `SMTP_APP_PASSWORD`. Port `587` with STARTTLS is enabled automatically.

---

## 4. Environment Variables Reference

Configure the following variables in your terminal session before launching the backend:

| Variable Name | Required For | Example Value | Description |
|---|---|---|---|
| `TWILIO_ACCOUNT_SID` | SMS & WhatsApp | `AC7a3e9...` | Twilio Account SID |
| `TWILIO_AUTH_TOKEN` | SMS & WhatsApp | `[ROTATED_TOKEN]` | Rotated Twilio Auth Token |
| `TWILIO_SMS_FROM` | SMS | `+12055550123` | E.164 Twilio phone number |
| `TWILIO_WHATSAPP_FROM` | WhatsApp | `+14155238886` | Twilio WhatsApp sandbox number |
| `SENDGRID_API_KEY` | SendGrid Email | `SG.xxxxxxxx...` | SendGrid v3 API Key |
| `SENDGRID_FROM_EMAIL` | SendGrid Email | `verified@example.com` | Verified SendGrid sender address |
| `SMTP_HOST` | Gmail/Custom SMTP | `smtp.gmail.com` | SMTP Server Host |
| `SMTP_PORT` | Gmail/Custom SMTP | `587` | SMTP Port (587 for STARTTLS) |
| `SMTP_USERNAME` | Gmail/Custom SMTP | `your-email@gmail.com` | SMTP Username |
| `SMTP_APP_PASSWORD` | Gmail/Custom SMTP | `abcdefghijklmnop` | 16-char Google App Password |
| `SMTP_FROM_EMAIL` | Gmail/Custom SMTP | `your-email@gmail.com` | From header email address |
| `SIH_LIVE_TEST_PHONE` | Live Safety Override | `+919876543210` | Test phone number for safety rerouting |
| `SIH_LIVE_TEST_WHATSAPP`| Live Safety Override | `+919876543210` | WhatsApp test phone for safety rerouting |
| `SIH_LIVE_TEST_EMAIL` | Live Safety Override | `test-trainee@gmail.com`| Test email address for safety rerouting |

---

## 5. Launching the Backend in Live Mode

### Windows PowerShell

Run the following commands in PowerShell before starting the application:

```powershell
# 1. Twilio Live Credentials
$env:TWILIO_ACCOUNT_SID = "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
$env:TWILIO_AUTH_TOKEN  = "your_newly_rotated_auth_token"
$env:TWILIO_SMS_FROM    = "+12055550123"
$env:TWILIO_WHATSAPP_FROM = "+14155238886"

# 2. Email Setup (Choose SendGrid or Gmail SMTP)
# Option A: SendGrid
$env:SENDGRID_API_KEY    = "SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
$env:SENDGRID_FROM_EMAIL = "verified-sender@example.com"

# Option B: Gmail SMTP
$env:SMTP_HOST         = "smtp.gmail.com"
$env:SMTP_PORT         = "587"
$env:SMTP_USERNAME     = "your-gmail@gmail.com"
$env:SMTP_APP_PASSWORD = "abcdefghijklmnop"
$env:SMTP_FROM_EMAIL   = "your-gmail@gmail.com"

# 3. Live Safe Recipient Overrides (Recommended: Diverts messages to your test phone)
$env:SIH_LIVE_TEST_PHONE    = "+919876543210"
$env:SIH_LIVE_TEST_WHATSAPP = "+919876543210"
$env:SIH_LIVE_TEST_EMAIL    = "your-test-email@gmail.com"

# 4. Enable Live Mode in Application Launch
mvn spring-boot:run -f backend/pom.xml "-Dspring-boot.run.arguments=--communication.mode=LIVE --communication.live-recipient-override-enabled=true"
```

### Linux / macOS Bash

```bash
export TWILIO_ACCOUNT_SID="ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
export TWILIO_AUTH_TOKEN="your_newly_rotated_auth_token"
export TWILIO_SMS_FROM="+12055550123"
export TWILIO_WHATSAPP_FROM="+14155238886"

# Gmail SMTP
export SMTP_HOST="smtp.gmail.com"
export SMTP_PORT="587"
export SMTP_USERNAME="your-gmail@gmail.com"
export SMTP_APP_PASSWORD="abcdefghijklmnop"
export SMTP_FROM_EMAIL="your-gmail@gmail.com"

# Overrides
export SIH_LIVE_TEST_PHONE="+919876543210"
export SIH_LIVE_TEST_WHATSAPP="+919876543210"
export SIH_LIVE_TEST_EMAIL="your-test-email@gmail.com"

mvn spring-boot:run -f backend/pom.xml -Dspring-boot.run.arguments="--communication.mode=LIVE --communication.live-recipient-override-enabled=true"
```

---

## 6. Real Live Verification via Spring Boot CLI Runner

> [!IMPORTANT]
> **Definitive Delivery Verification Mechanism**
>
> Ordinary Maven unit and integration tests run strictly in `MOCK` mode to ensure zero trial credit consumption and zero accidental network dispatches.
> To prove genuine end-to-end outbound delivery to physical recipient devices, execute the opt-in **`LiveCommunicationSmokeTestCliRunner`** with `--communication.mode=LIVE`.
>
> Requirements:
> 1. Set credentials in environment variables (never hardcoded or committed to git).
> 2. Ensure target database catalog is strictly `SIH26135_TEST`.
> 3. Ensure the target test trainee has legitimate, granted consents in `SIH26135_TEST` (`DATA_PROCESSING` + channel consent). Automatic consent granting has been eliminated.
> 4. Specify the target trainee explicitly via `--communication.target-trainee-id=<id>`. Silent fallback to a default trainee is strictly disallowed.
> 5. The runner strictly fails with an error if `--communication.mode=LIVE` is not supplied, preventing mock tests from masquerading as live delivery evidence.

### 1. SMS Live Smoke Test (Windows PowerShell)

Ensure Twilio environment variables (`$env:TWILIO_ACCOUNT_SID`, `$env:TWILIO_AUTH_TOKEN`, `$env:TWILIO_SMS_FROM`) and safety override (`$env:SIH_LIVE_TEST_PHONE`) are set in your current PowerShell session, then run:

```powershell
mvn spring-boot:run -f backend/pom.xml `
  "-Dspring-boot.run.arguments=--DB_NAME=SIH26135_TEST --DB_USERNAME=root --DB_PASSWORD=$env:SIH_TEST_DB_PASSWORD --communication.live-smoke-test.enabled=true --communication.mode=LIVE --communication.channel=SMS --communication.target-trainee-id=1005143 --communication.live-recipient-override-enabled=true"
```

### 2. WhatsApp Live Smoke Test (Windows PowerShell)

Ensure Twilio WhatsApp environment variables (`$env:TWILIO_ACCOUNT_SID`, `$env:TWILIO_AUTH_TOKEN`, `$env:TWILIO_WHATSAPP_FROM`) and recipient override (`$env:SIH_LIVE_TEST_WHATSAPP`) are set, your handset has joined the sandbox (`join <sandbox-code>` sent to `+1 415 523 8886`), then run:

```powershell
mvn spring-boot:run -f backend/pom.xml `
  "-Dspring-boot.run.arguments=--DB_NAME=SIH26135_TEST --DB_USERNAME=root --DB_PASSWORD=$env:SIH_TEST_DB_PASSWORD --communication.live-smoke-test.enabled=true --communication.mode=LIVE --communication.channel=WHATSAPP --communication.target-trainee-id=1005143 --communication.live-recipient-override-enabled=true"
```

### 3. Email Live Smoke Test (Windows PowerShell)

Ensure SendGrid (`$env:SENDGRID_API_KEY`, `$env:SENDGRID_FROM_EMAIL`) OR Gmail SMTP (`$env:SMTP_HOST=smtp.gmail.com`, `$env:SMTP_PORT=587`, `$env:SMTP_USERNAME`, `$env:SMTP_APP_PASSWORD`, `$env:SMTP_FROM_EMAIL`) and recipient override (`$env:SIH_LIVE_TEST_EMAIL`) are set, then run:

```powershell
mvn spring-boot:run -f backend/pom.xml `
  "-Dspring-boot.run.arguments=--DB_NAME=SIH26135_TEST --DB_USERNAME=root --DB_PASSWORD=$env:SIH_TEST_DB_PASSWORD --communication.live-smoke-test.enabled=true --communication.mode=LIVE --communication.channel=EMAIL --communication.target-trainee-id=1005143 --communication.live-recipient-override-enabled=true"
```

---

## 7. Inspecting MySQL Delivery Evidence in SIH26135_TEST

Execute the following query against the dedicated test catalog `SIH26135_TEST` to inspect the persisted delivery evidence:

```sql
USE SIH26135_TEST;

SELECT 
    l.communication_log_id,
    c.channel_name,
    p.purpose_name,
    s.status_code,
    l.provider_name,
    l.provider_message_id,
    l.sent_at,
    l.failed_at,
    l.failure_reason
FROM communication_logs l
JOIN ref_communication_channel c ON l.communication_channel_id = c.communication_channel_id
JOIN ref_communication_purpose p ON l.communication_purpose_id = p.communication_purpose_id
JOIN ref_communication_status s ON l.communication_status_id = s.communication_status_id
ORDER BY l.communication_log_id DESC
LIMIT 5;
```

### Expected Output:
- `channel_name`: `SMS` / `WHATSAPP` / `EMAIL`
- `status_code`: `SENT` (or `QUEUED` / `ATTEMPTED` / `DELIVERED` depending on carrier response; `FAILED` if rejected)
- `provider_name`: `TwilioSmsProvider` / `TwilioWhatsAppProvider` / `SmtpEmailProvider` / `SendGridEmailProvider`
- `provider_message_id`: Starts with `SM...` (Twilio SMS), `WA...` (Twilio WhatsApp), or `SMTP_...` / `SG_...`
- `sent_at`: Timestamp matching the dispatch instant

### Handset Verification Evidence
- **SMS**: Physical mobile handset receives the SMS from the Twilio number.
- **WhatsApp**: Handset receives the WhatsApp message from `Twilio Sandbox`.
- **Email**: Destination inbox receives the dispatch confirmation email.
