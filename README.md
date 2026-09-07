---
title: VaultMD
emoji: 🔐
colorFrom: blue
colorTo: indigo
sdk: docker
app_port: 7860
pinned: false
---

<div align="center">

# 🔐 VaultMD

**A privacy-first, AI-powered medical record platform with patient-controlled access, compiler-enforced security invariants, and full audit transparency.**

[![CI](https://github.com/kesavadatta2410/VaultMD/actions/workflows/ci.yml/badge.svg)](https://github.com/kesavadatta2410/VaultMD/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](./LICENSE)
[![Java 17](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Python 3.11](https://img.shields.io/badge/Python-3.11-3776AB?logo=python&logoColor=white)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110-009688?logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![ChromaDB](https://img.shields.io/badge/ChromaDB-0.4-FF6B35)](https://www.trychroma.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)

</div>

---

> ⚠️ **Demo project** — all data is fully synthetic. No real patient data is used or stored anywhere in this repository.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [System Architecture](#-system-architecture)
  - [High-Level Design](#high-level-design)
  - [Why Two Services?](#why-two-services)
  - [Data Model](#data-model)
  - [Backend Package Structure](#backend-package-structure)
  - [AI Service Module Structure](#ai-service-module-structure)
- [Security Model](#-security-model)
  - [The Core Access Rule](#the-core-access-rule)
  - [Compiler-Enforced Invariants](#compiler-enforced-invariants)
  - [The Audit Trail](#the-audit-trail)
  - [Additional Hardening](#additional-hardening)
- [API Reference](#-api-reference)
  - [Authentication](#authentication)
  - [Consent Management](#consent-management)
  - [Emergency Access](#emergency-access)
  - [AI Assistant](#ai-assistant)
  - [Audit Log](#audit-log)
  - [Health Records](#health-records)
- [RAG Pipeline Deep Dive](#-rag-pipeline-deep-dive)
- [Quick Start](#-quick-start)
  - [Prerequisites](#prerequisites)
  - [Setup and Run](#setup-and-run)
  - [Service Endpoints](#service-endpoints)
  - [Demo Accounts](#demo-accounts)
- [End-to-End Demo Flow](#-end-to-end-demo-flow)
- [Running Tests](#-running-tests)
- [Configuration Reference](#-configuration-reference)
- [Deployment](#-deployment)
- [Known Limitations and Roadmap](#-known-limitations-and-roadmap)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌐 Overview

**VaultMD** is a privacy-first health record platform that gives patients full, granular ownership of their medical data. Instead of a hospital or clinic system deciding who sees what, the **patient is the access authority**.

Patients decide:
- **Which doctors** can query their records
- **For how long** (optional expiry on consent)
- And they can **revoke access instantly** at any time

Authorized doctors interact with patient records through a **natural-language AI assistant** powered by Retrieval-Augmented Generation (RAG). The assistant retrieves only the most semantically relevant record chunks and generates a grounded, cited answer — it cannot hallucinate data that is not in the patient's records.

Every access — whether via explicit patient consent or a documented medical emergency — is permanently and immutably logged. Patients can review the full audit trail at any time: not just *who* accessed their data, but the exact question asked, which records were cited, and *why the doctor was authorized*.

### What makes VaultMD architecturally interesting

The project demonstrates a key engineering principle: **security-critical invariants should be enforced by the type system, not just by policy**. Unauthorized AI access is not merely *against the rules* — it is a **compile error**. No developer can accidentally bypass the consent gate; the code simply will not compile without it.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔐 **Role-Based Authentication** | Patients and doctors have isolated accounts. JWT tokens are issued on login and validated on every request via Spring Security's filter chain. |
| 📋 **Time-Bound Consent Management** | Patients grant access to specific doctors with an optional `expiresAt` timestamp. Consent can be revoked at any time, taking effect immediately. |
| 🚨 **Auditable Emergency Access** | Doctors can log a justified emergency override to access records without prior consent. Every such access creates a permanent, timestamped audit trail visible to the patient. |
| 🤖 **Grounded RAG AI Assistant** | Doctors query patient records in natural language. The assistant retrieves the top-K semantically similar chunks and generates a cited, grounded answer — limited strictly to what is in the records. |
| 📜 **Immutable Audit Log** | Every AI query is logged: the doctor who asked, the question, which records were cited, and the authorization mechanism (CONSENT or EMERGENCY, with the specific row ID). |
| 🧩 **LLM-Optional Fallback** | Works without any LLM API key. If no Gemini key is configured, the service returns the most relevant verbatim chunk from the vector store (extractive answer). |
| 🐳 **Zero-Dependency Local Setup** | The entire stack (PostgreSQL, AI service, backend) starts with a single `docker compose up --build`. No Java, Python, or database installation required locally. |
| 🏗️ **Data Isolation by Design** | Each patient's embeddings are stored in a separate ChromaDB collection, scoped by `patient_id`. There is no query path that can cross-access another patient's data. |

---

## 🛠️ Technology Stack

### Backend (`/backend`)

| Component | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.2.5 |
| Security | Spring Security + JJWT | 0.11.5 |
| Persistence | Spring Data JPA + Hibernate | — |
| Database | PostgreSQL | 16 |
| In-Memory DB (HF profile) | H2 | — |
| Build tool | Maven | 3.9+ |
| Test framework | JUnit 5 + Mockito | — |

### AI Service (`/ai-service`)

| Component | Technology | Version |
|---|---|---|
| Language | Python | 3.11 |
| Web framework | FastAPI | 0.110.2 |
| Server | Uvicorn | 0.29.0 |
| Embeddings | fastembed (`all-MiniLM-L6-v2`) | 0.7.1 |
| Vector store | ChromaDB | 0.4.24 |
| LLM (optional) | Google Gemini via `google-genai` | 1.16.0 |
| Rate limiting | slowapi | 0.1.9 |
| Schema validation | Pydantic v2 | 2.7.1 |
| Test framework | pytest | — |

### Infrastructure

| Component | Technology |
|---|---|
| Containerisation | Docker + Docker Compose V2 |
| CI/CD | GitHub Actions |
| Secrets management | `.env` file (gitignored), `.env.example` provided |

---

## 🏗️ System Architecture

### High-Level Design

```
+----------------------------------------------------------+
|                    Patient / Doctor                      |
|                  (Browser - No build)                    |
|                  frontend/index.html                     |
+-------------------------+--------------------------------+
                          |  HTTP (JWT Bearer)
                          v
+----------------------------------------------------------+
|                Spring Boot Backend                       |
|                 Java 17 - Port 8080                      |
|                                                          |
|  +----------+  +----------+  +----------+  +----------+ |
|  |   Auth   |  | Consent  |  |Emergency |  |  Audit   | |
|  |Controller|  |Controller|  |Controller|  |   Log    | |
|  +----------+  +----------+  +----------+  +----------+ |
|                                                          |
|  +-----------------------------------------------------+ |
|  |             AccessControlService                    | |
|  |   (sole constructor of AccessGrant - enforced)      | |
|  +-------------------------+--------------------------+ |
|                            |  AccessGrant (required)   |
|  +-------------------------v--------------------------+ |
|  |                 AiServiceClient                    | |
|  |        (only path to AI service /query)            | |
|  +-------------------------+--------------------------+ |
|                            |                            |
+----------------------------+----------------------------+
                             |  Internal HTTP
                             v
+----------------------------------------------------------+
|                   FastAPI AI Service                     |
|                   Python 3.11 - Port 8000                |
|                                                          |
|  POST /ingest => chunker => fastembed => ChromaDB        |
|  POST /query  => fastembed => ChromaDB => Gemini         |
|                                    (or extractive)       |
+----------------------------------------------------------+
                             |
             +---------------+----------------+
             |                                |
             v                                v
+------------------------+      +------------------------+
|        ChromaDB        |      |      PostgreSQL 16      |
|  Per-patient collection|      |  Consent, Audit, Users  |
|  (vaultmd_chroma_data) |      |  (vaultmd_pg_data)      |
+------------------------+      +------------------------+
```

### Full Project Layout

```
vaultmd/
|-- backend/                          Spring Boot (Java 17)
|   |-- src/main/java/com/vaultmd/backend/
|   |   |-- assistant/                AccessControlService, AccessGrant, AiServiceClient
|   |   |-- config/                   CORS, RestTemplate, Security config
|   |   |-- controller/               REST controllers (Auth, Consent, Assistant, etc.)
|   |   |-- dto/                      Request/response DTOs
|   |   |-- exception/                Typed exceptions + GlobalExceptionHandler
|   |   |-- model/                    JPA entities (Patient, Doctor, Consent, etc.)
|   |   |-- repository/               Spring Data JPA repositories
|   |   |-- security/                 JWT filter, UserPrincipal, UserDetailsService
|   |   +-- service/                  Business logic services
|   |-- Dockerfile
|   +-- pom.xml
|
|-- ai-service/                       FastAPI (Python 3.11)
|   |-- app/
|   |   |-- main.py                   FastAPI app, /health, /ingest, /query endpoints
|   |   |-- chunking.py               Sliding-window text chunker
|   |   |-- embeddings.py             fastembed wrapper (all-MiniLM-L6-v2)
|   |   |-- vectorstore.py            ChromaDB client, per-patient collections
|   |   |-- llm.py                    Gemini generation + extractive fallback
|   |   |-- schemas.py                Pydantic request/response models
|   |   +-- config.py                 Settings loaded from .env
|   |-- tests/
|   |-- Dockerfile
|   +-- requirements.txt
|
|-- frontend/                         Vanilla HTML + CSS + JS
|   |-- index.html                    Single-page patient and doctor UI
|   |-- app.js                        All client-side logic
|   +-- styles.css
|
|-- .github/
|   +-- workflows/ci.yml              GitHub Actions: backend + AI service tests
|
|-- docker-compose.yml
|-- postman_collection.json
|-- .env.example
|-- CONTRIBUTING.md
+-- LICENSE
```

### Why Two Services?

| Concern | Backend (Java / Spring Boot) | AI Service (Python / FastAPI) |
|---|---|---|
| **Primary responsibility** | Auth, consent logic, access control, audit | Chunking, embedding, vector search, LLM |
| **Why this language** | Spring Security makes it structurally hard to expose an endpoint without a role check. JPA gives transactional ACID guarantees for consent and audit data. | The ML/embedding ecosystem is Python-native. `fastembed`, `chromadb`, and `google-genai` are all Python-first. |
| **Dependency isolation** | ML libraries (PyTorch, etc.) do not bloat or destabilize the JVM service | Can be scaled, restarted, or swapped independently of the backend |
| **Replaceability** | Backend can be rewritten without touching the vector store | AI service can be swapped (e.g. Gemini for OpenAI) without touching auth or consent logic |

### Data Model

The relational data model (PostgreSQL) consists of the following entities:

```
Patient --< HealthRecord
Patient --< Consent >-- Doctor
Patient --< EmergencyAccess >-- Doctor
Patient --< AuditLog >-- Doctor
```

| Entity | Key Fields | Notes |
|---|---|---|
| `Patient` | `id`, `name`, `email`, `passwordHash`, `role=PATIENT` | Separate table from Doctor for clarity |
| `Doctor` | `id`, `name`, `email`, `passwordHash`, `role=DOCTOR` | — |
| `HealthRecord` | `id`, `patientId`, `type`, `content`, `createdAt` | Plain-text content ingested into ChromaDB on creation |
| `Consent` | `id`, `patientId`, `doctorId`, `status` (ACTIVE/REVOKED), `expiresAt` | Nullable `expiresAt`; revocation sets `status=REVOKED` immediately |
| `EmergencyAccess` | `id`, `doctorId`, `patientId`, `justification`, `accessedAt` | Immutable once created; validity window controlled by `EMERGENCY_ACCESS_VALIDITY_HOURS` |
| `AuditLog` | `id`, `doctorId`, `patientId`, `question`, `recordIds`, `accessType`, `grantedById`, `createdAt` | `accessType` = CONSENT or EMERGENCY; `grantedById` references the specific Consent or EmergencyAccess row |

### Backend Package Structure

```
com.vaultmd.backend/
|-- assistant/
|   |-- AccessControlService.java     Sole producer of AccessGrant - the consent/emergency gate
|   |-- AccessGrant.java              Value type required by AiServiceClient.query()
|   |-- AiServiceClient.java          HTTP client to ai-service; only accepts AccessGrant
|   +-- AiServiceModels.java          Java records mirroring the AI service JSON schemas
|
|-- controller/
|   |-- AuthController.java           POST /auth/register, POST /auth/login
|   |-- ConsentController.java        POST /consent, GET /consent, DELETE /consent/{id}
|   |-- EmergencyAccessController.java POST /emergency-access, GET /emergency-access
|   |-- AssistantController.java      POST /assistant/query
|   |-- AuditLogController.java       GET /audit-log, GET /audit-log/{id}
|   |-- RecordController.java         POST /records, GET /records/{patientId}
|   +-- HealthController.java         GET /health
|
|-- service/
|   |-- AuthService.java              Registration, login, JWT issuance
|   |-- ConsentService.java           Grant, list, revoke consent
|   |-- EmergencyAccessService.java   Log emergency, list emergency accesses
|   |-- AssistantService.java         Orchestrates: authorize => query AI => log audit
|   |-- AuditLogService.java          Write and read audit records
|   |-- RecordService.java            CRUD for health records + ingest trigger
|   +-- DataSeeder.java               Seeds demo patients, doctors, records on startup
|
|-- model/                            JPA entities
|-- repository/                       Spring Data JPA repositories
|-- dto/                              Request/response DTOs with Bean Validation
|-- security/                         JWT filter, UserPrincipal, UserDetailsService
+-- exception/                        GlobalExceptionHandler, typed API exceptions
```

### AI Service Module Structure

```
app/
|-- main.py          FastAPI app setup, rate limiting, /health /ingest /query endpoints
|-- chunking.py      Sliding-window character chunker with whitespace-safe boundaries
|-- embeddings.py    fastembed wrapper - loads all-MiniLM-L6-v2 locally (no API key)
|-- vectorstore.py   ChromaDB client - get_collection(patient_id) scopes to one patient
|-- llm.py           Gemini generation with temperature=0; extractive fallback if no key
|-- schemas.py       Pydantic v2 models: IngestRequest/Response, QueryRequest/Response
+-- config.py        Pydantic Settings - loads from ai-service/.env then project root .env
```

---

## 🛡️ Security Model

### The Core Access Rule

> **A doctor's question reaches the AI service only if, at query time, at least one of the following is true:**
>
> - **(A) Active Consent:** An `ACTIVE`, non-expired `Consent` row exists for the `(doctor, patient)` pair.
> - **(B) Recent Emergency:** The doctor has logged an `EmergencyAccess` for that patient within the last `N` hours *(default: 12 hours, configurable via `EMERGENCY_ACCESS_VALIDITY_HOURS`)*.
>
> If neither condition holds, the request is rejected with **HTTP 403 Forbidden** before it ever reaches the AI service.

### Compiler-Enforced Invariants

This invariant is not a runtime check that could be forgotten — it is **structurally baked into the type system**:

**Step 1:** `AccessControlService.authorize(doctorId, patientId)` is the **one and only** method that constructs an `AccessGrant` object. It throws `ForbiddenApiException` if neither condition is met.

```java
// Only AccessControlService can construct this:
return new AccessGrant(doctor, patient, accessType, grantedById);
```

**Step 2:** `AiServiceClient.query(AccessGrant grant, String question)` — the **only** method that calls the AI service's `/query` endpoint — requires an `AccessGrant` as a **mandatory parameter**.

```java
// Won't compile without a valid AccessGrant:
AiServiceModels.QueryResponse response = aiServiceClient.query(grant, question);
```

**Result:** It is a **compile error** to call the AI service without first obtaining an `AccessGrant` from `AccessControlService`. No developer can accidentally add a code path that bypasses the consent/emergency check.

**Step 3:** `AssistantService.query()` is the single orchestration point — authorized by design:

```java
public AssistantQueryResponse query(Long doctorId, Long patientId, String question) {
    AccessGrant grant = accessControlService.authorize(doctorId, patientId); // throws 403 if denied
    AiServiceModels.QueryResponse aiResponse = aiServiceClient.query(grant, question);
    auditLogService.record(grant, question, sources); // always written after success
    return new AssistantQueryResponse(...);
}
```

### The Audit Trail

Every successful AI query writes an `AuditLog` row containing:

| Field | What it stores |
|---|---|
| `doctorId` | Who asked the question |
| `patientId` | Whose records were accessed |
| `question` | The exact question the doctor asked |
| `recordIds` | Which records the AI cited in its answer |
| `accessType` | `CONSENT` or `EMERGENCY` |
| `grantedById` | The specific `Consent.id` or `EmergencyAccess.id` that authorized the access |
| `createdAt` | Timestamp of the access |

Patients retrieve their full audit history via `GET /audit-log`. The endpoint is ownership-checked: a patient can only see their own log.

### Additional Hardening

| Area | Implementation |
|---|---|
| **Patient data isolation** | Each patient has a separate ChromaDB collection named by `patient_id`. `get_collection(patient_id)` ensures every `/query` is scoped to exactly one patient — no query can cross-access another patient's data. |
| **AI endpoint rate limiting** | `POST /ingest` is limited to 60 requests/minute; `POST /query` to 20 requests/minute via `slowapi`. |
| **JWT validation** | Every protected endpoint validates the JWT signature, expiry, and role via Spring Security's filter chain before the controller method is invoked. |
| **No internal details in responses** | `GlobalExceptionHandler` maps all exceptions to typed HTTP responses — stack traces and internal details are never leaked. |
| **Secrets never committed** | `.env` is gitignored. `.env.example` provides a template with documentation but no real values. |

---

## 📡 API Reference

All backend endpoints are served at `http://localhost:8080`. Protected endpoints require `Authorization: Bearer <token>` obtained from `POST /auth/login`.

### Authentication

| Method | Endpoint | Role | Description |
|---|---|---|---|
| `POST` | `/auth/register` | Public | Register a new patient or doctor account |
| `POST` | `/auth/login` | Public | Authenticate and receive a JWT token |
| `GET` | `/health` | Public | Backend health check |

**Login request:**
```json
{
  "email": "doctor1@example.com",
  "password": "Password123!"
}
```

**Login response:**
```json
{
  "token": "eyJhbGc...",
  "role": "DOCTOR",
  "id": 1,
  "name": "Dr. Meera Iyer"
}
```

---

### Consent Management

> **Role required:** `PATIENT`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/consent` | Grant a doctor access to your records (optional expiry) |
| `GET` | `/consent` | List all consent records you have issued |
| `DELETE` | `/consent/{id}` | Revoke a specific consent grant immediately |

**Grant consent request:**
```json
{
  "doctorId": 1,
  "expiresAt": "2027-01-01T00:00:00Z"
}
```

Omit `expiresAt` for a perpetual grant.

---

### Emergency Access

> **Role required:** `DOCTOR`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/emergency-access` | Log a justified emergency access for a patient |
| `GET` | `/emergency-access` | List emergency accesses logged by the authenticated doctor |

**Log emergency request:**
```json
{
  "patientId": 2,
  "justification": "Patient is unconscious in ER, no next-of-kin reachable"
}
```

---

### AI Assistant

> **Role required:** `DOCTOR` — access gated by `AccessControlService` (consent or emergency required)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/assistant/query` | Ask a natural-language question about a patient's records |

**Query request:**
```json
{
  "patientId": 1,
  "question": "Does this patient have any known drug allergies?"
}
```

**Query response:**
```json
{
  "answer": "Yes, the patient has a documented allergy to penicillin (Record R-001).",
  "sources": ["record-uuid-001"],
  "accessType": "CONSENT",
  "chunkPreviews": [
    {
      "record_id": "record-uuid-001",
      "snippet": "Allergy: Penicillin - documented anaphylactic reaction (2022-03-14)...",
      "relevance": 0.923
    }
  ]
}
```

**Error responses:**

| Status | Condition |
|---|---|
| `403 Forbidden` | No active consent and no recent emergency access |
| `404 Not Found` | Patient does not exist |
| `400 Bad Request` | Question is blank |

---

### Audit Log

> **Role required:** `PATIENT` — ownership-checked (patients see only their own log)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/audit-log` | List all AI queries made against your records |
| `GET` | `/audit-log/{id}` | Get a specific audit log entry |

**Audit log entry:**
```json
{
  "id": 42,
  "doctorName": "Dr. Meera Iyer",
  "question": "Does this patient have any known drug allergies?",
  "recordIds": ["record-uuid-001"],
  "accessType": "CONSENT",
  "grantedById": 3,
  "createdAt": "2026-09-07T10:23:11Z"
}
```

---

### Health Records

| Method | Endpoint | Role | Description |
|---|---|---|---|
| `POST` | `/records` | `PATIENT` | Upload a new health record (text content) |
| `GET` | `/records/{patientId}` | `DOCTOR` | List records for a patient (active consent required) |

---

## 🤖 RAG Pipeline Deep Dive

The AI service implements a full Retrieval-Augmented Generation pipeline. Here is exactly what happens when a doctor submits a question:

```
Doctor question
      |
      v
1. EMBED QUESTION
   fastembed encodes the question into a 384-dimensional vector
   using sentence-transformers/all-MiniLM-L6-v2 (runs locally, no API key).
      |
      v
2. VECTOR SEARCH
   ChromaDB queries the patient's collection for the top-K most
   semantically similar chunks using L2 distance (default K=4).
   get_collection(patient_id) ensures strict per-patient isolation.
      |
      v
3. CONTEXT ASSEMBLY
   Retrieved chunks are formatted as:
     "[Record <record_id>] <chunk text>"
   Multiple chunks from the same record are included as separate entries.
      |
      v
4a. GENERATION (if GEMINI_API_KEY is configured)
    Gemini (gemini-2.5-flash-lite by default) is called with:
    - System prompt: "Answer ONLY using the provided context. If the
      answer is not supported by the context, reply: not found in records."
    - User prompt: assembled context + doctor's question
    - temperature = 0.0 (deterministic, grounded answers)
      |
4b. EXTRACTIVE FALLBACK (if no GEMINI_API_KEY)
    The top-scoring chunk is returned verbatim, prefixed with
    "[extractive match, no LLM configured]". No API key required.
      |
      v
5. RESPONSE
   Returns: answer text, source record IDs, and chunk_previews
   (snippet + relevance score per record).
```

### Chunking Algorithm

The text chunker (`app/chunking.py`) uses a sliding-window approach:

| Parameter | Default | Environment Variable |
|---|---|---|
| Chunk size | 500 characters | `CHUNK_SIZE` |
| Overlap between chunks | 50 characters | `CHUNK_OVERLAP` |

**Boundary safety:** The chunker backs up to the last whitespace within the window to avoid cutting words in half, as long as doing so produces a chunk at least half the target size.

**Overlap guard:** `overlap` is clamped to `chunk_size - 1` to prevent near-duplicate consecutive chunks, which would waste vector-store space and distort retrieval ranking.

### Relevance Scoring

Chunk relevance is derived from ChromaDB's L2 distance and returned in the response:

```
relevance = 1.0 / (1.0 + l2_distance)
```

- `1.0` = perfect vector match
- `~0.0` = no semantic similarity

One preview (the most relevant chunk) is returned per unique source record.

---

## 🚀 Quick Start

### Prerequisites

| Tool | Requirement | Notes |
|---|---|---|
| Docker | Latest stable | — |
| Docker Compose | V2 (`docker compose`) | The `docker-compose` V1 CLI is not supported |
| Git | Any recent version | — |

No Java, Python, or database installation is required on the host machine.

### Setup and Run

```bash
# 1. Clone the repository
git clone https://github.com/kesavadatta2410/VaultMD.git
cd VaultMD

# 2. (Optional but recommended) Configure your environment
cp .env.example .env
# Open .env and set:
#   JWT_SECRET     - at minimum 32 random characters (required for non-local use)
#   GEMINI_API_KEY - optional; enables LLM-generated answers
#                    Get one free at https://aistudio.google.com/app/apikey

# 3. Build and start all services
docker compose up --build

# Services are ready when you see:
#   backend    | Started VaultMdApplication in X.XXX seconds
#   ai-service | Application startup complete.
```

**Background mode:**

```bash
docker compose up --build -d
docker compose logs -f   # stream logs
docker compose down       # stop containers (data volumes are preserved)
```

### Service Endpoints

| Service | URL | Notes |
|---|---|---|
| Backend API | http://localhost:8080 | Spring Boot REST API |
| AI Service | http://localhost:8000 | FastAPI (internal use) |
| AI Service Docs | http://localhost:8000/docs | Interactive Swagger UI |
| AI Health Check | http://localhost:8000/health | Shows ChromaDB mode, LLM config |
| Frontend | `frontend/index.html` | Open directly in browser — no build step |
| Postman | Import `postman_collection.json` | All flows pre-configured |

### Demo Accounts

On first startup, `DataSeeder.java` automatically seeds the database with synthetic demo data.

| Role | Name | Email | Password | Pre-seeded State |
|---|---|---|---|---|
| Patient | Asha Rao | `patient1@example.com` | `Password123!` | Active consent granted to `doctor1` — ready to query immediately |
| Patient | Ben Carter | `patient2@example.com` | `Password123!` | No consent — ideal for emergency-access demo |
| Patient | Chidi Okafor | `patient3@example.com` | `Password123!` | No consent — same as above |
| Doctor | Dr. Meera Iyer | `doctor1@example.com` | `Password123!` | Has active consent for `patient1` |
| Doctor | Dr. Sam Lee | `doctor2@example.com` | `Password123!` | No pre-granted consent |

> **No Gemini key?** The AI service runs fine without one — it returns the most relevant verbatim record chunk as the answer. Set `GEMINI_API_KEY` in your `.env` file for LLM-generated, cited answers using `gemini-2.5-flash-lite` (free tier: 1000 requests/day, 15/min).

---

## 🔄 End-to-End Demo Flow

Use the **frontend** (`frontend/index.html`) or import **`postman_collection.json`** into Postman to walk through the complete access lifecycle.

### Consent Flow

1. **Login** as `doctor1@example.com` → receive JWT
2. **Query** the AI assistant for `patient1` → ✅ **Succeeds** (active consent already seeded)
   - Response includes the answer, source record IDs, and chunk previews with relevance scores
3. Login as `patient1@example.com`, **view audit log** → see the question `doctor1` asked, which records were cited, and that `accessType = CONSENT`

### Consent Revocation

4. As `patient1`, **revoke** `doctor1`'s consent (`DELETE /consent/{id}`)
5. Login as `doctor1`, query for `patient1` → ❌ **403 Access Denied** (consent revoked)

### Granting New Consent

6. Login as `patient2`, **grant consent** to `doctor1` (`POST /consent`)
7. Login as `doctor1`, query for `patient2` → ✅ **Succeeds** (newly granted consent)

### Emergency Access Flow

8. As `patient2`, **revoke** `doctor1`'s consent
9. Login as `doctor1`, query for `patient2` → ❌ **403 Access Denied**
10. **Log an emergency access** for `patient2` with a written justification (`POST /emergency-access`)
11. Query for `patient2` → ✅ **Succeeds** via the emergency authorization path
12. Login as `patient2`, view audit log → see `accessType = EMERGENCY` with the specific emergency access row ID

### Consent Expiry

13. As a patient, grant consent with `expiresAt` set to a past timestamp
14. As the doctor, attempt to query → ❌ **403 Access Denied** (expired consent is treated as no consent)

---

## 🧪 Running Tests

### Backend (Java / JUnit 5)

```bash
cd backend
mvn test
```

Executes `AccessControlServiceTest` — **pure unit tests** with no database or network required (all dependencies mocked with Mockito). Tests cover every branch of the core security rule:

| Test Case | Expected Result |
|---|---|
| Active, non-expired consent exists | ✅ Grant issued (type: CONSENT) |
| Consent status is REVOKED | ❌ `ForbiddenApiException` thrown |
| Consent `expiresAt` is in the past | ❌ `ForbiddenApiException` thrown |
| No consent; emergency logged within validity window | ✅ Grant issued (type: EMERGENCY) |
| No consent; emergency logged outside validity window | ❌ `ForbiddenApiException` thrown |
| Neither consent nor emergency exists | ❌ `ForbiddenApiException` thrown |

### AI Service (Python / pytest)

```bash
cd ai-service
pip install -r requirements.txt pytest
pytest tests/ -v
```

Tests the document chunker (`app/chunking.py`) in isolation — **no model download or live Chroma instance required**. Covers edge cases including empty input, text shorter than chunk size, overlap clamping, and whitespace-safe boundary detection.

### CI (GitHub Actions)

Every push triggers `.github/workflows/ci.yml`, which runs both test suites. PRs are not merged unless CI passes.

---

## ⚙️ Configuration Reference

All variables are documented in [`.env.example`](./.env.example). Copy it to `.env` before running.

### Backend

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | *(dev default in compose)* | **Required.** HMAC-SHA256 signing secret. Must be ≥ 32 characters. Always override outside local dev. |
| `JWT_EXPIRATION_MS` | `28800000` | JWT lifetime in milliseconds. Default is 8 hours. |
| `EMERGENCY_ACCESS_VALIDITY_HOURS` | `12` | How long (hours) a logged emergency access counts as a valid grant. |
| `CORS_ALLOWED_ORIGINS` | `*` | Allowed CORS origins. Restrict to your frontend domain in production. |
| `DB_HOST` | `postgres` | PostgreSQL host (Docker Compose service name). |
| `DB_PORT` | `5432` | PostgreSQL port. |
| `DB_NAME` | `vaultmd` | Database name. |
| `DB_USER` | `vaultmd` | Database user. |
| `DB_PASSWORD` | `vaultmd` | Database password. **Change in production.** |
| `AI_SERVICE_URL` | `http://ai-service:8000` | Internal URL for the AI service. |

### AI Service

| Variable | Default | Description |
|---|---|---|
| `GEMINI_API_KEY` | *(not set)* | Optional. Google Gemini API key. Without this, the service uses extractive fallback. Get one free at [aistudio.google.com](https://aistudio.google.com/app/apikey). |
| `GEMINI_MODEL` | `gemini-2.5-flash-lite` | Gemini model. The default has the most generous free-tier quota (1000 req/day, 15/min). |
| `EMBEDDING_MODEL_NAME` | `sentence-transformers/all-MiniLM-L6-v2` | fastembed model. Runs locally — no API key needed. |
| `CHROMA_PERSIST_DIR` | `./chroma_data` | ChromaDB persistence directory. Set to `""` for in-memory mode (free-tier hosting without persistent disk). |
| `TOP_K` | `4` | Number of vector-store chunks retrieved per query. Higher values give more context but slower responses. |
| `CHUNK_SIZE` | `500` | Maximum character size of each text chunk during ingestion. |
| `CHUNK_OVERLAP` | `50` | Character overlap between consecutive chunks so facts near boundaries are not lost. |

---

## 🚢 Deployment

VaultMD is designed to run on any Docker-capable platform.

### Render (recommended for quick demos)

A `render.yaml` is included for deployment to [Render](https://render.com). On Render's free tier:
- The backend uses the `h2` Spring profile (in-memory H2 database — data resets on restart)
- ChromaDB runs in in-memory mode (`CHROMA_PERSIST_DIR=""`)

### Production Checklist

Before deploying to a real environment:

- [ ] Set a strong, random `JWT_SECRET` (≥ 32 characters, never commit to source control)
- [ ] Set a strong `DB_PASSWORD`
- [ ] Restrict `CORS_ALLOWED_ORIGINS` to your specific frontend domain
- [ ] Use a managed, persistent PostgreSQL instance
- [ ] Mount a persistent volume for ChromaDB or use a managed vector database
- [ ] Configure `JWT_EXPIRATION_MS` to match your security requirements
- [ ] Set up a BAA-covered infrastructure provider if handling real patient data
- [ ] Address all items in the Known Limitations table below

---

## 🗺️ Known Limitations and Roadmap

| Limitation | Detail | Proposed Path Forward |
|---|---|---|
| **Voice input** | No audio ingestion or transcription | Add a `POST /transcribe` endpoint (Whisper) in `ai-service`; add a record button in the doctor UI |
| **Fine-grained consent scope** | Consent is all-or-nothing; a consenting doctor can query any record type | Add a `scope` column on `Consent` (e.g. `LABS`, `MEDICATIONS`); filter chunks by `record.type` at query time |
| **Production encryption / HIPAA** | `health_records` (DB) and `chroma_data` (volume) are not encrypted at rest | Encrypt at rest using platform-level disk encryption; use BAA-covered infrastructure for real patient data |
| **Refresh tokens and rate limiting** | JWTs are long-lived (default 8h) with no rotation; no IP-level rate limiting on login | Short-lived access tokens + refresh token rotation; per-IP rate limiting on `POST /auth/login` |
| **Patient notification on emergency access** | Patients are not notified in real time when emergency access is logged | Hook into `EmergencyAccessService.logAccess` to fire email/SMS/push notification |
| **JWT stored in `localStorage`** | Known XSS trade-off; `localStorage` is accessible to any JS on the page | Migrate to `httpOnly`, `SameSite=Strict` cookies for production |
| **No record versioning** | Health records are immutable once created (no edit/replace flow) | Add versioned records with a `supersededById` foreign key |
| **Single-region deployment** | No HA or multi-region setup | Use managed PostgreSQL with read replicas; managed vector database for ChromaDB |

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](./CONTRIBUTING.md) first. It covers:

- **Local setup** — Docker-based, no extra installs needed
- **Branching strategy** — `main` is protected; branch as `feature/`, `fix/`, `chore/`
- **Commit conventions** — [Conventional Commits](https://www.conventionalcommits.org/): `feat(scope): description`
- **Pull request guidelines** — one concern per PR, tests required, CI must pass
- **Code standards** — Java, Python, and frontend guidelines
- **Security notes** — including the critical rule: *never weaken the `AccessGrant → AiServiceClient.query()` invariant*

### Security Vulnerabilities

Please **do not** open a public GitHub issue for security vulnerabilities. Instead, use GitHub's **private Security Advisory** feature so the issue can be triaged and patched before public disclosure.

---

## 📄 License

Distributed under the [MIT License](./LICENSE). © 2026 Kesava Datta
