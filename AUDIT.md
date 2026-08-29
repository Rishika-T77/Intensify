# Intensify — Engineering Audit

**Scope:** Full repo review — `PRD.md`, `README.md`, entire `backend/src` (Java/Spring Boot 4.1), entire `frontend/src` (React 19/Vite), build/config files (`pom.xml`, `application.properties`, `.env*`, `package.json`, `vite.config.js`).
**Method:** Every source file was read in full (not sampled). No code was executed or compiled. Findings below are all traceable to specific files/lines quoted inline.
**Reviewer stance:** This was audited as if reviewing a colleague's PR before a production launch — the standard is "would I sign off on this," not "does it compile."

---

## 1. Executive summary

The backend is further along and better structured than most solo 3-week MVPs — the layering (controller/service/repository/entity/dto/security/integration) is clean, ownership checks are consistently applied, JWT auth is correctly wired, account deletion cascades properly, and the question bank is fully seeded to spec (45 questions, exactly matching PRD §10). Two AI providers (Gemini + OpenAI) are already implemented, which is ahead of the PRD's own schedule (a second provider was explicitly flagged P2/"not expected in 3 weeks").

However, there are **several bugs in exactly the mechanisms the PRD calls out as the product's core differentiator and biggest risk** — the evidence-verification anti-hallucination check and the FAILED/retry recovery path. There is also **zero test coverage** (PRD §24 requires unit tests, integration tests, and a golden test set — none exist), **no version control** (no `.git` anywhere in the project), and **no `docs/` folder** despite the PRD's repo structure (§26) and documentation requirements (§27) calling for one.

None of this is "bad code" in the sense of sloppy style — the code is generally readable and the intent is clear. The issues below are the kind that only surface under real use (concurrent users, a failed AI call, a page refresh at the wrong moment) — precisely what a pre-launch audit exists to catch.

**Headline numbers:**
- 40 backend Java files read, ~2,430 lines.
- 16 frontend files read, ~1,450 lines (JS/JSX).
- 1 test file exists, containing 1 test (`contextLoads`, asserts nothing).
- 0 `.git` directories found anywhere in the project.
- 45/45 seed questions present and correctly distributed (15 DSA / 15 System Design / 15 Conceptual), matching PRD.

---

## 2. Critical findings (fix before calling this MVP done)

Ordered by how directly each one undermines either correctness, data integrity, or a promise the product makes to the user.

### 2.1 The "near-verbatim" evidence-verification fallback doesn't actually search for a match — it's broken

**File:** `backend/src/main/java/com/intensify/service/AIAnalysisService.java`, `findBestWindow()` (~line 250)

```java
private String findBestWindow(String text, int windowLen) {
    if (text.length() <= windowLen) return text;
    String best = text.substring(0, windowLen);
    // Simple sliding window — for MVP a basic implementation is sufficient
    for (int i = 1; i <= text.length() - windowLen; i++) {
        best = text.substring(i, i + windowLen);
    }
    return best;
}
```

This function is supposed to find the substring of the candidate's text that best matches the AI's `evidence` string (PRD §11.7: "compute normalized Levenshtein similarity between the evidence string and the **best-matching window**"). Instead, the loop unconditionally overwrites `best` on every iteration and never compares anything — it always returns the **last** possible window (i.e., the final `windowLen` characters of the text), regardless of how well it matches. The similarity score computed against this arbitrary window is essentially meaningless.

**Why this matters more than an average bug:** evidence verification is the PRD's explicitly named anti-hallucination safeguard and its top competitive/interview talking point (§29: "the evaluation rubric, evidence-based feedback requirement, validation layer, and hallucination guard are deliberate engineering decisions you can explain and defend"). Right now, any evidence string that isn't an exact substring match falls back to a broken check — meaning some genuinely hallucinated evidence will pass (similarity computed against the wrong window happens to score ≥85% by chance) and some genuinely real near-verbatim quotes will be wrongly discarded (silently, so no one will notice from the outside). Both failure directions are bad for a feature whose entire job is trustworthiness.

