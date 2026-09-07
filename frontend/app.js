// ASSUMPTION: single-origin-agnostic demo config. Change API_BASE if the
// backend isn't running on localhost:8080 (e.g. when served via
// docker-compose, the backend is still published on host port 8080).
const API_BASE = "http://localhost:8080";

// ASSUMPTION (documented in README): the JWT is kept in localStorage for
// demo simplicity. That's fine for a local demo but is an XSS-exposure
// trade-off versus an httpOnly cookie - not something to carry into a real
// deployment. See README "what was cut for time".
const SESSION_KEY = "vaultmd_session";

function getSession() {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
}

function setSession(session) {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

function clearSession() {
    localStorage.removeItem(SESSION_KEY);
}

async function apiFetch(path, { method = "GET", body, auth = true } = {}) {
    const headers = { "Content-Type": "application/json" };
    const session = getSession();
    if (auth && session?.token) {
        headers["Authorization"] = `Bearer ${session.token}`;
    }

    const res = await fetch(`${API_BASE}${path}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
    });

    let data = null;
    try {
        data = await res.json();
    } catch (_) {
        // no body (e.g. 204 No Content)
    }

    if (!res.ok) {
        const message = data?.error || `Request failed (${res.status})`;
        const err = new Error(message);
        err.status = res.status;
        err.fieldErrors = data?.fieldErrors;
        throw err;
    }
    return data;
}

function showError(message) {
    const banner = document.getElementById("error-banner");
    banner.textContent = message;
    banner.classList.remove("hidden");
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function clearError() {
    document.getElementById("error-banner").classList.add("hidden");
}

// ===================== VIEW SWITCHING =====================

function renderView() {
    const session = getSession();
    const authSection = document.getElementById("auth-section");
    const patientSection = document.getElementById("patient-section");
    const doctorSection = document.getElementById("doctor-section");
    const sessionBar = document.getElementById("session-bar");
    const sessionInfo = document.getElementById("session-info");

    if (!session) {
        authSection.classList.remove("hidden");
        patientSection.classList.add("hidden");
        doctorSection.classList.add("hidden");
        sessionBar.classList.add("hidden");
        return;
    }

    authSection.classList.add("hidden");
    sessionBar.classList.remove("hidden");
    sessionInfo.textContent = `${session.fullName} (${session.role}, id ${session.userId})`;

    if (session.role === "PATIENT") {
        patientSection.classList.remove("hidden");
        doctorSection.classList.add("hidden");
        loadConsents();
        loadAuditLog();
    } else {
        doctorSection.classList.remove("hidden");
        patientSection.classList.add("hidden");
    }
}

// ===================== AUTH =====================

document.querySelectorAll(".tab-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".tab-btn").forEach((b) => b.classList.remove("active"));
        document.querySelectorAll(".tab-panel").forEach((p) => p.classList.add("hidden"));
        btn.classList.add("active");
        document.getElementById(`${btn.dataset.tab}-form`).classList.remove("hidden");
    });
});

document.getElementById("login-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    clearError();
    const form = new FormData(e.target);
    try {
        const auth = await apiFetch("/auth/login", {
            method: "POST",
            auth: false,
            body: {
                role: form.get("role"),
                email: form.get("email"),
                password: form.get("password"),
            },
        });
        setSession({ token: auth.token, userId: auth.userId, fullName: auth.fullName, role: auth.role });
        e.target.reset();
        renderView();
    } catch (err) {
        showError(err.message);
    }
});

document.querySelector('#signup-form select[name="role"]').addEventListener("change", (e) => {
    document.getElementById("license-field").classList.toggle("hidden", e.target.value !== "DOCTOR");
});

document.getElementById("signup-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    clearError();
    const form = new FormData(e.target);
    try {
        const auth = await apiFetch("/auth/signup", {
            method: "POST",
            auth: false,
            body: {
                role: form.get("role"),
                fullName: form.get("fullName"),
                email: form.get("email"),
                password: form.get("password"),
                licenseId: form.get("licenseId") || undefined,
            },
        });
        setSession({ token: auth.token, userId: auth.userId, fullName: auth.fullName, role: auth.role });
        e.target.reset();
        renderView();
    } catch (err) {
        showError(err.message);
    }
});

document.getElementById("logout-btn").addEventListener("click", () => {
    clearSession();
    renderView();
});

// ===================== PATIENT: UPLOAD =====================

document.getElementById("upload-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    clearError();
    const form = new FormData(e.target);
    const statusList = document.getElementById("upload-status");
    try {
        const record = await apiFetch("/records", {
            method: "POST",
            body: { title: form.get("title"), type: form.get("type"), text: form.get("text") },
        });
        const li = document.createElement("li");
        li.className = record.ingested ? "ok" : "fail";
        li.textContent = record.ingested
            ? `Uploaded "${record.title}" (id ${record.id}) - indexed and queryable.`
            : `Uploaded "${record.title}" (id ${record.id}) - saved, but the AI service could not index it yet.`;
        statusList.prepend(li);
        e.target.reset();
    } catch (err) {
        showError(err.message);
    }
});

// ===================== PATIENT: CONSENT =====================

document.getElementById("consent-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    clearError();
    const form = new FormData(e.target);
    const expiresAtLocal = form.get("expiresAt");
    try {
        await apiFetch("/consent", {
            method: "POST",
            body: {
                doctorId: Number(form.get("doctorId")),
                expiresAt: expiresAtLocal ? new Date(expiresAtLocal).toISOString() : null,
            },
        });
        e.target.reset();
        loadConsents();
    } catch (err) {
        showError(err.message);
    }
});

async function loadConsents() {
    try {
        const consents = await apiFetch("/consent");
        const tbody = document.querySelector("#consent-table tbody");
        tbody.innerHTML = "";
        consents.forEach((c) => {
            const tr = document.createElement("tr");

            const doctorTd = document.createElement("td");
            doctorTd.textContent = `${c.doctorName} (id ${c.doctorId})`;

            const statusTd = document.createElement("td");
            statusTd.textContent = c.status;

            const expiresTd = document.createElement("td");
            expiresTd.textContent = c.expiresAt ? new Date(c.expiresAt).toLocaleString() : "never";

            const actionTd = document.createElement("td");
            if (c.status === "ACTIVE") {
                const revokeBtn = document.createElement("button");
                revokeBtn.className = "link";
                revokeBtn.textContent = "Revoke";
                revokeBtn.addEventListener("click", async () => {
                    try {
                        await apiFetch(`/consent/${c.id}`, { method: "DELETE" });
                        loadConsents();
                    } catch (err) {
                        showError(err.message);
                    }
                });
                actionTd.appendChild(revokeBtn);
            }

            tr.append(doctorTd, statusTd, expiresTd, actionTd);
            tbody.appendChild(tr);
        });
    } catch (err) {
        showError(err.message);
    }
}

// ===================== PATIENT: AUDIT LOG =====================

document.getElementById("refresh-audit-btn").addEventListener("click", loadAuditLog);

async function loadAuditLog() {
    const session = getSession();
    if (!session) return;
    try {
        const entries = await apiFetch(`/audit-log/${session.userId}`);
        const tbody = document.querySelector("#audit-table tbody");
        tbody.innerHTML = "";
        entries.forEach((entry) => {
            const tr = document.createElement("tr");
            [
                `${entry.doctorName} (id ${entry.doctorId})`,
                entry.accessType,
                entry.question,
                entry.citedRecordIds || "-",
                new Date(entry.accessedAt).toLocaleString(),
            ].forEach((text) => {
                const td = document.createElement("td");
                td.textContent = text;
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });
    } catch (err) {
        showError(err.message);
    }
}

// ===================== DOCTOR: ASSISTANT QUERY =====================

document.getElementById("query-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    clearError();
    const form = new FormData(e.target);
    const resultBox = document.getElementById("query-result");
    const deniedBox = document.getElementById("query-denied");
    resultBox.classList.add("hidden");
    deniedBox.classList.add("hidden");

    try {
        const response = await apiFetch("/assistant/query", {
            method: "POST",
            body: { patientId: Number(form.get("patientId")), question: form.get("question") },
        });
        document.getElementById("query-answer").textContent = response.answer;
        document.getElementById("query-sources").textContent =
            response.sources.length ? response.sources.join(", ") : "none";
        document.getElementById("query-access-type").textContent = response.accessType;
        resultBox.classList.remove("hidden");
    } catch (err) {
        if (err.status === 403) {
            deniedBox.classList.remove("hidden");
        } else {
            showError(err.message);
        }
    }
});

// ===================== DOCTOR: EMERGENCY ACCESS =====================

document.getElementById("emergency-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    clearError();
    const form = new FormData(e.target);
    const statusList = document.getElementById("emergency-status");
    try {
        const result = await apiFetch("/emergency-access", {
            method: "POST",
            body: { patientId: Number(form.get("patientId")), justification: form.get("justification") },
        });
        const li = document.createElement("li");
        li.className = "ok";
        li.textContent = `Emergency access logged for patient ${result.patientId}. ` +
            `Valid for ${result.validForHours}h - you can now query the assistant for this patient.`;
        statusList.prepend(li);
        e.target.reset();
    } catch (err) {
        showError(err.message);
    }
});

// ===================== INIT =====================

renderView();
