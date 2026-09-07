---
project: VaultMD
assessed_at: 2026-09-07T15:30:00Z
agent_readiness: ready-with-compensation
context_type: brownfield
stack_components:
  topology: polyglot monorepo (3 components)
  backend:
    language: Java 17
    framework: Spring Boot 3.2.5
    build_tool: Maven
    test_runner: JUnit 5 (spring-boot-starter-test)
    package_manager: Maven
  ai_service:
    language: Python 3.11
    framework: FastAPI
    build_tool: pip (requirements.txt, no lockfile)
    test_runner: pytest
    package_manager: pip
  frontend:
    language: JavaScript (no TypeScript)
    framework: none (deliberate no-build-step vanilla JS)
    build_tool: none
    test_runner: none
    package_manager: none
  ci_provider: GitHub Actions (.github/workflows/ci.yml)
  deployment_target: Render (Docker web services, backend + ai-service) + Hugging Face Static Space (frontend)
gates_passed: 5
gates_failed: 2
gates_partial: 2
---

## Stack Components

VaultMD is not a single-stack project — it's a polyglot monorepo with three independently deployed components, each with its own stack:

- **backend/** — Java 17, Spring Boot 3.2.5 (Web, Security, Data JPA, Validation), Maven, JUnit 5. Handles auth, consent, access control, and the audit log.
- **ai-service/** — Python 3.11, FastAPI, ChromaDB, fastembed, google-genai, pip (requirements.txt, no lockfile), pytest. Handles chunking, embeddings, retrieval, and Gemini-backed generation.
- **frontend/** — plain HTML/CSS/JavaScript with no framework and no build step, by explicit design choice (documented in the project README as a scope trade-off for a small demo UI).

No root-level project marker exists because there is no single top-level stack — each component was assessed on its own terms below.

## Quality Gate Assessment

### backend (Java 17 / Spring Boot)

| Component  | Typed | Convention | Training Data | Documented | Verdict |
|------------|-------|------------|----------------|------------|---------|
| Language   | ✓     | —          | —              | —          | pass    |
| Framework  | —     | ✓          | ✓              | ✓          | pass    |
| Build tool | —     | ✓          | ✓              | ✓          | pass    |
| Test runner| —     | —          | ✓              | ✓          | pass    |

### ai-service (Python 3.11 / FastAPI)

| Component  | Typed | Convention | Training Data | Documented | Verdict |
|------------|-------|------------|----------------|------------|---------|
| Language   | ✗     | —          | —              | —          | fail    |
| Framework  | —     | ~          | ✓              | ✓          | partial |
| Build tool | —     | ✗          | ✓              | ✓          | partial |
| Test runner| —     | —          | ✓              | ✓          | pass    |

### frontend (vanilla JavaScript)

| Component  | Typed | Convention | Training Data | Documented | Verdict |
|------------|-------|------------|----------------|------------|---------|
| Language   | ✗     | —          | —              | —          | fail    |
| Framework  | —     | —          | —              | —          | n/a (no framework, by design) |

Legend: ✓ = pass, ✗ = fail, ~ = partial, — = not applicable

### Gate Details

**backend — all gates pass, no compensation needed.**
- Typed: Java is statically typed by default.
- Convention: Spring Boot has strong, well-known opinions on layering, and this codebase follows them exactly — `controller/`, `service/`, `repository/`, `dto/`, `model/`, `security/`, `config/`, `exception/` packages, one class per concern.
- Training data: Spring Boot is the dominant Java web framework; extremely well represented.
- Documented: spring.io's official docs are thorough, versioned, and current for 3.2.x.

**ai-service — language fails typed, framework and build tool partially fail.**
- Typed (fail): no `pyproject.toml`, `mypy.ini`, or `[tool.mypy]`/`[tool.pyright]` section exists anywhere in `ai-service/`. Nothing enforces type-checking. Partial mitigation already in place: Pydantic models (`schemas.py`) do enforce types at the FastAPI request/response boundary, which is real but doesn't cover internal function signatures.
- Convention (partial): FastAPI itself is less opinionated than Django about folder layout, but this project *has* a clean, consistent one in practice (`app/main.py`, `config.py`, `schemas.py`, `vectorstore.py`, `embeddings.py`, `llm.py`, `chunking.py` — one file per concern). The gap is that this convention exists only informally; there's no CLAUDE.md/AGENTS.md documenting it, so an agent has to infer it by reading the whole directory rather than being told upfront.
- Build tool (partial): plain `pip install -r requirements.txt` with top-level versions pinned (e.g., `chromadb==0.4.24`) but no lockfile, so transitive dependencies aren't pinned. This is not hypothetical risk — it's exactly what broke the Render deployment during this project's hosting work: nothing pinned `numpy`, pip resolved NumPy 2.0, and `chromadb==0.4.24`'s use of `np.float_` (removed in NumPy 2.0) crashed the service on boot. `numpy<2.0.0` was added as a point fix, but the underlying reproducibility gap (no lockfile) remains for any other transitive dependency.
- Training data / documented (pass): FastAPI is a top-tier Python framework with excellent, current official docs.

**frontend — language fails typed; no framework to score.**
- Typed (fail): plain JavaScript, no TypeScript, no JSDoc type annotations.
- No framework gate applies — there is no framework, deliberately (see README: "no build step" is a stated design choice for this project's scope, not an oversight). At its current size (~340 lines in `app.js`, 3 files total) this is a low-severity gap. It would become a real friction point if the frontend grew significantly beyond its current scope.

## Gaps & Compensation

### Python type-checking (ai-service)

**Why it matters for agent workflows:** without type hints or a checker, an agent has to read implementation bodies (not just signatures) to know what a function accepts and returns, and can introduce type mismatches (e.g., passing a `str` where a `list[str]` is expected) that only surface at runtime.

**Compensation — add to CLAUDE.md/AGENTS.md:**
```markdown
## Python type discipline (ai-service)
ai-service has no mypy/pyright configured. Until it does:
- Every new or edited function in ai-service/app/ must have full type
  annotations on parameters and return values (this codebase already does
  this consistently — keep it that way).
- Validate all external boundaries (HTTP request bodies, ai-service <->
  backend payloads) with Pydantic models in schemas.py, never with raw
  dicts.
- Before relying on a function's behavior, read its full body, not just
  its signature — there is no static checker to catch a mismatch.
```

### Undocumented ai-service module layout

**Why it matters:** the convention is good, but an agent (or a new contributor) currently has to reverse-engineer it from the file tree instead of being told directly.

**Compensation — add to CLAUDE.md/AGENTS.md:**
```markdown
## ai-service module layout
One file per concern, no subpackages:
- main.py       — FastAPI routes only (/health, /ingest, /query). No
                   business logic beyond wiring the other modules together.
- config.py     — Settings, loaded from env vars / .env. Add new config
                   here, never read os.getenv() elsewhere.
- schemas.py    — All Pydantic request/response models.
- chunking.py   — Text -> chunks.
- embeddings.py — Chunks -> vectors (fastembed, lazily loaded + cached).
- vectorstore.py— Chroma client + per-patient collection access. This is
                   the ONLY module that constructs a Chroma collection -
                   the patient_id-scoped collection boundary IS the
                   patient isolation boundary, so never bypass it.
- llm.py        — Gemini call + extractive fallback + chunk preview
                   building.
New functionality should extend one of these, not introduce a new
subpackage, unless a module genuinely outgrows a single file.
```

### No dependency lockfile (ai-service)

**Why it matters:** already caused a real production incident this session — an unpinned transitive dependency (`numpy`) broke the deployed service.

**Compensation — add to CLAUDE.md/AGENTS.md:**
```markdown
## ai-service dependency changes
requirements.txt has no lockfile, so transitive dependencies are NOT
pinned - only what's listed explicitly. When adding or upgrading a
dependency:
- Pin the exact version you tested against (`package==X.Y.Z`), not a
  range.
- If a dependency is known to have breaking transitive requirements
  (e.g., a specific numpy major version), pin that transitive dependency
  explicitly too, with a comment explaining why (see the `numpy<2.0.0`
  line and its comment for the pattern to follow).
- After any dependency change, a fresh `pip install -r requirements.txt`
  in a clean environment is the only way to know it still resolves
  consistently - don't assume the currently-installed environment
  reflects what a fresh deploy will get.
```

### Frontend has no type safety or framework conventions

**Why it matters:** low severity today given the frontend's small, deliberate scope, but worth naming so it isn't mistaken for an oversight, and so it's revisited if the frontend grows.

**Compensation — add to CLAUDE.md/AGENTS.md (optional, low priority):**
```markdown
## Frontend conventions (frontend/)
No framework, no build step, by design (see README). Until that changes:
- Keep app.js organized by the existing section comments
  (`// ===================== AUTH =====================`, etc.) - add
  new features as a new section, not scattered across existing ones.
- API_BASE in app.js is the only place that knows how to reach the
  backend across environments (local file-opening vs. the deployed HF
  Space) - never hardcode a backend URL anywhere else.
```

## Summary

VaultMD's stack is **ready-with-compensation**: the Java/Spring Boot backend is fully agent-friendly out of the box (all gates pass), while the Python ai-service and vanilla-JS frontend have real but well-understood and cheaply-compensated gaps — none of them call for a stack change. The most consequential gap isn't hypothetical: the missing dependency lockfile in ai-service already caused a live incident during this project's own deployment (an unpinned NumPy 2.0 upgrade broke ChromaDB on boot), which is exactly the kind of risk this assessment exists to surface. The project currently has no CLAUDE.md/AGENTS.md at all — adopting the four compensation blocks above as the start of one would close most of the gap at low cost.

**Recommended next step:** `/10x-health-check`, which can now read this file to focus its checks on the gaps identified here (Python type/dependency hygiene, ai-service test coverage beyond chunking).
