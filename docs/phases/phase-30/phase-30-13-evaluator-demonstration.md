# Phase 30.13 Local evaluator demonstration

Start the frontend from the `frontend` directory.

```powershell
$env:VITE_LOCAL_DEMO = 'true'
npm run dev -- --host 127.0.0.1
```

Open `http://127.0.0.1:5173/evaluator`. **Evaluator tools** opens the drawer from any page. The controls are enabled on loopback only. In live mode, start Spring Boot using the local project configuration and sign in with authorized demo accounts. Existing roles switch directly, and a different live account uses its assigned credentials.

## Five-minute walkthrough

| Time | Chapter | Portal views |
| --- | --- | --- |
| 1 min | Candidate registration and DPDP consent | Self-registration → trainee consent preferences |
| 1 min | Bulk multipart CSV and quarantine | Provider bulk intake → record quarantine |
| 1 min | Assessment, certification, and placement | Assessor workbench → trainee certificates → employer placements |
| 1 min | Multi-channel employer verification | Employer pending requests → Communication Center delivery logs |
| 1 min | Executive KPIs and geographic heatmap | Government MIS → Maharashtra → district outcomes |

Choose **Start 5-minute tour**. **Next stop** opens each view and switches to the appropriate sample persona in simulation. **Explore this page** pauses the highlight so you can operate the portal. **Resume guide** returns to the same stop.

## Rehearse with the backend offline

Choose **Enable offline simulation** and confirm the switch. The frontend waits for the service worker before opening the app. The simulation banner stays visible, and the guided popovers identify the mode. Keep Vite running to serve the local frontend.

Use **Download sample CSV** for the multipart intake. Process it to see an invalid district and a duplicate registration. Review the sample assessment, certificate, placement, and employer verification. Submit an attestation in the employer portal and inspect the three sample notification channels. The geographic drilldown includes a district with suppressed values.

**Load edge-case fixtures** sets up SMS consent withdrawal and a suppressed KPI. **Reset journey fixtures** restores the sample CSV batch, pending verification, and other synthetic records. Both actions require confirmation and apply to the current simulated tab. **Return to live mode** reconnects the frontend to its normal backend configuration.

The simulator implements the endpoints used by these chapters. Other workflows return a clear simulation-unavailable response. Sample changes live in browser memory and start from the baseline after a page reload.

## Accessibility

The drawer and tour support keyboard navigation, visible focus, and labeled controls. The header offers English and Hindi and marks the translated region with its language. Portal content retains its existing language. The accessibility review covers the evaluator controls and header changes. [GIGW accessibility guidelines](https://guidelines.india.gov.in/accessibility-guidelines-and-attributes/) · [WCAG 2.1](https://www.w3.org/TR/WCAG21/)
