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

**A privacy-first, AI-powered medical record platform with patient-controlled access and full audit transparency.**

[![CI](https://github.com/kesavadatta2410/VaultMD/actions/workflows/ci.yml/badge.svg)](https://github.com/kesavadatta2410/VaultMD/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](./LICENSE)
[![Java 17](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Python 3.11](https://img.shields.io/badge/Python-3.11-blue?logo=python)](https://www.python.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)

</div>

---

> ⚠️ **Demo project** — all data is fully synthetic. No real patient data is used or stored anywhere in this repository.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Security Model](#-security-model)
- [Quick Start](#-quick-start)
- [End-to-End Demo Flow](#-end-to-end-demo-flow)
- [Running Tests](#-running-tests)
- [Configuration Reference](#-configuration-reference)
- [Known Limitations & Roadmap](#-known-limitations--roadmap)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌐 Overview

**VaultMD** is a privacy-first health record platform that gives patients full ownership of their medical data. Patients decide which doctors can access their records, for how long, and can revoke access at any time. Authorized doctors interact with records through a natural-language AI assistant powered by Retrieval-Augmented Generation (RAG). Every access — whether granted by explicit consent or triggered by a documented emergency — is permanently logged and visible to the patient in real time.

The project demonstrates how security-critical invariants can be enforced at the **compiler level**, not just by convention — making unauthorized data access a compile error rather than a runtime risk.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔐 **Role-Based Authentication** | Patients and doctors have separate, isolated accounts secured by JWT-signed tokens |
| 📋 **Consent Management** | Patients grant or revoke doctor access with optional time-bound expiry |
| 🚨 **Emergency Access** | Doctors may access records without prior consent in emergencies; every such access is auditable |
| 🤖 **RAG AI Assistant** | Doctors query patient records in natural language — only if currently authorized |
| 📜 **Immutable Audit Log** | Every AI query is logged with the question, records cited, and the authorization mechanism used |
| 🧩 **OpenAI-Optional Fallback** | Works without an OpenAI key — returns the most relevant chunk verbatim (extractive answer) |
| 🐳 **One-Command Setup** | Full stack runs via `docker compose up --build` with no additional local dependencies |

---

## 🏗️ Architecture

```
vaultmd/
├── backend/          Spring Boot (Java 17)   — auth, consent, access control, audit
├── ai-service/       FastAPI (Python 3.11)   — chunking, embeddings, ChromaDB, RAG
├── frontend/         Vanilla HTML + JS       — patient and doctor UI (no build step)
├── .github/
│   └── workflows/ci.yml                     — GitHub Actions (backend + AI tests)
├── docker-compose.yml
├── postman_collection.json
├── .env.example
└── CONTRIBUTING.md
```

### Why a two-service design?

| Service | Rationale |
|---|---|
| **Backend (Java / Spring Boot)** | Where correctness is paramount. Spring Security's request-level authorization makes it structurally difficult to expose an endpoint without a role check. JPA provides transactional, relational guarantees for consent and audit records. |
| **AI Service (Python / FastAPI)** | Where the ML ecosystem matters most. Embeddings, chunking, and ChromaDB are Python-native. Isolation means ML dependencies do not bloat or destabilize the JVM service — and either side can be replaced independently. |

---

## 🛡️ Security Model

### The Core Access Rule

> **A doctor's query reaches the AI service only if, at query time, one of the following conditions holds:**
> - **(a)** An `ACTIVE`, non-expired `Consent` row exists for that `(doctor, patient)` pair, **OR**
> - **(b)** The doctor has logged an `EmergencyAccess` for that patient within the last `N` hours *(default: 12 hours)*

### Compiler-Enforced, Not Convention-Based

This invariant is not just a policy document — it is **structurally encoded into the type system**:

- `AccessControlService.authorize()` is the **sole** method that constructs an `AccessGrant` object.
- `AiServiceClient.query()` — the **only** method that calls the AI `/query` endpoint — requires an `AccessGrant` as a parameter.

This design makes it a **compile error** to invoke the AI service without first passing the consent/emergency authorization check. There is no way to accidentally bypass it.

### Audit Trail

Every audit log entry captures:

- **Who** queried (doctor identity)
- **What** was asked (the question)
- **Which records** were cited
- **Why access was allowed** (CONSENT or EMERGENCY, with the specific row ID)

Patients always see not just *who* accessed their data, but *why they were permitted to*.

---

## 🚀 Quick Start

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose V2 (`docker compose`)
- No other local dependencies are required

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/kesavadatta2410/VaultMD.git
cd VaultMD

# 2. (Optional) Configure your environment
cp .env.example .env
# Edit .env — at minimum, set a strong JWT_SECRET for any non-local deployment

# 3. Start the full stack
docker compose up --build
```

### Service Endpoints

| Service | URL |
|---|---|
| Backend API | http://localhost:8080 |
| AI Service | http://localhost:8000 |
| Frontend | Open `frontend/index.html` in a browser |

### Demo Accounts

On first startup, the backend automatically seeds the database with synthetic demo data.

| Role | Name | Email | Password |
|---|---|---|---|
| Patient | Asha Rao | `patient1@example.com` | `Password123!` |
| Patient | Ben Carter | `patient2@example.com` | `Password123!` |
| Patient | Chidi Okafor | `patient3@example.com` | `Password123!` |
| Doctor | Dr. Meera Iyer | `doctor1@example.com` | `Password123!` |
| Doctor | Dr. Sam Lee | `doctor2@example.com` | `Password123!` |

**Pre-seeded state:** `doctor1` has active consent for `patient1` — you can start querying the AI assistant immediately. `patient2` and `patient3` have no consent, making them ideal for demonstrating the emergency-access path.

> **No OpenAI key?** The AI service automatically falls back to returning the most relevant record chunk verbatim. Set `OPENAI_API_KEY` in your `.env` file to enable LLM-generated answers.

---

## 🔄 End-to-End Demo Flow

Use the **frontend** (`frontend/index.html`) or import **`postman_collection.json`** into Postman to walk through the full access lifecycle:

1. **Login** as `doctor1@example.com`
2. **Query** the AI assistant for `patient1` → ✅ Succeeds (active consent exists)
3. **Query** for `patient2` → ❌ 403 Access Denied (no consent on record)
4. Login as `patient2`, **grant consent** to `doctor1`
5. Login as `doctor1`, query for `patient2` → ✅ Succeeds (newly granted consent)
6. Login as `patient2`, **view audit log** — the doctor's question is recorded with full details
7. **Revoke** consent for `doctor1`
8. Login as `doctor1`, query for `patient2` → ❌ 403 Access Denied (consent revoked)
9. **Log an emergency access** for `patient2` with a written justification
10. Query for `patient2` → ✅ Succeeds via the emergency authorization path

---

## 🧪 Running Tests

### Backend (Java)

```bash
cd backend
mvn test
```

Executes `AccessControlServiceTest` — pure unit tests covering all branches of the core security rule:

- Active consent → grant ✅
- Revoked or expired consent, no emergency → denied ❌
- Expired consent + recent emergency → grant via emergency ✅
- Neither consent nor emergency → denied ❌

### AI Service (Python)

```bash
cd ai-service
pip install -r requirements.txt pytest
pytest tests/ -v
```

Tests the document chunker in isolation — no model download or live Chroma instance required.

---

## ⚙️ Configuration Reference

All configurable values are documented in [`.env.example`](./.env.example).

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | *(required)* | Must be ≥ 32 random characters. **Always override in any non-local environment.** |
| `EMERGENCY_ACCESS_VALIDITY_HOURS` | `12` | Time window (hours) in which a logged emergency access counts as an active authorization grant |
| `OPENAI_API_KEY` | *(not set)* | Optional — enables LLM-generated answers. Falls back to extractive mode if unset |
| `TOP_K` | `4` | Number of vector-store chunks retrieved per query |
| `CORS_ALLOWED_ORIGINS` | `*` | Restrict to your frontend domain before deploying to production |

---

## 🗺️ Known Limitations & Roadmap

| Limitation | Proposed Path Forward |
|---|---|
| **Voice input** | Add a `/transcribe` endpoint (Whisper) in `ai-service` + a record button in the doctor UI |
| **Fine-grained consent scope** | Add a `scope` column on the `Consent` entity; filter chunks by `record.type` at query time |
| **Production encryption / HIPAA** | Encrypt `health_records` and `chroma_data` volumes at rest; use BAA-covered infrastructure |
| **Refresh tokens & rate limiting** | Short-lived JWTs with refresh rotation; per-IP rate limiting on `/auth/login` |
| **Patient notification on emergency access** | Hook into `EmergencyAccessService.logAccess` to fire email/SMS/push notifications |
| **JWT storage** | Currently stored in `localStorage` (documented XSS trade-off); migrate to `httpOnly` cookies for production |

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for setup instructions, branching strategy, commit conventions, and pull request guidelines before submitting changes.

---

## 📄 License

Distributed under the [MIT License](./LICENSE). © 2026 Kesava Datta
