# Phase 30.11 Communication Center

Open **Communication Center** at `/communication` in the local frontend. Assessors with `assessment.manage` can render the five standard templates with sample values. Nationwide administrators with `system.manage`, `communication.manage`, or `survey.manage` can also review delivery logs and the dispatch queue.

| View | API under `/api/v1/communication` | Behavior |
| --- | --- | --- |
| Template manager | `GET /templates`, `POST /templates/{code}/render` | Uses the registered parameter keys, renderer, and URL validation. The preview displays the backend’s plain text. |
| Delivery logs | `GET /logs` | Filters outbound records by channel, status, log ID, template code, or purpose. The API returns masked hints from the contact details on file. A stored consent withdrawal appears as **Opted out** over the backend’s `BLOCKED_NO_CONSENT` status. |
| Dispatch queue | `GET /jobs` | Shows counts by durable job state, attempt counts, run mode, and links to associated logs. Refreshes every five seconds while open. |
| Receipt demonstration | `GET /center`, `POST /demo/receipts`, `POST /demo/receipts/{id}/deliver` | Creates a **Demo receipt** log in `SENT`. A server-signed loopback request to `POST /webhooks/mock` reconciles the same row to `DELIVERED` and records the delivery timestamp. |
| Consent demonstration | `POST /demo/consent` | Uses a sample trainee with recorded withdrawal. The normal dispatcher persists `BLOCKED_NO_CONSENT` and returns `CONSENT_WITHDRAWN` before contacting a provider. |

The durable queue worker is enabled by default. It only claims jobs created for its configured delivery mode. Set `COMMUNICATION_QUEUE_WORKER_ENABLED=false` to pause scheduled processing while leaving the queue view available.

## Enable the local demonstrations

From the backend directory, use the provisioned `SIH26135_TEST` database and its credentials.

```powershell
$env:DB_NAME = 'SIH26135_TEST'
$env:DB_USERNAME = $env:SIH_TEST_DB_USERNAME
$env:DB_PASSWORD = $env:SIH_TEST_DB_PASSWORD
mvn spring-boot:run '-Dspring-boot.run.profiles=local-demo' '-Dspring-boot.run.arguments=--server.address=127.0.0.1 --communication.mode=MOCK --communication.allow-test-webhook-signatures=true'
```

Run `npm run dev` in the frontend directory and open the site on port 5173.

On **Delivery logs**, select **Create sent sample** and check that the row shows **Sent**. Select **Simulate delivery receipt** and confirm **Delivered** and the delivery timestamp on that row. Select **Test withdrawn consent** to open the warning dialog. The receipt and consent actions use the dedicated `LOCAL-DEMO-COMM-CENTER` trainee in the test database.