**Fix sketch:** compute Levenshtein similarity (or distance) against *every* window of the given length and keep the minimum-distance / max-similarity one, e.g.:
```java
private String findBestWindow(String text, String evidence) {
    int windowLen = evidence.length();
    if (text.length() <= windowLen) return text;
    String best = text.substring(0, windowLen);
    double bestSim = levenshteinSimilarity(evidence, best);
    for (int i = 1; i <= text.length() - windowLen; i++) {
        String candidate = text.substring(i, i + windowLen);
        double sim = levenshteinSimilarity(evidence, candidate);
        if (sim > bestSim) { bestSim = sim; best = candidate; }
    }
    return best;
}
```
(O(n·windowLen²) is fine at these input sizes — explanations are capped at 2,000 chars.)

### 2.2 The AI call happens inside the same long-lived DB transaction as the HTTP request — will exhaust the connection pool and defeats the polling UX

**Files:** `PracticeSessionService.submitResponse()` (`@Transactional`) → calls `aiAnalysisService.analyzeSession(session)` (also `@Transactional`, default `REQUIRED` propagation → joins the caller's transaction, doesn't start a new one).

```java
@Transactional
public SessionSummaryResponse submitResponse(...) {
    ...
    session.setStatus(SessionStatus.ANALYZING);
    sessionRepository.save(session);
    aiAnalysisService.analyzeSession(session);   // <-- up to 30s (plus Gemini's own 3 retry attempts on 503) inside this same transaction
    return SessionSummaryResponse.from(sessionRepository.findById(sessionId).orElseThrow());
}
```

Because both methods are `@Transactional` with default propagation, the entire HTTP request — including the blocking external LLM call, which has a **hard 30-second timeout** by design (PRD §21) and can retry up to 3 times inside `GeminiProvider` on a 503 — runs inside one open database transaction. Two consequences:

1. **Connection pool exhaustion under any real concurrency.** A DB connection (HikariCP default pool size 10) is checked out for the full duration of every single submission — worst case tens of seconds. A handful of concurrent submissions will starve the pool and start timing out unrelated requests (login, question listing, everything).
2. **The polling UX the PRD specifically designed for this (§15) doesn't see intermediate state.** Because `status = ANALYZING` isn't committed until the *whole* transaction (including the AI call) finishes, a concurrent `GET /api/sessions/{id}` poll won't see `ANALYZING` — it'll see whatever was last committed, then jump straight to the final state once the giant transaction commits. The frontend's 3-second polling loop is written correctly; the backend just never actually exposes the intermediate state it's polling for.

**Fix sketch:** commit the `ANALYZING` status transition in its own short transaction, call the AI provider *outside* any open transaction, then persist the result (ANALYZED/FAILED) in a second short transaction. This is a standard "don't do slow I/O inside `@Transactional`" fix.

### 2.3 A session that ends up `FAILED` can never be retried — despite the product explicitly promising it can

**Files:** `PracticeSessionService.submitResponse()`, `SessionPage.jsx`, `HistoryPage.jsx`, `README.md`.

The PRD (§15) and the README both promise: *"Evaluation failed. Your response has been saved — you can retry from your session history."* But look at the idempotency guard that runs before every resubmission attempt:

```java
// Idempotency: if already past RESPONSE_SUBMITTED, return current state
if (session.getStatus().ordinal() > SessionStatus.RESPONSE_SUBMITTED.ordinal()) {
    log.info("Session {} already in status {}, returning current state.", sessionId, session.getStatus());
    return SessionSummaryResponse.from(session);
}
```

`SessionStatus` is declared as `STARTED, RESPONSE_SUBMITTED, ANALYZING, ANALYZED, FOLLOWUP_PENDING, FOLLOWUP_ANSWERED, COMPLETED, FAILED, ABANDONED`. `FAILED`'s ordinal (7) is greater than `RESPONSE_SUBMITTED`'s (1), so **any attempt to resubmit a response for a `FAILED` session silently no-ops and just returns the still-`FAILED` state** — analysis is never re-triggered. And on the frontend, `HistoryPage.jsx`'s `handleClick()` doesn't even have a branch for `FAILED` sessions, so clicking one does nothing at all — there is no UI path back into a failed session in the first place.

