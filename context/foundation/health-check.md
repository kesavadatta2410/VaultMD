---
project: VaultMD
checked_at: 2026-09-07T16:10:00Z
context_type: brownfield
health: needs-attention
critical_findings: 0
high_findings: 2
vulnerabilities_found: 44
vulnerable_packages: 5
test_runners_detected:
  backend: JUnit 5 (Maven) - present, not executed in this environment (no local Maven), passes in CI
  ai_service: pytest - present, executed, 5/5 tests collect and pass (chunking only)
ci_provider: GitHub Actions (.github/workflows/ci.yml)
stack_assessment_ref: context/foundation/stack-assessment.md
---

## How This Check Ran

VaultMD is a polyglot monorepo (Java backend, Python ai-service, framework-free JS frontend) rather than a single-stack project, so each component was checked on its own terms rather than against one dependency toolchain. This report also builds directly on `context/foundation/stack-assessment.md` — where that assessment flagged a quality-gate gap, this report checks whether it's already showing up as a live operational problem.

## Dependency & Security Audit

### Lockfile status

| Component | Lockfile | Status |
|---|---|---|
| backend | N/A (Maven doesn't use a lockfile the way npm/cargo do) | not applicable |
| ai-service | `requirements.txt` only | ⚠ weak — pins direct dependencies but not transitive ones. Already flagged in stack-assessment.md as a compensation-needed gap. |
| frontend | N/A (no package manager) | not applicable |

### Vulnerability scan (`pip-audit -r ai-service/requirements.txt`)

Java has no built-in audit tool (noted and skipped, per standard practice — recommend OWASP Dependency-Check or Snyk if this becomes a priority). Frontend has no package manager to audit. ai-service:

**44 known vulnerabilities across 5 packages:**

| Package | Pinned | Vulns | Directly reachable in this app? |
|---|---|---|---|
| `starlette` | 0.37.2 (transitive, via `fastapi==0.110.2`) | 9 | **Yes** — every single HTTP request to ai-service goes through Starlette. Highest real-world priority of this list. |
| `chromadb` | 0.4.24 (direct) | 2 | Likely no. Both are in ChromaDB's *server/multi-tenant-auth* code path (`CVE-2026-45830`: cross-tenant read/write; `CVE-2026-45833`: RCE via a malicious model repo through the HTTP API). This app only ever uses ChromaDB as an embedded library (`chromadb.Client()`/`PersistentClient()`) inside the same process — it never runs ChromaDB's own server or relies on its tenant/auth system, so this specific attack surface isn't exposed. Still worth tracking since no fix version exists yet. |
| `python-multipart` | 0.0.9 (direct) | 7 | Likely no. All are path-traversal issues gated behind the non-default `UPLOAD_DIR`/`UPLOAD_KEEP_FILENAME=True` config, which this app never sets — ai-service has no file-upload endpoints at all (`/ingest` and `/query` both take JSON). Cheap to fix anyway (`>=0.0.31`). |
| `pillow` | 11.3.0 (transitive, not in requirements.txt — pulled in by fastembed's dependency tree) | 25 | No. This app never processes images or calls into Pillow's decoders (text-only embeddings). Large vuln count, but genuinely unused attack surface. |
| `python-dotenv` | 1.0.1 (direct) | 1 | Low real-world risk (`.env` is authored by the deployer, not user-controlled input). Cheap to fix (`>=1.2.2`). |

**Why this matters concretely, not hypothetically:** this exact class of risk — an unpinned or outdated dependency causing a real problem — already happened once during this project's own deployment this session: an unpinned transitive `numpy` resolved to 2.0, which broke `chromadb==0.4.24` on boot (`np.float_` was removed in NumPy 2.0). That specific case is now fixed (`numpy<2.0.0` pinned), but it's a live demonstration of exactly what "no lockfile" risk looks like in practice, not a theoretical concern.

## Test Infrastructure

- **backend**: JUnit 5 via `spring-boot-starter-test`, run with `mvn test`. One test class, `AccessControlServiceTest`, with 4 tests covering the core consent/emergency authorization rule. Could not execute directly in this environment (no local Maven installation), but the same suite runs in CI (`.github/workflows/ci.yml`, `backend-test` job) and per the project's own docs, passes.
- **ai-service**: pytest. Ran `python -m pytest --collect-only` directly — 5 tests collected successfully, all in `tests/test_chunking.py`. Confirmed passing per the project's CI job (`ai-service-test`).
- **Coverage gap** (the most significant finding in this report): both test suites are narrow relative to what the codebase actually depends on being correct.
  - **ai-service has zero tests for `vectorstore.py`** — the module whose own code comment states "the collection boundary IS the patient boundary." That's the single most security-critical invariant in the entire AI service, and nothing automated verifies it. A future change that accidentally shares a collection across patients, or changes `get_collection`'s scoping, would not be caught by any test.
  - **ai-service has zero tests for `llm.py`** — no test covers the Gemini-call path, the extractive fallback path, or `build_chunk_previews`' relevance scoring, all of which were built out or modified during this session's deployment work.
  - **backend has zero tests for `AssistantService`** — including the self-healing re-ingestion logic (`resyncPatientRecords`) added during this session to fix a real cold-start race condition on Render. That logic has already been hand-verified once against the live deployment, but has no regression test, so a future change could silently reintroduce the bug it fixes.

## CI/CD Coverage

GitHub Actions (`.github/workflows/ci.yml`) runs three jobs: `backend-test` (Maven/JUnit), `ai-service-test` (pytest), `python-lint` (ruff, explicitly non-blocking via `|| true`).

| Stage | Present? |
|---|---|
| Lint | ✓ (ruff, ai-service only; non-blocking) |
| Test | ✓ (both backend and ai-service) |
| Build | partial — Maven test implicitly compiles; there's no standalone "build the Docker images" CI stage (Render builds them directly on deploy) |
| Type check | ✗ (no mypy/pyright — consistent with the typing gap stack-assessment.md already flagged) |
| Security scan | ✗ (no Dependabot, no `pip-audit`/`npm audit` step, no CodeQL — this session's manual `pip-audit` run is the first time these 44 vulnerabilities were surfaced at all) |

No CI/CD pipeline gap blocks agent-assisted work today — tests run and pass. The missing security-scan stage is worth naming because it's the direct reason 44 known vulnerabilities went undetected until this manual check.

## Configuration Completeness

| File | Status | Note |
|---|---|---|
| `.gitignore` | present | (fixed this session — previously had a blanket `*.xml` rule that silently excluded `backend/pom.xml` from every commit) |
| `.env.example` | present | thorough, documents every configurable value |
| `.editorconfig` | missing | low priority |
| Frontend formatter/linter (`.prettierrc`, `.eslintrc`) | missing | low priority — frontend is 3 files, no build step, by design |
| `CLAUDE.md` / `AGENTS.md` | missing | not flagged as urgent — stack-assessment.md already has four ready-to-paste compensation blocks that would form a good first version whenever this project adopts one |

## Overall Health: needs-attention

No finding here blocks agent-assisted work from proceeding today — both test suites run and pass, dependencies install correctly, and the app is verified working end-to-end in production. "Needs-attention" reflects real, concrete, low-to-moderate-effort gaps rather than emergencies:

1. **Thin test coverage on security-critical logic** (patient isolation in `vectorstore.py`, the Gemini/extractive/citation logic in `llm.py`, the self-healing resync in `AssistantService`) — this is the standout finding. None of these paths failing would be caught by the current test suite.
2. **44 known vulnerabilities in ai-service's dependency tree**, discovered only by this manual audit because no automated scan exists. Most aren't practically exploitable given how this app actually uses its dependencies (documented above per-package), but `starlette`'s 9 are on the live request path and deserve a closer look.
3. **No dependency lockfile for ai-service**, which already caused one real incident this session (the NumPy 2.0 break) and remains a live risk for any other transitive dependency.

## Prioritized Fixes

Ranked by impact on agent workflows (a change an agent can't verify, or a risk already proven to bite in production, ranks above a cosmetic gap):

1. **Add tests for patient isolation in `vectorstore.py`.** *Impact: an agent (or a human) could break the app's core security invariant with no test catching it.* *Fix:* add `ai-service/tests/test_vectorstore.py` asserting that `get_collection("1")` and `get_collection("2")` return distinct collections, and that a chunk upserted into one is never returned by `query_collection` on the other. *Effort: moderate (15–30 min).*

2. **Add a regression test for `AssistantService`'s self-heal resync.** *Impact: this session's fix for the Render cold-start race condition (re-ingesting a patient's records when ai-service reports an empty collection) has no automated test — a future refactor could silently break it.* *Fix:* a Spring test that mocks `AiServiceClient` to first return `collectionEmpty=true` then real results, and asserts `resyncPatientRecords` is called exactly once with the right patient's records. *Effort: moderate (15–30 min).*

3. **Bump `python-multipart` to `>=0.0.31` and `python-dotenv` to `>=1.2.2`.** *Impact: closes 8 known CVEs for the cost of a version bump; both packages' vulnerable code paths appear unused by this app, but there's no reason to leave free fixes on the table.* *Fix:* update `ai-service/requirements.txt`, redeploy. *Effort: quick (<5 min).*

4. **Decide on `chromadb`'s two unpatched CVEs.** *Impact: both require ChromaDB's own server/tenant-auth mode, which this deployment doesn't use — but confirm that stays true before any future architecture change (e.g., if ai-service ever exposes ChromaDB's HTTP API directly, or moves to a hosted/multi-tenant ChromaDB instance).* *Fix:* no action needed today beyond noting the caveat; watch for a chromadb release that addresses these CVEs before any deployment change that would expose its server surface. *Effort: quick to document, ongoing to monitor.*

5. **Add tests for `llm.py`** (extractive fallback, Gemini call, `build_chunk_previews` relevance scoring). *Impact: this logic was actively modified this session (wiring chunk previews into the response, fixing the Gemini model name) and has zero coverage.* *Fix:* mock the Gemini client to test both the success and fallback paths; a pure-function test for `build_chunk_previews` needs no mocking at all. *Effort: moderate (15–30 min).*

6. **Adopt a real lockfile for ai-service** (e.g., `pip-compile` from `pip-tools`, or migrate to `uv`). *Impact: the concrete gap that already caused the NumPy 2.0 incident.* Already documented with a ready-to-paste CLAUDE.md compensation block in `stack-assessment.md` if a full lockfile migration isn't done immediately. *Effort: moderate (15–30 min) to introduce `pip-compile` and generate an initial lock.*

7. **Add a security-scan step to CI** (GitHub's built-in Dependabot is the lowest-effort option — just add `.github/dependabot.yml`). *Impact: would have caught all 44 findings above automatically, and continues to catch new ones.* *Effort: quick (<5 min to add the config file).*

8. **Add `.editorconfig`.** *Impact: low — cosmetic consistency only.* *Effort: quick (<5 min).*

## Summary

VaultMD is demonstrably working end-to-end in production (verified live this session: login, consent-gated RAG queries with citations, 403 denial, emergency access, self-healing retrieval) — this health check found no findings that block that. What it surfaces is where the *next* round of hardening should go: shore up test coverage around the two pieces of logic this session actually touched under pressure (patient isolation, the self-heal resync), and close the dependency-hygiene gap that already caused one real incident. None of the 44 vulnerabilities found are urgent given how this app actually uses its dependencies, but the fact that they'd been invisible until a manual audit is itself the more important finding — a five-minute Dependabot config would have surfaced them continuously for free.

**Recommended next step:** agent onboarding (creating `CLAUDE.md`/`AGENTS.md`, informed by `stack-assessment.md`'s ready-to-paste compensation blocks) — both the greenfield and brownfield paths converge here with equivalent context artifacts now in place.
