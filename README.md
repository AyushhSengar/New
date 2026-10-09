# FundHub — Modern Java Crowdfunding Platform

FundHub is a modern, full-stack Java web application for managing community fundraising campaigns. It features three distinct user roles (**ADMIN**, **CREATOR**, and **CONTRIBUTOR**), an atomic server-side transaction engine, and a premium responsive UI.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          BROWSER / FRONTEND (HTML5/CSS3/JS)                 │
│  • Modern Obsidian & Indigo Design System with Glassmorphism & Responsive UI│
│  • Firebase Authentication (Client Sign-In & ID Token generation)           │
│  • Cloud Firestore Real-time Listeners (Read-Only, Role-Scoped Feeds)       │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Authorization: Bearer <Firebase ID Token>
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    JAVA BACKEND (Java 17 + Spring Boot 3.3.5)               │
│  • FirebaseAuthFilter: Cryptographically verifies tokens & loads role       │
│  • Controllers: AccountController, CampaignController                       │
│  • Services: UserService, CampaignService, ContributionService              │
│  • Security & Rules: Role checks, 2-decimal precision, Concurrency control  │
│  • Firebase Admin SDK (Atomic Firestore Transactions)                       │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       ▼
                        Google Cloud Firestore Database
                        (users, campaigns, donations)
```

---

## Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Backend Core** | Java 17, Spring Boot 3.3.5 (`spring-boot-starter-web`) | Application logic, request routing, role enforcement, business rules |
| **Server Security** | Firebase Admin SDK 9.4.1 | Token verification, server-side data access, atomic transactions |
| **Testing** | JUnit 5, Mockito, AssertJ | Automated unit and integration testing (23 tests, 100% pass) |
| **Build Tool** | Apache Maven 3.10.0+ | Dependency management and JAR packaging |
| **Database & Identity** | Cloud Firestore, Firebase Authentication | User identity, real-time snapshot listeners |
| **Frontend Presentation** | Semantic HTML5, Modern CSS3 | Responsive grid/flexbox layout, status pills, progress bars |
| **Client Scripting** | Vanilla ES Modules (`script.js`, `config.js`) | Form capture, DOM updates, token attachment, Firestore snapshot binding |

---

## Role Permissions & Workflows

### 1. ADMIN
- **Platform Analytics:** Views live counts of pending, approved, rejected, completed campaigns, total funds raised, and user distribution.
- **Campaign Review:** Reviews submissions in `PENDING` status. Approves to `APPROVED` or rejects to `REJECTED` (with mandatory reason feedback).
- **User Role Management:** Promotes registered users to `ADMIN`, `CREATOR`, or `CONTRIBUTOR` (cannot modify own role to prevent lockout).

### 2. CREATOR
- **Create Campaign:** Submits fundraising requests with Title ($\le 120$ chars), Description ($\le 2000$ chars), and Target Goal ($> 0$).
- **Status Tracking:** Initially saved as `PENDING`. Tracks live progress %, target vs collected amounts, and admin feedback reasons.
- **Search & Filter:** Real-time search across submitted campaigns.

### 3. CONTRIBUTOR
- **Browse Campaigns:** Views approved and completed campaigns.
- **Simulated Demo Contributions:** Records demo contributions up to the remaining goal amount.
- **Contribution History:** Real-time ledger of all supported causes and amounts.
- **Automatic Completion:** When collected amount meets the target goal, the campaign automatically transitions to `COMPLETED`.

---

## Demo / Simulated Contribution Notice

> **Academic / Demonstration Mode:**
> FundHub is strictly designed as an academic demonstration. It does **NOT** integrate real payment gateways (Razorpay, Stripe, PayPal, UPI) and does **NOT** collect sensitive banking or card credentials. Contributions are recorded as simulated ledger entries with `simulated: true` stored in Firestore.

---

## Folder Structure

```
c:\Users\ayush\OneDrive\Desktop\Java Sem 3\Project/
├── backend/
│   ├── pom.xml                     # Maven project descriptor (Java 17, Spring Boot 3.3.5)
│   ├── Dockerfile                  # Container specification
│   ├── .gitignore                  # Ignores target/ and key files
│   └── src/
│       ├── main/
│       │   ├── java/com/crowdfund/
│       │   │   ├── CrowdfundApplication.java
│       │   │   ├── config/         (CorsConfig, FirebaseConfig)
│       │   │   ├── controller/     (AccountController, CampaignController)
│       │   │   ├── dto/            (AmountRequest, CreateCampaignRequest, ReasonRequest, RegisterRequest, RoleRequest)
│       │   │   ├── exception/      (AppException, GlobalExceptionHandler)
│       │   │   ├── security/       (AuthUser, FirebaseAuthFilter)
│       │   │   ├── service/        (UserService, CampaignService, ContributionService)
│       │   │   └── util/           (FirestoreSupport, Money, Text)
│       │   └── resources/
│       │       └── application.properties
│       └── test/java/com/crowdfund/ # 23 Automated JUnit 5 / Mockito Unit Tests
├── frontend/
│   ├── index.html                  # FundHub Single-Page Application
│   ├── style.css                   # Modern Design System (Indigo/Slate, Glassmorphism, Responsive)
│   ├── script.js                   # Client interactions & live read listeners
│   ├── config.js                   # Backend endpoint configuration
│   └── firestore.rules             # Production security rules (zero client direct writes)
├── index.html                      # Root entry point
├── style.css                       # Root stylesheet
├── script.js                       # Root script
├── config.js                       # Root config
├── firestore.rules                 # Root security rules
└── README.md                       # Comprehensive documentation
```

---

## Local Development Guide

### Prerequisites
- **Java 17+** (OpenJDK / Temurin recommended)
- **Apache Maven 3.8+**
- **Modern Web Browser** (Chrome, Edge, Firefox, Safari)

### 1. Configure Firebase Service Account Key
1. In the Firebase Console, go to **Project Settings** $\rightarrow$ **Service Accounts** $\rightarrow$ **Generate New Private Key**.
2. Save the JSON file outside your repository (e.g., `C:\keys\fundhub-key.json`).
3. Set the environment variable:
   ```powershell
   # Windows (PowerShell)
   $env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\keys\fundhub-key.json"

   # macOS / Linux (Bash)
   export FIREBASE_SERVICE_ACCOUNT_PATH="/path/to/fundhub-key.json"
   ```

### 2. Run the Java Backend
```bash
cd backend
mvn spring-boot:run
```
Test health endpoint: Open `http://localhost:8080/api/health` $\rightarrow$ `{"status":"ok"}`.

