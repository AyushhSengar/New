# CrowdFund (HTML + CSS + JS + Firebase + Java Spring Boot)

```
crowdfunding/
├── frontend/                 -> goes into your GitHub Pages repo
│   ├── index.html
│   ├── style.css
│   ├── script.js
│   ├── config.js             <- set API_BASE_URL here
│   └── firestore.rules       <- paste into Firebase console (LAST step)
└── backend/                  -> Spring Boot API (deploy separately)
    ├── pom.xml
    ├── Dockerfile
    └── src/main/...          controller / service / security / dto / exception / util / config
```

## Who does what
| Task | Owner |
|---|---|
| Sign-in / sign-up, ID tokens | Firebase Authentication (browser) |
| Live lists (campaigns, users, contributions) | Firestore listeners in the browser, **read-only**, scoped by role |
| Register profile, create campaign, approve/reject, change role, contribute | **Java API** (verifies token and role, then writes with Admin SDK) |
| Final guard | `firestore.rules`: clients cannot write at all |

Contributions are **simulated**: a record is stored, no real money moves.

## 1. Service-account key (needed by Java)
1. Firebase console -> Project settings -> Service accounts -> **Generate new private key**.
2. Save the file OUTSIDE the project folder (for example `C:\keys\crowdfund-key.json`).
3. **Never commit it or paste it in the frontend.** `.gitignore` already blocks common names.

## 2. Run the backend locally (Java 17+, Maven)
```bash
cd backend
# Windows (PowerShell)
$env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\keys\crowdfund-key.json"
mvn spring-boot:run

# Mac/Linux
export FIREBASE_SERVICE_ACCOUNT_PATH=/path/to/crowdfund-key.json
mvn spring-boot:run
```
Test: open http://localhost:8080/api/health -> `{"status":"ok"}`.

## 3. Run the frontend locally
Serve `frontend/` over HTTP (ES modules do not work from `file://`), for example VS Code **Live Server** (port 5500) or `python -m http.server 5500`.
`config.js` already points to `http://localhost:8080`. Allowed origins are set in `backend/src/main/resources/application.properties` (or env var `CORS_ALLOWED_ORIGINS`).

## 4. Deploy order (important)
1. Deploy the backend (Render / Railway / Cloud Run, using the `Dockerfile`). Set env vars:
   - `FIREBASE_SERVICE_ACCOUNT_JSON` = the full contents of the key file
   - `CORS_ALLOWED_ORIGINS` = `https://ayushhsengar.github.io`
2. Put the backend HTTPS URL in `frontend/config.js` -> `API_BASE_URL`, push the frontend to GitHub Pages.
3. Test every flow below against the deployed backend.
4. **Only then** publish `firestore.rules` (Firebase console -> Firestore -> Rules). Publishing earlier would block the old browser-side writes.

## API (all need `Authorization: Bearer <Firebase ID token>` except health)
| Method | Path | Role | Body | Success |
|---|---|---|---|---|
| GET | `/api/health` | none | | `{status:"ok"}` |
| GET | `/api/me` | any signed-in | | `{uid,email,name,role,hasProfile}` |
| POST | `/api/register` | signed-in, no profile yet | `{name, role: CREATOR\|CONTRIBUTOR}` | `{uid, role}` |
| POST | `/api/campaigns` | CREATOR | `{title, description, targetAmount}` | 201 `{id, status:"PENDING"}` |
| POST | `/api/campaigns/{id}/approve` | ADMIN | | `{id, status:"APPROVED"}` |
| POST | `/api/campaigns/{id}/reject` | ADMIN | `{reason}` (required) | `{id, status:"REJECTED"}` |
| POST | `/api/campaigns/{id}/contributions` | CONTRIBUTOR | `{amount}` | `{donationId, collectedAmount, status}` |
| PATCH | `/api/users/{uid}/role` | ADMIN | `{role}` | `{uid, role}` |

Errors are always `{"error": "message"}` with status 400 (bad input), 401 (missing/invalid token), 403 (wrong role), 404 (not found), 409 (conflict, e.g. campaign not pending / goal reached), 503 (database unavailable).

## Data model (unchanged, existing data stays valid)
- `users/{uid}`: `name, email, ROLE, createdAt, updatedAt`
- `campaigns/{id}`: `title, description, targetAmount, collectedAmount, creatorId, creatorName, status, rejectionReason, createdAt, updatedAt`
- `donations/{id}`: `contributorId, contributorEmail, contributorName, campaignId, campaignTitle, amount, simulated, createdAt`

## Test checklist
1. Register a Creator and a Contributor; login/logout, then login again and logout again.
2. Creator: create a campaign (empty fields rejected, double click does not duplicate); it shows PENDING in My Campaigns.
3. Admin: Pending campaign appears first; reject without a reason is blocked; approve works; a second approve is refused.
4. Contributor: approved campaign appears live without refresh; contribute below the remaining amount; progress bar and history update.
5. Contribute exactly the remaining amount: campaign becomes COMPLETED, button disabled, creator sees "Funding target reached".
6. Admin: change a user's role; you cannot edit your own role.
7. From the browser console try `updateDoc` on a campaign: it must fail with permission-denied (after step 4 of deploy).
8. Stop the backend and press Approve: you get "Cannot reach the server", the app does not break.
9. Resize to mobile width: sidebar becomes a scrolling tab bar, cards stack.

## Notes
- The first ADMIN must exist already (your current admin keeps working). New sign-ups can only become Creator or Contributor; admins promote users from Manage Users.
- Money is displayed in INR. Change `CURRENCY`/`LOCALE` in `config.js` for USD.
- Campaign images are intentionally not part of this version.