Net effect: **there is no retry path anywhere in the system**, front or back end, despite it being an explicit, named part of the product's failure-recovery story. (Worth noting: this idempotency rule as literally written in PRD §10 — "if session is already in `ANALYZING` or later, a duplicate submit returns current state" — does technically cover `FAILED` too, so the code is arguably a faithful implementation of an ambiguous spec. But that just means the PRD and the README/UX copy contradict each other, and neither the PRD author's intent (§15/§30's risk table) nor the actual implementation delivers a working retry.)

**Fix sketch:** either add a dedicated `POST /api/sessions/{id}/retry` endpoint that's only valid from `FAILED`, or special-case `FAILED` in the idempotency check to allow a fresh `analyzeSession()` call; either way, also add a "Retry" action in `HistoryPage`/`ResultsPage` for `FAILED` sessions.

### 2.4 `GET /api/sessions/{id}/followup` is not safely repeatable — a page refresh permanently locks the user out of their own follow-up question

**File:** `backend/src/main/java/com/intensify/service/FollowUpService.java`

```java
if (session.getStatus() != SessionStatus.ANALYZED) {
    throw AppException.conflict(
        "Session must be in ANALYZED state to retrieve the follow-up question. Current: " + session.getStatus()
    );
}
...
session.setStatus(SessionStatus.FOLLOWUP_PENDING);
```

This is a `GET` endpoint (invoked from `ResultsPage.jsx`'s "Get follow-up question →" button) that mutates server state as a side effect: the *first* call transitions `ANALYZED → FOLLOWUP_PENDING`. If the user reloads the results page before answering — a completely ordinary thing to do — `ResultsPage.jsx` re-renders the same "Get follow-up question →" button (its guard condition is `session.status === 'ANALYZED' || session.status === 'FOLLOWUP_PENDING'`), but the *second* call to this endpoint now sees status `FOLLOWUP_PENDING` (not `ANALYZED`) and throws a 409. The already-generated follow-up question text is sitting in the DB (`FollowUpQuestion` row exists) but there is no code path that returns it once the status has moved past `ANALYZED` — the user is stuck looking at an error with no way to see or answer a follow-up question they were already shown once.

**Fix sketch:** make this endpoint idempotent — if `session.getFollowUpQuestion() != null`, just return it regardless of status (only *create* a new one when status is `ANALYZED` and none exists yet); don't gate a read on a status that the read itself changes.

### 2.5 Frontend never leaves the "evaluating" screen on failure — no working recovery UI, despite the PRD mandating one

**File:** `frontend/src/pages/SessionPage.jsx`

```jsx
} else if (s.status === 'FAILED') {
  clearInterval(pollRef.current)
  setError('Evaluation failed. Your response has been saved — go to history to retry.')
  setSubmitting(false)
}
...
const isAnalyzing = session?.status === 'ANALYZING' || submitting
...
if (isAnalyzing) return ( /* spinner + "Evaluating your reasoning…" + {error && <alert>} */ )
```

On `FAILED` (and on the 35-second client-side timeout, same pattern), the code sets `submitting = false` and an error message, but **never updates `session.status`** away from `'ANALYZING'` (it was optimistically set to `'ANALYZING'` in `handleSubmit` and is never corrected). Since `isAnalyzing` is `session?.status === 'ANALYZING' || submitting`, it stays `true` forever (the `session.status === 'ANALYZING'` half of the OR is still true even though `submitting` is now false). Result: the user is stuck on the spinner screen with an error message rendered *underneath* the "Evaluating your reasoning… please don't close this tab" spinner — and there is no "Go to History" button anywhere, even though PRD §15 explicitly specifies one for this exact scenario.

**Fix sketch:** on `FAILED`/timeout, update local `session` state (e.g. `setSession(s => ({ ...s, status: 'FAILED' }))`) and render a distinct failure view with a "Go to History" button, rather than reusing the analyzing view with the error text bolted on.

### 2.6 Follow-up evaluation can't actually check "consistency with the main response" — the main response is never sent to the LLM for that call

**File:** `AIAnalysisService.buildFollowUpPrompt()` / `analyzeFollowUp()`

The follow-up rubric (PRD §11.10) includes `CONSISTENCY: Is the follow-up answer consistent with the candidate's main session explanation?`. But `buildFollowUpPrompt(String followUpQuestion, String correlationId)` only receives the follow-up *question* text, and `wrapFollowUpInDelimiters(answer)` only wraps the follow-up *answer*. The candidate's original main explanation is never fetched or included anywhere in the follow-up prompt. The LLM is being asked to judge consistency against a document it was never shown — it can only guess or hallucinate plausible-sounding consistency commentary. This is a functional gap against the PRD's own rubric definition, not just a style nit.

**Fix sketch:** fetch `getExplanationText(session)` in `analyzeFollowUp()` and include it in the follow-up system/user prompt, clearly delimited, so the model has something real to compare against.

### 2.7 Retry-once-on-validation-failure doesn't do anything

**File:** `AIAnalysisService.retryOnce()`

```java
private EvaluationResult retryOnce(String systemPrompt, String userContent, EvaluationResult result, String correlationId) {
    if (result == null) {
        log.warn("[{}] First attempt returned null, retrying once...", correlationId);
        return callWithTimeout(systemPrompt, userContent, correlationId);
    }
    return result;
}
```

The architecture doc (PRD §19) requires: *"retry once on validation failure; otherwise mark FAILED."* But `callWithTimeout()` either returns a fully-parsed `EvaluationResult` or throws (`AIProviderException` on timeout/HTTP error, a Jackson exception on malformed JSON) — it never returns `null`. So this method's only branch condition is unreachable in normal operation, and it is effectively dead code: malformed AI output goes straight to the outer `catch` and the session is marked `FAILED` on the very first attempt, with no retry ever happening. This is a real, if lower-severity, gap versus the documented architecture.

### 2.8 Hardcoded JWT signing-secret fallback checked into the repo

**File:** `backend/src/main/resources/application.properties`

```properties
app.jwt.secret=${JWT_SECRET:ThisIsAVeryLongSecretKeyForJWTSigningThatMustBeAtLeast256BitsLong!}
```

If `JWT_SECRET` is ever unset in a given environment (a misconfigured deploy, a forgotten env var on a new machine, a container without secrets wired up), the app silently signs tokens with this fixed string, which is sitting in plaintext in a file that (once this project is pushed to git, see §4.1) would be public in the repo. Anyone who has read this file could forge a valid JWT for any user/email on any deployment that fell back to the default. This should fail fast (refuse to start) rather than silently fall back to a known secret. (The actual `.env` in this project *does* set a real `JWT_SECRET`, and `.env` is correctly gitignored in `backend/.gitignore` — so today's local setup isn't exposed — but the fallback itself is the defect: a production-safety net that should not exist.)

**Fix sketch:** remove the default; let Spring fail to start with a clear error if `JWT_SECRET` is missing (`app.jwt.secret=${JWT_SECRET}` with no colon-default, or an `@PostConstruct` assertion).

---

## 3. Moderate findings

### 3.1 No version control anywhere in the project
There is no `.git` directory at the project root or in `backend/`/`frontend/` — this is not currently a git repository at all. `backend/.gitignore` and `.gitattributes` exist (correctly configured), but they're inert without a repo. Given the PRD explicitly lists Git/GitHub in the tech stack (§3) and the whole point of the project is a resume/portfolio piece (§29), this should be `git init` + first commit before anything else — there is currently no history, no way to review what changed, and no way to safely revert a bad change.

### 3.2 Zero automated tests
`backend/src/test/java/com/intensify/BackendApplicationTests.java` contains exactly one test, `contextLoads()`, whose body is empty. PRD §24 explicitly requires: unit tests for session state transitions, ownership/authorization checks, structured AI-output validation, evidence verification, daily usage limits, and account-deletion; integration tests with Testcontainers; a golden test set of good/mediocre/weak sample answers; and mocking `AIProvider` in CI (never real LLM calls in tests). None of this exists yet, on either backend or frontend. Given the bugs in §2.1–§2.4 are exactly the kind unit tests would have caught (a `findBestWindow` test with a known best-match window would have failed immediately; an idempotency test that resubmits a `FAILED` session would have surfaced §2.3 instantly), this is the single highest-leverage gap to close next — not for coverage-percentage's sake, but because it would have caught the correctness bugs above by construction.

### 3.3 `docs/` folder and supporting documentation don't exist
PRD §26 (repository structure) specifies `docs/architecture-diagram.png`, `docs/er-diagram.png`, `docs/api-documentation.md`, `docs/ai-evaluation-methodology.md`, and a `screenshots/` folder. None exist. `README.md` does cover a condensed version of some of this (an ASCII architecture diagram, an API table, "Key Design Decisions," known limitations) — it's a reasonable README, but it doesn't satisfy §27's ask for a proper ER diagram, a dedicated AI-evaluation-methodology writeup (the PRD calls the rubric documentation "your strongest interview talking point, document it well" — currently it lives only in the PRD and in prompt strings in code, not in a standalone doc), or explicit sample request/response API documentation.

### 3.4 The AI "validation layer" doesn't validate much
PRD §19 describes a validation layer checking "schema, score ranges, required fields" before persisting. In the actual code, validation is limited to: Jackson deserialization succeeding (structural only — a response missing fields just leaves them `null`), and a score-scale normalization hack (`if (cs.getScore() > 0 && cs.getScore() <= 10) cs.setScore(cs.getScore() * 10)`) that guesses whether the model answered on a 0–10 or 0–100 scale. There's no check that scores are within `[0,100]` after normalization (a hallucinated `250` would sail through), no check that `dimension` values match the expected rubric's dimension names, and no server-side override of `CODE_CONSISTENCY.applicable` based on whether code was actually submitted (it's entirely trusted from the model's own JSON, even though the ground truth — was code submitted or not — is known server-side and cheap to enforce).

### 3.5 `overallScore` deviation is never logged
PRD §11.5: *"If the AI-returned `overallScore` deviates from the computed mean by more than 5 points, log a warning for prompt-quality monitoring."* The code always overwrites `result.setOverallScore(computedScore)` (correct — the computed value should win) but never compares it to what the model originally returned before overwriting it, so the prompt-quality monitoring signal the PRD asks for doesn't exist.

### 3.6 `AIProviderRouter` isn't actually a component, and adding a provider means editing `AIAnalysisService`
PRD §19's stated design principle: *"Swapping or adding providers should not require changes to `AIAnalysisService`."* In practice, `AIAnalysisService` has hard constructor dependencies on both `OpenAIProvider` and `GeminiProvider` by concrete type, and `getActiveProvider()` is an `if/else` inside the service itself:
```java
private AIProvider getActiveProvider() {
    if ("openai".equalsIgnoreCase(configuredProvider)) return openAIProvider;
    return geminiProvider;
}
```
Adding a third provider requires editing this class. A small `Map<String, AIProvider>` (Spring auto-populates this from all `AIProvider` beans keyed by bean name) injected once and looked up by `configuredProvider` would satisfy the open/closed principle the PRD states as a design goal, and would also be a natural home for the "router" concept named in the architecture diagram (§19) but never actually built as a distinct class.

### 3.7 Config-driven length caps are dead configuration
`application.properties` defines `app.session.explanation-max-chars=2000` and `app.session.code-max-chars=5000`, but nothing in the codebase reads them (`grep` confirms no `@Value` binds to either key). The actual enforcement is hardcoded directly in `SessionDtos` via `@Size(max = 2000)` / `@Size(max = 5000)`. The numbers happen to agree today, but changing the properties file will silently do nothing — worth either wiring the properties in (e.g. a custom validator) or deleting the unused keys so the config doesn't lie about being tunable.

### 3.8 Race condition: duplicate registration under concurrent requests surfaces as a raw 500
`AuthenticationService.register()` checks `existsByEmail()` then inserts — classic TOCTOU. Two near-simultaneous registrations with the same email will both pass the check, and the second `INSERT` will hit the `UNIQUE` constraint on `users.email`, throwing a `DataIntegrityViolationException` that isn't caught anywhere, so it falls to `GlobalExceptionHandler`'s generic `Exception` handler and returns a bare "An unexpected error occurred" 500 instead of the friendly 409 "Email is already registered" the non-racing path returns. Low real-world likelihood for a single-user practice tool, but a five-minute fix (catch `DataIntegrityViolationException` and map to the existing `AppException.conflict`).

### 3.9 Controllers duplicate a "resolve current user" pattern that a utility class already exists for
`SecurityUtils.getCurrentUser()`/`getCurrentUserId()` exist and are well-written, but no controller uses them. Instead, `AuthController`, `ProgressController`, and `SessionController` each independently re-look up the user by email from `UserDetails.getUsername()` (`SessionController.resolveUserId()`, an inline block in `ProgressController.getSummary()`). Not a bug, but it's unused, already-built infrastructure being reimplemented three times — worth consolidating onto `SecurityUtils` for one source of truth.

### 3.10 No database migration tool — `ddl-auto=update`
`spring.jpa.hibernate.ddl-auto=update` lets Hibernate infer schema changes from entity annotations at boot. It's a reasonable speed trade-off for a solo 3-week MVP, but it has no migration history, can't express destructive changes safely, and behaves unpredictably once the schema needs a real change post-launch (renaming/dropping a column, changing a type). Flyway or Liquibase would be a natural "first post-MVP improvement" alongside the async-analysis upgrade the PRD already flags (§21).

### 3.11 No Docker / docker-compose despite PRD guidance
PRD §25: *"a working local Docker Compose setup is sufficient through most of the 3 weeks."* No `Dockerfile` or `docker-compose.yml` exists anywhere in the project. Not blocking for local dev (the README's manual MySQL + `mvnw spring-boot:run` + `npm run dev` instructions work), but it's explicitly-recommended, entirely-missing groundwork for the deployment step that hasn't happened yet.

### 3.12 Gemini model name / config duplication
`GeminiProvider.java` defaults `model` to `"gemini-2.0-flash"` via `@Value("${app.ai.gemini.model:gemini-2.0-flash}")`, but `application.properties` sets the *property itself* to `${GEMINI_MODEL:gemini-3.5-flash-lite}` — since the property key is present, the class-level default never actually applies; the properties-file default (`gemini-3.5-flash-lite`) wins whenever `GEMINI_MODEL` isn't set in the environment. Two different hardcoded model-name defaults living in two different files is a maintenance trap even before asking whether both names are current/valid model identifiers with the provider (worth double-checking against Google's current model list before relying on the fallback).

### 3.13 Frontend `.env` isn't gitignored
`frontend/.gitignore` (the stock Vite template's `.gitignore`) does not list `.env`, unlike `backend/.gitignore` which explicitly does. Today `frontend/.env` only holds `VITE_API_BASE_URL=http://localhost:8080` (not sensitive), but the pattern is inconsistent with the backend and would silently commit any future frontend secret. Add `.env` (or at least `.env.local`) to `frontend/.gitignore` before this project's first `git init`.

### 3.14 History page has no interaction for `ANALYZING` / `FOLLOWUP_ANSWERED` / `FAILED` rows
`HistoryPage.jsx`'s `handleClick()` only branches on `COMPLETED`/`ANALYZED`/`FOLLOWUP_PENDING` (→ results) and `STARTED` (→ session). Rows in `ANALYZING`, `FOLLOWUP_ANSWERED`, or `FAILED` render with a pointer cursor (implying clickability) but do nothing when clicked. `FAILED` additionally has nowhere to go per §2.3 above.

### 3.15 Leftover Vite starter-template boilerplate
`frontend/src/App.css` still contains the default Vite/React template's `.counter`, `.hero .base/.framework/.vite` rules, and `frontend/src/assets/react.svg` + `vite.svg` are unused template assets. Harmless, but worth a cleanup pass — dead CSS/assets in a from-scratch product page read as unfinished polish.

---

## 4. What's actually done well

Worth stating plainly, since an audit that only lists problems is as misleading as one that only lists praise:

- **Auth & ownership.** JWT flow (`JwtUtils`, `JwtAuthFilter`, `SecurityConfig`) is correctly stateless, BCrypt is used for passwords, and every session-scoped read/write goes through `findByIdAndUserId` / `findOwnedSession` — a user genuinely cannot access another user's session by guessing an ID. This is the exact "logged in *and* owns this resource" bar the PRD sets in §18/§29, and it's met consistently across `SessionController`.
- **Account deletion cascades correctly.** `CascadeType.ALL` + `orphanRemoval = true` on `User.sessions`/`User.skillMetrics` and on `PracticeSession`'s children means `DELETE /api/auth/account` genuinely removes all dependent data in one call, matching PRD §10/§18 exactly.
- **Question bank matches the spec precisely.** 45 questions seeded (15 DSA / 15 System Design / 15 Conceptual), each with a real prompt and a meaningful `expectedConcepts` list for rubric grounding — this was verified by counting every entry in `QuestionSeeder.java`, not assumed from the README.
- **Prompt-injection mitigation is implemented as designed.** Candidate text is wrapped in `<candidate_response>`/`<explanation>`/`<code>` tags with an explicit system-prompt instruction to treat the content as untrusted and ignore embedded instructions — matches PRD §18 exactly.
- **Daily cap + abandoned-session cleanup both work as specified.** `enforceDailyCap` correctly excludes `ABANDONED` sessions from the count; the `@Scheduled(cron = "0 0 2 * * *")` job correctly sweeps `STARTED` sessions older than 24h into `ABANDONED`.
- **Two AI providers already exist** (Gemini + OpenAI via Spring AI's `ChatClient`), ahead of the PRD's own timeline expectation (a second provider was explicitly P2/"not expected in 3 weeks," §12).
- **Consistent `{ data, error }` API envelope** across every controller, and a single `@RestControllerAdvice` for error handling — exactly the consistency PRD §21 asks for.
- **The frontend polling/loading UX skeleton is correctly designed** (3-second poll interval, 35-second client timeout, spinner + "usually takes up to 30 seconds" copy) — the *logic* backing it has the bugs noted in §2.2/§2.5, but the intended design matches PRD §15 closely.
- **Progress dashboard correctly gates on the 3-session minimum per dimension** (`ProgressService`, `MIN_SESSIONS_FOR_CHART = 3`) and is filtered by category, both per PRD §20.

---

## 5. PRD compliance matrix

Based on the P0 feature list in PRD §12 and the MVP acceptance criteria in §23.

| Feature (PRD §12/§23) | Status | Notes |
|---|---|---|
| Auth (register/login/JWT) + account deletion | **Done** | Cascading delete verified in entity mappings |
| Question bank (DSA + System Design + Conceptual, 15–20/category) | **Done** | 45/45 seeded, verified by count |
| Practice session flow (start → explain → submit) | **Done, with bugs** | Works end to end; retry-on-FAILED is broken (§2.3) |
| Typed approach explanation | **Done** | 2,000-char cap enforced |
| AI evaluation (structured JSON, DSA rubric) | **Done, with bugs** | Core pipeline works; validation layer is thin (§3.4), retry-on-validation-failure is dead code (§2.7) |
| Evidence verification | **Broken** | Near-verbatim fallback doesn't search for a best match (§2.1) — the feature's core anti-hallucination logic is not functioning as designed |
| Results page (scores, strengths/weaknesses, evidence) | **Done** | Matches PRD data shape |
| One AI-generated follow-up question + evaluation | **Done, with bugs** | Question generation works; consistency-with-main-response can't be evaluated (§2.6); re-fetching the question after the first fetch 409s (§2.4) |
| Session history (list + detail) | **Done, with gaps** | No action available for `FAILED`/`ANALYZING` rows (§3.14) |
| Progress dashboard (≥3-session gate) | **Done** | Correctly gated and category-filtered |
| Code submission for DSA (no execution) | **Done** | Stored, UNIQUE-per-session with update-on-resubmit, sent to LLM in delimited block |
| Multi-language code field | **Done** | Stored as free string, not enum, per spec |
| `AIProvider` interface + provider(s) + router | **Partially done** | Interface + 2 concrete providers exist; no distinct router component — selection logic lives inside `AIAnalysisService` (§3.6) |
| Daily usage cap + input length caps | **Done** | Both enforced (length caps hardcoded rather than config-driven, §3.7) |
| Golden test set | **Not done** | No test infrastructure of any kind exists |
| System Design / Conceptual rubrics | **Done** | Full rubric + prompt branch exists for both categories |
| FAILED state + recovery | **Partially done** | State transition and failure_reason work; the promised *recovery* (retry) does not exist anywhere (§2.3), and the frontend's failure screen is stuck (§2.5) |

---

## 6. Recommended priority order

1. Fix evidence-verification's `findBestWindow` (§2.1) — this is the product's core trust mechanism.
2. Move the AI provider call out of the request's DB transaction (§2.2) — this is a production-stability landmine, not a nice-to-have.
3. Implement an actual retry path for `FAILED` sessions, front and back end (§2.3), and fix the stuck loading screen (§2.5) — together these mean the app currently has *no* working failure recovery, despite that being called out as a named risk mitigation in the PRD itself (§30).
4. Make `GET /followup` idempotent (§2.4).
5. Add the missing test layer (§3.2) — even a modest set covering session-state transitions, ownership checks, and evidence verification (mocking `AIProvider`) would have caught four of the seven critical findings above before they shipped.
6. `git init` this project today, before any further changes, so all of the above fixes are reviewable and revertible (§3.1).
7. Everything in §3 after that, roughly in the order listed — none of it blocks a demo, but all of it is real technical debt for a "call it production-ready" bar.

---

*This audit reflects the state of the code as of the files read on 2026-08-29. It is based entirely on static reading of the source — nothing was compiled, run, or tested against a live database/LLM, so purely runtime behaviors (actual LLM output shapes in practice, real connection-pool exhaustion under load, etc.) are inferred from the code rather than observed directly. Recommend validating §2.1–§2.2 empirically (a unit test for the former, a load test for the latter) before treating them as fully confirmed in production conditions — though both are deterministic logic bugs readable directly from the source, independent of any LLM behavior.*