### 3. Run Automated Tests
```bash
cd backend
mvn test
```
All 23 unit and integration tests across Controllers, Services, Utilities, Security, and Exception Handlers will execute.

### 4. Run the Frontend
Serve the project directory over a local HTTP server:
```bash
# Using Python
python -m http.server 5500

# Or using VS Code Live Server extension (Port 5500)
```
Open `http://localhost:5500` in your web browser.

---

## API Reference (Bearer Token Required)

| Method | Endpoint | Authorized Role | Request Body | Description |
|---|---|:---:|---|---|
| `GET` | `/api/health` | Public | None | Health check |
| `GET` | `/api/me` | Any Authenticated | None | Returns verified caller info |
| `POST` | `/api/register` | New User (No Profile) | `{ name, role }` | Registers Creator or Contributor profile |
| `POST` | `/api/campaigns` | `CREATOR` | `{ title, description, targetAmount }` | Creates new campaign in `PENDING` state |
| `POST` | `/api/campaigns/{id}/approve` | `ADMIN` | None | Approves pending campaign |
| `POST` | `/api/campaigns/{id}/reject` | `ADMIN` | `{ reason }` | Rejects pending campaign with feedback |
| `POST` | `/api/campaigns/{id}/contributions` | `CONTRIBUTOR` | `{ amount }` | Records atomic simulated contribution |
| `PATCH` | `/api/users/{uid}/role` | `ADMIN` | `{ role }` | Updates authorized user role |

---

## Semester 3 Viva Concepts

1. **Encapsulation:** Java DTO records ([`AmountRequest`](file:///c:/Users/ayush/OneDrive/Desktop/Java%20Sem%203/Project/backend/src/main/java/com/crowdfund/dto/AmountRequest.java), [`CreateCampaignRequest`](file:///c:/Users/ayush/OneDrive/Desktop/Java%20Sem%203/Project/backend/src/main/java/com/crowdfund/dto/CreateCampaignRequest.java)) ensure immutability and valid construction.
2. **Layered Architecture:** Clear separation between Controllers (REST API), Services (Business Logic), Utilities (Stateless helpers), and Security Filters.
3. **Concurrency Control:** Atomic transactions in [`ContributionService.java`](file:///c:/Users/ayush/OneDrive/Desktop/Java%20Sem%203/Project/backend/src/main/java/com/crowdfund/service/ContributionService.java) prevent race conditions when multiple contributors support a campaign simultaneously.
4. **Exception Handling:** Centralized [`GlobalExceptionHandler`](file:///c:/Users/ayush/OneDrive/Desktop/Java%20Sem%203/Project/backend/src/main/java/com/crowdfund/exception/GlobalExceptionHandler.java) translates [`AppException`](file:///c:/Users/ayush/OneDrive/Desktop/Java%20Sem%203/Project/backend/src/main/java/com/crowdfund/exception/AppException.java) instances into standard HTTP responses.
