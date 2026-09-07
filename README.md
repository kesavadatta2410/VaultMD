# VaultMD

[![CI](https://github.com/kesavadatta2410/VaultMD/actions/workflows/ci.yml/badge.svg)](https://github.com/kesavadatta2410/VaultMD/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](./LICENSE)

**VaultMD** is a privacy-first health record platform that lets patients store their medical records and control exactly which doctors can query them using an AI assistant. Every access — whether by consent or in an emergency — is fully audited and visible to the patient.

> ⚠️ **Demo project** — all data is synthetic. No real patient data is used or stored anywhere in this repository.

---

## Features

| Feature | Description |
|---------|-------------|
| 🔐 **Role-based Auth** | Patients and doctors have separate accounts with JWT-secured APIs |
| 📋 **Consent Management** | Patients grant or revoke doctor access with optional expiry |
| 🚨 **Emergency Access** | Doctors can access records without prior consent in emergencies; creates an auditable trail |
| 🤖 **RAG AI Assistant** | Doctors can ask natural-language questions about a patient's records — only if authorized |
| 📜 **Audit Log** | Every AI query is logged with the question asked, records cited, and which authorization mechanism was used |
| 🧩 **Extractive Fallback** | Works without an OpenAI key — returns the most relevant chunk verbatim |

---

## Architecture

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

### Why two services?

- **Backend (Java)** — where correctness matters most. Spring Security's request-level authorization rules make it structurally hard to expose an endpoint without the correct role check. JPA gives transactional, relational guarantees for consent and audit records.
- **AI Service (Python)** — where the ML ecosystem matters most. Embeddings, chunking, and ChromaDB are Python-first. Isolation also means ML dependencies don't bloat or destabilize the JVM service, and either side can be replaced independently.

---

## The Core Security Rule

**A doctor's question only reaches the AI service if, at query time:**
- (a) an `ACTIVE`, non-expired `Consent` row exists for that (doctor, patient) pair, **OR**
- (b) the doctor logged an `EmergencyAccess` for that patient within the last `N` hours (default: 12)

**Enforced by the compiler, not just convention:**

`AccessControlService.authorize()` is the *only* place that constructs an `AccessGrant`. `AiServiceClient.query()` — the *only* method that calls the AI `/query` endpoint — requires an `AccessGrant` parameter. This makes it a **compile error** to reach the AI without passing the consent/emergency check.

Every audit log entry records *which* mechanism authorized the access (CONSENT or EMERGENCY) and the specific row ID — so patients always see not just *who* looked, but *why they were allowed to*.

---

## Quick Start

### Requirements

- Docker and Docker Compose V2 (`docker compose`)
- No other local dependencies required

### Run

```bash
# 1. Clone
git clone https://github.com/kesavadatta2410/VaultMD.git
cd VaultMD

# 2. (Optional) Configure environment
cp .env.example .env
# Edit .env — at minimum, set a real JWT_SECRET for non-local use

# 3. Start everything
docker compose up --build
```

| Service | URL |
|---------|-----|
| Backend API | http://localhost:8080 |
| AI Service | http://localhost:8000 |
| Frontend | Open `frontend/index.html` in a browser |

On the first backend startup, demo data is seeded automatically. Summary:

| Account | Email | Password |
|---------|-------|----------|
| Patient — Asha Rao | `patient1@example.com` | `Password123!` |
| Patient — Ben Carter | `patient2@example.com` | `Password123!` |
| Patient — Chidi Okafor | `patient3@example.com` | `Password123!` |
| Doctor — Dr. Meera Iyer | `doctor1@example.com` | `Password123!` |
| Doctor — Dr. Sam Lee | `doctor2@example.com` | `Password123!` |

`doctor1` is pre-granted active consent for `patient1` — start the assistant flow immediately. `patient2` and `patient3` have **no** consent, making them ideal for demoing the emergency-access path.

> **No OpenAI key?** The AI service falls back to returning the most relevant chunk verbatim (extractive answer). Set `OPENAI_API_KEY` in your `.env` file to get LLM-generated answers.

---

## End-to-End Flow

Use the **frontend** (`frontend/index.html`) or import **`postman_collection.json`** into Postman:

1. **Login** as `doctor1`
2. **Query** the assistant for `patient1` → succeeds (active consent)
3. **Query** for `patient2` → 403 Access Denied (no consent)
4. Login as `patient2`, **grant consent** to doctor1
5. Login as `doctor1`, query for `patient2` → succeeds
6. Login as `patient2`, view **audit log** — see the doctor's question recorded
7. **Revoke** consent for doctor1
8. Login as `doctor1`, query for `patient2` → 403 again
9. **Log emergency access** for `patient2` with a justification
10. Query for `patient2` → succeeds via the emergency path

---

## Running Tests

### Backend

```bash
cd backend
mvn test
```

Runs `AccessControlServiceTest` — pure unit tests for the core security rule (active consent → grant; revoked/expired consent + no emergency → denied; expired consent + recent emergency → grant via emergency; neither → denied).

### AI Service

```bash
cd ai-service
pip install -r requirements.txt pytest
pytest tests/ -v
```

Tests the chunker without requiring a model download or Chroma instance.

---

## Configuration Reference

See [`.env.example`](./.env.example) for all configurable values with documentation.

Key settings:

| Variable | Default | Notes |
|----------|---------|-------|
| `JWT_SECRET` | *(required)* | Must be ≥ 32 random characters. **Always override in deployment.** |
| `EMERGENCY_ACCESS_VALIDITY_HOURS` | `12` | Window in which a logged emergency access counts as an active grant |
| `OPENAI_API_KEY` | *(not set)* | Optional — enables LLM answers instead of extractive fallback |
| `TOP_K` | `4` | Number of vector-store chunks retrieved per query |
| `CORS_ALLOWED_ORIGINS` | `*` | Restrict to your frontend domain in production |

---

## Known Limitations & Future Work

| Limitation | Path to address |
|------------|----------------|
| Voice input (Whisper) | New `/transcribe` endpoint in ai-service + "record" button in doctor UI posting audio |
| Fine-grained consent scope | Add a `scope` column on `Consent`; filter chunks by `record.type` in the query step |
| Production encryption / HIPAA | Encrypt `health_records` and `chroma_data` at rest; use BAA-covered infra |
| Refresh tokens / rate limiting | Short-lived JWTs + refresh rotation; per-IP rate limiting on `/auth/login` |
| Patient notification on emergency access | Hook in `EmergencyAccessService.logAccess` — fire email/SMS/push to patient |
| JWT in `localStorage` | Known XSS trade-off documented; replace with `httpOnly` cookie for production |

---

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for setup instructions, branching strategy, commit conventions, and PR guidelines.

---

## License

[MIT](./LICENSE) © 2026 Kesava Datta
