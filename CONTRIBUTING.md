# Contributing to VaultMD

Thank you for your interest in contributing! This document covers how to set up the project locally, the branching strategy, coding standards, and the PR process.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Local Setup](#local-setup)
- [Running Tests](#running-tests)
- [Branching Strategy](#branching-strategy)
- [Commit Conventions](#commit-conventions)
- [Pull Request Guidelines](#pull-request-guidelines)
- [Code Standards](#code-standards)

---

## Prerequisites

| Tool | Min Version | Notes |
|------|-------------|-------|
| Docker + Docker Compose | 24+ / V2+ | Compose V2 (`docker compose`) required |
| Java JDK | 17 | For running backend tests locally without Docker |
| Maven | 3.9+ | Bundled with the build container, or install locally |
| Python | 3.11 | For running AI-service tests locally |
| Git | 2.40+ | — |

---

## Local Setup

```bash
# 1. Clone the repo
git clone https://github.com/kesavadatta2410/VaultMD.git
cd VaultMD

# 2. Create your local env file
cp .env.example .env
# Edit .env — at minimum, change DB_PASSWORD and JWT_SECRET

# 3. Start all services
docker compose up --build

# Services:
#   Backend  → http://localhost:8080
#   AI svc   → http://localhost:8000
#   Frontend → open frontend/index.html in a browser (no build step needed)
```

Demo accounts are seeded automatically on the first backend startup. See the **Running it** section in the [README](./README.md) for credentials.

---

## Running Tests

### Backend (Java / JUnit 5)

```bash
cd backend
mvn test
```

Runs `AccessControlServiceTest` — the core security-rule unit tests. These are fast, pure unit tests with no DB required.

### AI Service (Python / pytest)

```bash
cd ai-service
pip install -r requirements.txt pytest
pytest tests/
```

The chunking tests don't need the embedding model or Chroma installed — they're cheap and fast.

---

## Branching Strategy

```
main          — always deployable, protected (require PR + passing CI)
feature/<name> — new features, e.g. feature/voice-transcription
fix/<name>     — bug fixes, e.g. fix/audit-log-ownership
chore/<name>   — non-functional changes, e.g. chore/update-gitignore
```

- **Always branch off `main`.**
- **Never commit directly to `main`.** Open a PR instead.
- Keep branches short-lived — one logical change per PR.

---

## Commit Conventions

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short description>

[optional body]
[optional footer]
```

| Type | When to use |
|------|-------------|
| `feat` | New feature or behaviour |
| `fix` | Bug fix |
| `chore` | Tooling, CI, deps, docs that don't change app behaviour |
| `refactor` | Code restructure with no behaviour change |
| `test` | Adding or updating tests |
| `docs` | Documentation only |

**Examples:**

```
feat(consent): add expiry validation on grant
fix(audit-log): add patient ownership check on GET /audit-log/{id}
chore(ci): add GitHub Actions workflow for backend + ai-service tests
```

---

## Pull Request Guidelines

1. **One concern per PR** — don't mix a bug fix with a refactor.
2. **Include tests** — for every bug fix, add a test that would have caught it; for every feature, add at least one happy-path test.
3. **CI must pass** — the GitHub Actions workflow runs backend tests and Python tests on every push. PRs with a red CI are not merged.
4. **Write a clear PR description** — explain *what* changed and *why*. Include before/after behaviour for bug fixes.
5. **Self-review before requesting review** — read your own diff in the PR UI before adding reviewers.

---

## Code Standards

### Java (backend)
- Java 17+, Spring Boot 3.2 conventions.
- Constructor injection only — no field injection (`@Autowired` on fields).
- All public service methods should have a Javadoc comment explaining the access-control contract if applicable.
- Never expose stack traces or internal detail in API responses — the `GlobalExceptionHandler` already handles this; use the typed exceptions (`BadRequestApiException`, `NotFoundApiException`, etc.).

### Python (ai-service)
- Python 3.11+, type hints on all function signatures.
- `ruff` for linting (run `pip install ruff && ruff check app/ tests/`).
- Never catch `Exception` silently — always log before swallowing.
- Tests should not require a live model download (mock the embedding calls).

### Frontend
- Vanilla HTML + CSS + JS — no framework or bundler.
- Never store sensitive data beyond what's necessary (`localStorage` is a known demo trade-off, documented in README).
- Keep API calls inside `apiFetch` — don't add raw `fetch` calls.

---

## Security Notes

- **Never commit secrets.** Use `.env` (which is gitignored) — see `.env.example`.
- **Never weaken the consent/emergency gate.** The `AccessGrant` → `AiServiceClient.query()` pattern is intentional compile-time enforcement. Any new code path to the AI `/query` endpoint **must** go through `AccessControlService.authorize()`.
- For security vulnerabilities, please open a **private** GitHub Security Advisory rather than a public issue.
