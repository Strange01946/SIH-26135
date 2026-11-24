# Phase 30.7 Employer portal

Employers can publish vacancies with the skills they need, record when assigned candidates start work, and respond to employment verification requests. The verification decision updates the relevant employment record and gives later outcome reporting a traceable source.

## Backend contracts

The employer APIs sit under `/api/v1/employer`. The session determines the employer. Each list uses zero-based pagination and each case action checks ownership on the server.

| Screen | Methods and paths | Behavior |
| --- | --- | --- |
| Vacancy catalog | `GET /reference-data`, `GET /skills?search=`, `GET /job-postings`, `POST /job-postings` | Job role, qualification, district, pay frequency, and skill level come from backend reference data. Publishing records an open job posting and its required skills in one transaction. |
| Candidate placements | `GET /placements`, `POST /placements/{placementId}/confirm` | The form records job role, actual joining date, offered pay, and pay frequency. The existing placement workflow creates the employment spell and starting-wage observation at month zero. |
| Verification inbox and history | `GET /verifications/pending`, `GET /verifications` | Pending requests and recorded decisions come from existing employment verification records. A joining confirmation can be followed by a separate verification request through the existing workflow. |
| Verification decision | `POST /verifications/{requestId}/verdict` | A multipart request carries a JSON `verdict` part and an optional `evidence` file. Attestation records `VERIFIED`. A dispute requires a backend rejection reason and records `REJECTED`. An exit records the confirmed past employment and closes the employment spell using the reported separation date, type, and reason. Each accepted decision completes the request. |
| Evidence download | `GET /verifications/{requestId}/evidence/{evidenceId}` | The backend checks request ownership and serves the stored document as an authenticated attachment. Evidence accepts PDF or JPEG files up to 5 MB. The existing evidence record stores the document reference and SHA-256 checksum. |

An employer account needs an active employer profile and the placement, employment, or verification permissions for the action. The portal reuses the existing placement-to-employment, verification, and exit services. Retention metrics still use the employment dates and eligibility rules in the reporting layer.

Evidence is stored in `~/.sih26135/verification-evidence` by default. Set `SIH_VERIFICATION_EVIDENCE_ROOT` on the backend to choose a different private directory. For a local demonstration, use synthetic salary documents and an approved employer account with assigned placements and open verification requests.

## Verification

- The frontend production build passed with the existing bundle-size warning. All 300 frontend tests and 41 targeted backend tests passed.
- Chromium ran the portal through Vite and the live Spring Boot backend using `SIH26135_TEST`. The test published a vacancy and verified its saved skill tags, confirmed joining and starting pay, uploaded and downloaded PDF evidence, disputed a claim with JPEG evidence, and recorded an exit.
- The live run checked invalid file rejection, duplicate submissions, and denied access across employers. Database checks confirmed `VERIFIED` and `REJECTED` verdicts, the closed employment spell, and zero later wage observations or six- and twelve-month retention flags for recent placements. The disposable records and documents were removed after testing.
