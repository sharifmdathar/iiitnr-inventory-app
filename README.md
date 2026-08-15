# 🎓 IIIT-NR Inventory App

[![Release](https://img.shields.io/github/v/release/sharifmdathar/iiitnr-inventory-app?label=Release&style=for-the-badge)](https://github.com/sharifmdathar/iiitnr-inventory-app/releases)
[![Downloads](https://img.shields.io/github/downloads/sharifmdathar/iiitnr-inventory-app/total.svg?style=for-the-badge)](https://github.com/sharifmdathar/iiitnr-inventory-app/releases)
[![CodeFactor](https://img.shields.io/codefactor/grade/github/sharifmdathar/iiitnr-inventory-app?label=CodeFactor&style=for-the-badge)](https://www.codefactor.io/repository/github/sharifmdathar/iiitnr-inventory-app)


[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-purple?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Fastify](https://img.shields.io/badge/Fastify-5.9.0-black?logo=fastify)](https://fastify.dev/)
[![Bun Runtime](https://img.shields.io/badge/Bun-1.3.11+-orange?logo=bun)](https://bun.sh/)
[![Drizzle ORM](https://img.shields.io/badge/Drizzle--ORM-PostgreSQL-blueviolet?logo=postgresql)](https://orm.drizzle.team/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-blue?logo=jetpackcompose)](https://github.com/JetBrains/compose-multiplatform)
[![Kotlin WASM](https://img.shields.io/badge/Kotlin-WASM-yellow?logo=webassembly)](https://kotlinlang.org/docs/wasm-overview.html)
[![License](https://img.shields.io/github/license/sharifmdathar/iiitnr-inventory-app)](LICENSE)

A state-of-the-art, secure, and robust **Inventory & Item Issue-Return Management System** custom-built for the **International Institute of Information Technology, Naya Raipur (IIIT-NR)**. This workspace comprises a cross-platform client app built using **Kotlin Multiplatform (KMP) & Compose Multiplatform** paired with a ultra-fast backend engine driven by **Fastify, Bun, Drizzle ORM, and PostgreSQL**.

---

## 📺 Demo Video

[![IIIT-NR Inventory App Demo](https://img.youtube.com/vi/MgsA0b5uwyU/maxresdefault.jpg)](https://youtu.be/MgsA0b5uwyU)

---

## 📌 Table of Contents

- [✨ Key Features](#-key-features)
- [🏗️ System Architecture](#️-system-architecture)
- [🗄️ Database Schema & Entities](#️-database-schema--entities)
- [🔄 Issue & Return Request Lifecycle](#-issue--return-request-lifecycle)
- [🛠️ Tech Stack & Dependencies](#️-tech-stack--dependencies)
- [🚀 Quick Start & Development Setup](#-quick-start--development-setup)
  - [Prerequisites](#prerequisites)
  - [Backend Setup](#backend-setup)
  - [Kotlin Multiplatform App Setup](#kotlin-multiplatform-app-setup)
- [🧰 Justfile Task Runner Cheatsheet](#-justfile-task-runner-cheatsheet)
- [🛡️ Code Quality & Static Analysis](#️-code-quality--static-analysis)

---

## ✨ Key Features

### 👤 Role-Based Auth & Profiles (RBAC)
- **Multi-Role Matrix:** Features strict role-based verification supporting five tiers: `STUDENT`, `LA` (Lab Assistant), `FACULTY`, `ADMIN`, and `PENDING`.
  - **`ADMIN`:** Full access across the system — component CRUD & images, request fulfillment/lifecycle, user management (search users, change roles/batches/branches), and exclusive access to the immutable audit log ledger.
  - **`LA` (Lab Assistant):** Inventory operations & request fulfillment — component CRUD & images, QR-code scanning, issuing/partially issuing approved requests, returning/partially returning items, and exporting CSV data.
  - **`FACULTY`:** Component catalog viewing & CSV export, supervision and approval/rejection of student project requests and renewal requests nominated to them.
  - **`STUDENT`:** Component catalog browsing, submitting item issue and renewal requests nominated under faculty supervisors, and tracking/retracting personal requests.
  - **`PENDING`:** New registrations awaiting administrator role verification.
- **Automatic Batch/Branch Derivation:** Student batch (graduation range, e.g., `2024-2028`) and branch (`CSE`, `ECE`, `DSAI`) are derived automatically from their institutional email address during signup or profile fetches.
- **Domain Gatekeeping:** Restricts registration/login to IIIT-NR emails via `ALLOWED_EMAIL_DOMAIN` configuration (e.g., `@iiitnr.edu.in`), protecting college resources.
- **Dual Authentication:** Supports traditional email-password credentials alongside secure Google Sign-in.
- **User Management Panel:** Provides administrators with a user panel to search registered accounts and update user details (names, roles, batches, and branches).

### 📦 Lab & Component Catalog Management
- **Hierarchical Layout:** Components organized by categories (`Sensors`, `Actuators`, `Microcontrollers`, `Microprocessors`, `Others`).
- **Physical Locations:** Tracks lab inventory mapping directly to physical college labs (`IoT_Lab`, `Robo_Lab`, `VLSI_Lab`).
- **Real-time Stock Auditing:** Separates `totalQuantity` from `availableQuantity` dynamically, updating automatically as items are requested, approved, and returned.
- **Cross-Platform Image Workflow:** Component images can be selected on Android, Desktop, and iOS, uploaded to the backend, and recorded in the audit log when they change.
- **Upload Guardrails:** Image uploads are restricted to common formats (`JPEG`, `PNG`, `WEBP`, `GIF`, `AVIF`) and capped at `10 MB`.
- **Platform-Native Data Export:** Built-in tool to export the component database to a CSV file from the client top bar (uses platform-specific storage on Android and Desktop).

### 🔄 Multi-State Issue, Return & Renewal System
- **Project Nominated Issues:** Students issue items under specific project titles and name a nominating `FACULTY` supervisor.
- **QR-Based Fulfillment:** Admins and LAs can fulfill and return requests instantly by scanning a student's unique Request QR code.
- **Fulfillment & Due Tracking:** Approved requests are fulfilled by admins/LAs, automatically setting a 30-day return due date (`returnDueAt`).
- **Auto-Expiry:** Fulfilled or renewed requests past their `returnDueAt` are automatically marked `EXPIRED` by a periodic background sweep.
- **Request Renewals:** Students can submit renewal requests with a detailed reason, allowing nominating faculty or admins/LAs to extend the due date by another 30 days.
- **Automatic Stock Restocking:** Returning components automatically increments `availableQuantity` on the backend, checking against `totalQuantity`.
- **Partial Issue & Return:** Admins/LAs can issue/return specific quantities per item when stock is limited or partial returns occur. Requests track `fulfilledQuantity` per item with `PARTIALLY_ISSUED` and `PARTIALLY_RETURNED` statuses, enabling multi-step fulfillment and granular inventory control.

### 🛡️ Enterprise-Grade Admin Audit Logging
- **Admin-Exclusive Immutable Ledger:** Records all system activities like `CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `LOGOUT`, `REQUEST_STATUS_CHANGE`, and `INVENTORY_ADJUST`. Only Administrators have permission to query and view audit logs.
- **System-Wide Accountability:** Stores state changes using highly detailed diffs (`oldValues` and `newValues`), tracking IP addresses, and user-agent details for advanced security audits.

### 🔄 Automatic Version Compatibility Check
- **Synchronized Deployments:** Startup compatibility check that calls the server version endpoint (`GET /version`), matching it using semantic version comparisons.
- **In-App Upgrades:** Displays a Snackbar banner to download updates from the GitHub Releases page if a newer server version is detected.

---

## 🏗️ System Architecture

The application is structured into a modern decoupled architecture. The frontend application shares Core logic, models, and network API layer across all targets (Desktop, Android, and iOS) via Kotlin Multiplatform, and connects over HTTP/JSON with the Fastify/PostgreSQL backend server.

```mermaid
graph TD
    %% Styling
    classDef shared fill:#7F52FF,stroke:#fff,stroke-width:2px,color:#fff;
    classDef backend fill:#E0533C,stroke:#fff,stroke-width:2px,color:#fff;
    classDef client fill:#3178C6,stroke:#fff,stroke-width:2px,color:#fff;
    classDef db fill:#336791,stroke:#fff,stroke-width:2px,color:#fff;

    %% Client Frontend Targets
    subgraph "Client Application (Kotlin Multiplatform)"
        Desktop["💻 Compose Desktop (JVM)"]:::client
        Android["📱 Android Client"]:::client
        iOS["🍏 iOS Client"]:::client
        Web["🌐 Web (WasmJs)"]:::client
        
        %% Shared Core Module
        subgraph "Shared Core Module (shared)"
            UI["🎨 Compose Multiplatform UI Screens"]:::shared
            API["📡 Ktor ApiClient & Services"]:::shared
            Cache["💾 ComponentsCache (In-Memory)"]:::shared
            Models["📋 Common Kotlin Models"]:::shared
        end
    end

    %% Network Connection
    Desktop & Android & iOS & Web -.-> UI
    UI --> API
    API --> Cache
    API --> Models

    %% Backend Engine
    subgraph "Backend Services (Fastify & Bun)"
        API_Gateway["⚡ Fastify Server (Bun Engine)"]:::backend
        Routes["📂 Routers (Auth, Admin, Components, Requests)"]:::backend
        Drizzle["🧩 Drizzle ORM (TypeScript Types)"]:::backend
    end

    API -- HTTP / JSON --> API_Gateway
    API_Gateway --> Routes
    Routes --> Drizzle

    %% Database
    Database[("🐘 PostgreSQL Database")]:::db
    Drizzle -- SQL Queries --> Database

```

---

## 🗄️ Database Schema & Entities

The PostgreSQL schema is powered by **Drizzle ORM** for strong typing and performance. Here are the core entities:

```mermaid
erDiagram
    User {
        text id PK
        text email UK
        text name
        text imageUrl
        text passwordHash
        text googleId UK
        UserRole role
        text batch
        text branch
        timestamp createdAt
        timestamp updatedAt
    }

    Component {
        text id PK
        text name
        text description
        text imageUrl
        integer totalQuantity
        integer availableQuantity
        ComponentCategory category
        Location location
        timestamp createdAt
        timestamp updatedAt
    }

    Request {
        text id PK
        text userId FK
        text targetFacultyId FK
        text projectTitle
        RequestStatus status
        timestamp returnDueAt
        timestamp returnedAt
        timestamp fulfilledAt
        text receivedByUserId
        timestamp lastRenewDate
        text lastRenewReason
        timestamp createdAt
        timestamp updatedAt
    }

    RequestItem {
        text id PK
        text requestId FK
        text componentId FK
        integer quantity
        integer fulfilledQuantity
        timestamp createdAt
        timestamp updatedAt
    }

    AuditLog {
        text id PK
        text userId FK
        AuditActionType action
        text entityType
        text entityId
        text oldValues
        text newValues
        varchar ipAddress
        text userAgent
        text metadata
        timestamp createdAt
    }

    User ||--o{ Request : "submits"
    User ||--o{ Request : "supervises (Faculty)"
    User ||--o{ AuditLog : "triggers"
    Request ||--o{ RequestItem : "contains"
    Component ||--o{ RequestItem : "included_in"
```

---

## 🔄 Issue & Return Request Lifecycle

The system utilizes an advanced, automated state machine to track components issue requests, from submission to final return:

```mermaid
stateDiagram-v2
    [*] --> PENDING : Student creates a Request with Supervisor nomination
    
    PENDING --> REJECTED : Faculty or Admin disapproves Request
    PENDING --> APPROVED : Faculty Supervisor approves Request
    
    APPROVED --> ISSUED : Admin/LA hands over all components & deducts stock
    APPROVED --> PARTIALLY_ISSUED : Admin/LA hands over some components (partial stock)
    APPROVED --> REJECTED : Admin cancels/rejects the pickup
    
    ISSUED --> RETURNED : Student returns all components & Admin/LA validates stock restock
    ISSUED --> PARTIALLY_RETURNED : Student returns some components (partial return)
    ISSUED --> EXPIRED : Return due date passed without return
    ISSUED --> REQUESTED_RENEW : Student requests extra time with reason
    
    PARTIALLY_ISSUED --> ISSUED : Admin/LA hands over remaining components
    PARTIALLY_ISSUED --> PARTIALLY_RETURNED : Student returns some of the issued components
    PARTIALLY_ISSUED --> EXPIRED : Return due date passed without return
    PARTIALLY_ISSUED --> REQUESTED_RENEW : Student requests extra time with reason
    
    PARTIALLY_RETURNED --> RETURNED : Student returns remaining components
    PARTIALLY_RETURNED --> EXPIRED : Return due date passed without return
    PARTIALLY_RETURNED --> REQUESTED_RENEW : Student requests extra time with reason
    
    REQUESTED_RENEW --> RENEWED : Faculty or Admin approves renewal request
    REQUESTED_RENEW --> ISSUED : Renewal rejected (original deadline stands)
    REQUESTED_RENEW --> PARTIALLY_ISSUED : Renewal rejected (partially issued deadline stands)
    REQUESTED_RENEW --> EXPIRED : Return due date passed without action
    
    RENEWED --> RETURNED : Student returns components after renewal
    RENEWED --> EXPIRED : Return due date passed without return
    
    EXPIRED --> RETURNED : Student returns an overdue request
    EXPIRED --> PARTIALLY_RETURNED : Student returns some overdue components
    
    REJECTED --> [*]
    RETURNED --> [*]
```

---

## 🛠️ Tech Stack & Dependencies

### Backend Engine (`/backend`)
*   **Runtime:** [Bun Runtime](https://bun.sh/) (Extremely fast JavaScript/TypeScript runtime)
*   **Web Framework:** [Fastify 5](https://fastify.dev/) (High-performance API server framework)
*   **ORM:** [Drizzle ORM](https://orm.drizzle.team/) (Next-generation type-safe database layer)
*   **Database:** PostgreSQL (Robust and secure relational store)
*   **Security & Protection:** 
    *   `@fastify/helmet` for secure HTTP headers.
    *   `@fastify/rate-limit` for rate limiting (100 requests per minute by default).
    *   `xss` for cross-site scripting input protection.
    *   `bcryptjs` for strong password hashing.
    *   `@fastify/jwt` for stateless JSON Web Token session auth.
    *   `google-auth-library` to securely verify Google OAuth sign-in tokens.
*   **Logging:** `pino-pretty` for structured, beautiful developer logs.
*   **Testing:** Native `bun test` runner.

### Kotlin Multiplatform Client (`/app`)
*   **Target Core Architecture:** Clean Architecture separating UI, Presentation, API, Cache, and Storage.
*   **UI Engine:** Jetpack Compose Multiplatform (UI codebase shared across all targets).
*   **Navigation:** `androidx.navigation.compose` for unified screen graph routing.
*   **Networking:** [Ktor HTTP Client](https://ktor.io/) with `ContentNegotiation` and `kotlinx.serialization` for robust type-safe API requests.
*   **Local Caching:** Dedicated `ComponentsCache` for lightning-fast search queries.
*   **Token Store:** `TokenManager` for persistent JWT handling.
*   **Static Code Analysis:** Kotlin `detekt` static analyzer & `ktlint` formatter checks.

---

## 🚀 Quick Start & Development Setup

### Prerequisites
Make sure you have the following installed on your developer machine:
- [Bun Runtime](https://bun.sh/) (v1.3.11+)
- [Java Development Kit (JDK) 17+](https://adoptium.net/temurin/releases/) (Required for KMP compilation)
- [Podman](https://podman.io/) or [Docker](https://www.docker.com/) (For launching the database)
- [Just](https://github.com/casey/just) task runner (Optional, but highly recommended)

---

### Backend Setup

1. **Navigate to the Backend directory:**
   ```bash
   cd backend
   ```

2. **Install Dependencies:**
   ```bash
   bun install
   ```

3. **Configure Environment Variables:**
   Copy the existing `.env` template or modify it:
   ```env
   DATABASE_URL=postgresql://<username>:<password>@localhost:5432/iiitnr_inventory
   TEST_DATABASE_URL=postgresql://<username>:<password>@localhost:5432/iiitnr_inventory_test
   PORT=4000
   JWT_SECRET=your_super_secret_jwt_sign_key
   GOOGLE_CLIENT_ID=your_google_oauth_client_id.apps.googleusercontent.com
   ALLOWED_EMAIL_DOMAIN=@iiitnr.edu.in
   ALLOW_UNVERIFIED_EMAIL=false
   
   # Admin Seed Credentials
   ADMIN_EMAIL=admin@test.com
   ADMIN_PASSWORD=admin123
   ADMIN_NAME="System Administrator"
   ```

4. **Spin up a local PostgreSQL Instance (via Podman Compose):**
   ```bash
   # Launch DB container
   just db-up
   ```

5. **Generate & Apply Database Migrations:**
   ```bash
   # Runs migration scripts and seeds database
   bun run migrate
   ```

6. **Seed default data (Labs, Categories, Initial Admin):**
   ```bash
   bun run seed
   ```

7. **Start the API Server (Development Mode):**
   ```bash
   just dev
   ```
   The backend API will be available at `http://localhost:4000`.

---

### Kotlin Multiplatform App Setup

The frontend app shares the core UI and logic in `app/shared`, while platform specific runners exist for Desktop, Android, and iOS.

#### 1. Running the Compose Desktop Client
You can run the Desktop application immediately using the preconfigured gradle task:
```bash
cd app
./gradlew desktop:run
```
*(Or simply run `just desk` from the project root)*

#### 2. Running the Android Client
Open the `app` directory in **Android Studio**. Make sure the target emulator or physical device is connected, and select the `android` module configuration and click **Run**.

*Note: For Android Emulators to connect to local backend, ensure the ApiClient URL configuration resolves to `http://10.0.2.2:4000`.*

#### 3. Running the iOS Client
Prerequisites: macOS with Xcode installed. Open the `/app/iosApp` project workspace inside Xcode, configure your developer certs, select a simulated iOS device, and press **Cmd + R**.

#### 4. Running the Web (WasmJs) Client
You can run the Web application in development mode with hot-reload:
```bash
cd app
./gradlew :web:wasmJsBrowserDevelopmentRun
```
*(Or simply run `just web` from the project root)*

---

## 🧰 Justfile Task Runner Cheatsheet

If you have `just` runner installed, you can use the following shortcuts from the **root** of the repository:

| Command | Action Description | Target Component |
| :--- | :--- | :--- |
| `just install` | Installs backend node modules using `bun` | Backend |
| `just dev` | Launches backend migration, seeds, and boots watch-mode server | Backend |
| `just db-up` | Starts a PostgreSQL instance using Compose | Database |
| `just db-down` | Tears down the PostgreSQL Compose instance | Database |
| `just up` / `just down` | Spins up/down the complete Podman Compose stack | Full Stack |
| `just logs` | Follows logs from the compose container stack | Diagnostics |
| `just test` | Launches database and executes the backend test suite | Quality / CI |
| `just desk` | Compiles and launches the desktop app locally | Compose Desktop |
| `just web` | Launches the Kotlin WasmJs web application | Compose Web |
| `just lint` | Runs eslint on backend and ktlint checks on client | Quality / Checks |
| `just lint-fix` | Formats both backend source files and Kotlin source files | Formatters |
| `just typecheck` | Validates TypeScript configuration and resolves type-errors | Backend TypeScript |
| `just fmt` | Prettifies backend and client files | Formatters |
| `just detekt` | Runs deep static code quality analysis on KMP project | Quality Client |

---

## 🛡️ Code Quality & Static Analysis

We maintain strict quality control standards across both projects:

*   **Backend Linting:** Fastify TypeScript styles are governed by modern ESLint and Prettier rules.
    ```bash
    just lint
    ```
*   **Kotlin Linting & Formatting:** Enforced via `ktlint` plugin.
    ```bash
    # View linting errors
    cd app && ./gradlew ktlintCheck
    # Auto-format errors
    cd app && ./gradlew ktlintFormat
    ```
*   **Kotlin Static Analysis:** detekt analyzes code complexity, potential memory leaks, code smells, and styling issues.
    ```bash
    just detekt
    ```

---

*Made with ❤️ for IIIT Naya Raipur.*
