# Product Requirements Document
# Technical Interview Reasoning Coach

**Version:** 1.1 (MVP)
**Owner:** Solo developer (Java/Spring Boot, learning React)
**Timeline:** 3-week target to first usable MVP (DSA-only is the real floor; weeks 4+ treated as expected buffer, not failure)

---

## 1. Product Overview

Technical Interview Reasoning Coach is a practice platform that evaluates a candidate's **technical reasoning** while solving interview-style problems — not whether their answer is correct, and not how smoothly they speak.

It focuses on a narrower gap than most existing tools: while coding platforms grade correctness and many AI coaches evaluate delivery or high-level communication, fewer tools systematically evaluate whether a candidate explains their approach, justifies decisions, discusses trade-offs, and stays consistent between what they say and what they submit — using a fixed, evidence-citing rubric and tracking improvement per dimension.

The MVP covers three interview formats — DSA/coding, system design, and technical conceptual questions — through one consistent evaluation and progress-tracking engine. System Design and Conceptual are valuable extensions; a reliable DSA-only loop is the minimum bar for calling the MVP done.

---

## 2. Problem Statement

Candidates preparing for technical interviews practice in two disconnected ways: solving problems silently on coding platforms, and (separately) polishing delivery on general interview-coaching apps. While several tools now attempt to evaluate communication or approach, most still under-emphasize the specific skills interviewers grade as a distinct axis: **explaining technical reasoning clearly while working through a problem, justifying decisions, discussing trade-offs, and keeping the explanation consistent with the submitted solution.**

A technically strong candidate can still underperform by starting to code before stating an approach, failing to justify why an approach was chosen, skipping trade-off/complexity discussion, or giving an explanation inconsistent with their actual solution. This gap is often discovered for the first time in a real interview. This product deliberately stays narrow on that axis.

---

## 3. Technology Stack

### Backend
- **Java 21** — primary backend language
- **Spring Boot** — application framework
- **Spring Web** — REST APIs
- **Spring Data JPA** and **Hibernate** — persistence/ORM
- **Spring Security** — authentication and authorization
- **JWT** — stateless authentication
- **Bean Validation** — request validation
- **Lombok** — boilerplate reduction
- **Maven** — build and dependency management

### Database
- **MySQL** — relational persistence for users, questions, sessions, responses, analyses, follow-ups, and progress data

### Frontend
- **React.js**
- **JavaScript**
- **HTML5 / CSS3**
- **React Router**
- **Axios** for REST API communication

### AI / External Services
- **Spring AI** — provider integration abstraction
- One or more external LLM providers for technical reasoning evaluation and contextual follow-up generation
- Candidate AI outputs are requested in **structured JSON** and validated before persistence
- Voice/speech-to-text is intentionally deferred from the MVP

### Testing & Development
- **JUnit 5**, **Mockito**, **Spring Boot Test**
- **Postman** for REST API testing
- **Git / GitHub**
- **IntelliJ IDEA / VS Code**

### Deployment
- Deployment platform remains open for the MVP and will be selected based on cost, simplicity, and compatibility with the backend, frontend, database, and selected AI provider(s).

**Architecture requirement:** Spring Boot must remain the primary application backend and must handle authentication, authorization, business logic, REST APIs, database operations, practice-session management, AI-service integration, analysis-result persistence, and progress tracking. AI providers are external integrations, not the core application architecture.

---

## 4. Target Users

- Students preparing for campus placements
- Fresh graduates entering the job market
- Software engineers preparing for a job change
- Experienced developers brushing up before a loop
- Candidates preparing for varied technical roles (backend, frontend, generalist)

---

## 5. Goals and Objectives

- Give candidates a way to practice **explaining reasoning**, not just solving problems, against a consistent rubric.
- Produce **evidence-based, specific** feedback ("you didn't justify why HashMap reduces complexity before implementing it") instead of generic advice.
- Track improvement **per reasoning dimension** over time, not a single vague score.
- Ship a reliable, complete, if narrow, end-to-end workflow within 3 weeks — favor depth of the core loop over breadth of features.

---

## 6. Product Scope

**In scope for MVP:**
- DSA/coding, system design, and conceptual question categories (DSA is the non-negotiable floor)
- Typed (text) explanation of approach — audio input deferred to P1
- Optional code submission for DSA questions (no execution/judging — analyzed for consistency with stated reasoning only; AI may offer best-effort, clearly labeled non-authoritative commentary on obvious issues)
- Structured AI evaluation against a defined rubric per category
- One AI-generated contextual follow-up question per session, with its own evaluation
- Session history and per-dimension progress tracking (charts appear only after minimum data)

**Explicitly out of scope for MVP** (see Section 28):
Live interview assistance, video/eye-tracking/emotion analysis, custom ML models, code execution/judging infrastructure, peer-to-peer mock interviews, recruiter tools, resume builder, job application tracking, mobile app.

---

## 7. User Personas

**Priya, final-year student** — Has done 200+ LeetCode problems alone and silently. Solves correctly but has never had to narrate her thinking out loud; freezes when asked to explain in a live interview.

**Arjun, 3 YOE engineer switching jobs** — Strong day-to-day engineer, but hasn't interviewed in years. Knows the algorithms but rusty at articulating trade-offs under time pressure.

**Meera, non-native English speaker** — Reasons well internally but struggles to structure the same reasoning fluently out loud in real time, in a second language, under pressure.

**Rahul, backend engineer prepping for a system-design-heavy loop** — Comfortable with DSA, wants structured practice specifically justifying architectural decisions (why this DB, why this caching strategy) rather than just naming components.

---

## 8. User Stories

- As a candidate, I want to pick a question category and difficulty so I can practice something relevant to my target interview.
- As a candidate, I want to explain my approach in text before submitting code, so my reasoning is captured separately from my implementation.
- As a candidate, I want specific, evidence-backed feedback tied to what I actually said, not generic advice.
- As a candidate, I want a follow-up question based on my own answer, so I practice handling the kind of probing a real interviewer would do.
- As a candidate, I want to see whether I'm improving on specific dimensions (e.g., "stating approach before coding") across sessions, not just an overall score.
- As a returning user, I want my full session history so I can revisit past feedback.

---

## 9. Core User Journey

1. Register / log in (JWT issued).
2. Land on dashboard — see progress summary and "start new session."
3. Select interview category (DSA / System Design / Conceptual).
4. Select a specific question (filtered by difficulty/topic).
5. Start practice session (status: `STARTED`).
6. Read the problem/question prompt.
7. Type explanation of understanding + approach.
8. (DSA only, optional) Submit code in a chosen language.
9. Submit the session response (status: `RESPONSE_SUBMITTED`).
10. Backend triggers AI analysis (status: `ANALYZING`).
11. AI evaluates reasoning against the category rubric; where code was submitted, checks explanation-vs-code consistency.
12. Structured evaluation persisted (status: `ANALYZED`).
13. System surfaces strengths, weaknesses, missing concepts, incorrect reasoning, and specific recommendations, each tied to evidence from the candidate's own text.
14. AI generates one contextual follow-up question based on the analysis (status: `FOLLOWUP_PENDING`).
15. Candidate answers the follow-up (typed).
16. AI evaluates the follow-up response (status: `FOLLOWUP_ANSWERED` → `COMPLETED`).
17. Final result stored; skill-metric rows written per dimension for this session.
18. Candidate views results page (scores, feedback, follow-up outcome).
19. Candidate can revisit history and view the progress dashboard (trend per dimension across sessions).

---

## 10. Functional Requirements

**Authentication & User Management**
- Register/login with email + password, JWT-based session, password hashing (BCrypt).
- User profile (name, email, target role/category preference — optional).
- Account deletion endpoint that cascades and removes all user data associated with the account.

**Question Management**
- Curated question bank per category, tagged by difficulty and topic.
- **15–20 high-quality questions per category** (45–60 total) for MVP. Re-answering the same question to improve explanation is legitimate practice (the skill being trained is communication, not solving a new problem each time). When a user re-answers a question they have previously completed, the results page surfaces their prior explanation alongside the new one to make improvement concrete and visible. This is stated clearly in-product.
- Each question stores an internal "expected reasoning checkpoints" structure used to ground AI evaluation (not shown to the user).

**Practice Session Management**
- Create, update status, retrieve, and list sessions per user.
- Enforce user-level data isolation — a user can only access their own sessions.
- Idempotent submit: if session is already in `ANALYZING` or later, a duplicate submit returns current state instead of re-triggering analysis.

**Response Handling**
- Accept typed approach explanation (required).
- Accept optional code submission (DSA category only), with a language selector (Java/Python/C++/JS/C#/Go — stored as a string field, not hardcoded enum, for easy extension).

**AI Evaluation**
- Trigger evaluation on session submission.
- Persist structured, validated JSON results — never store raw unvalidated LLM text as the source of truth.
- Every evidence string returned by the AI must be programmatically verified as a substring (or near-substring) of the candidate’s actual submitted text. If verification fails, that feedback item is discarded or flagged “unverified” and not shown as fact.
- Generate one follow-up question per completed session; evaluate the follow-up response separately.
- Optional lightweight thumbs-up/down on individual feedback items (smoke-detector signal only).

**Progress Tracking**
- Persist a score per reasoning dimension per session.
- Aggregate and expose per-dimension trend data across a user's session history.
- Trend charts remain hidden until the user has at least 3 completed sessions; before that, show a “keep practicing to unlock trends” placeholder.

**History**
- Paginated list of past sessions with summary scores; drill into any session's full feedback.

**Usage & Cost Controls**
- Enforce a per-user daily session cap (default: **10 sessions/day**) using existing MySQL data.
- Enforce explicit input length caps before sending content to the LLM: approach explanation ≤ **2,000 characters**; code submission ≤ **5,000 characters**. Requests exceeding these limits are rejected with a clear validation error *before* any AI call is made.
- Target per-session LLM cost ≤ **$0.05** (prompt + completion tokens combined). Monitor actual usage after launch and tighten caps if needed. Set a hard monthly spend ceiling at the API key level (provider dashboard) as a safety net against unexpected spikes.
- **Session cleanup:** sessions stuck in `STARTED` status for more than **24 hours** without a response submission are marked `ABANDONED` by a scheduled cleanup job (Spring `@Scheduled`, runs daily). Abandoned sessions do not count toward the daily cap and are shown in history as "Incomplete."

**Email Verification (Known Limitation)**
- Email verification is out of scope for MVP. The daily session cap is the primary abuse guard. This is a known limitation: a determined user can bypass per-account caps by re-registering. Document this plainly and plan email verification as an early post-MVP addition.

---

## 11. AI Evaluation Requirements

### 11.1 Rubric — DSA / Coding

| Dimension | What's evaluated |
|---|---|
| Problem Understanding & Clarification | Did the candidate restate/clarify the problem before proposing a solution? |
| Approach Formulation | Was an approach stated *before* implementation began? |
| Reasoning Quality | Is the "why" behind the approach explained, not just the "what"? |
| Alternatives & Trade-offs | Were other approaches considered and compared? |
| Complexity Analysis | Time and space complexity stated and correctly justified? |
| Technical Accuracy | Are technical claims correct? |
| Explanation–Code Consistency | Does the submitted code (if any) match what was described? |
| Missing Concepts / Incorrect Claims | Deduction-based: flags anything materially wrong or omitted. |

### 11.2 Rubric — System Design

| Dimension | What's evaluated |
|---|---|
| Requirement Clarification | Functional and non-functional requirements addressed? |
| High-Level Architecture | Components identified and justified? |
| Data Flow & API Design | Is the flow between components coherent? |
| Scalability & Reliability Reasoning | Are bottlenecks and scaling strategies discussed? |
| Database & Caching Choices | Are choices justified, not just named? |
| Trade-offs | Are trade-offs between options articulated? |
| Justification Quality | Overall: are decisions defended with reasoning? |

### 11.3 Rubric — Conceptual

| Dimension | What's evaluated |
|---|---|
| Accuracy | Is the explanation factually correct? |
| Completeness | Are the important facets of the concept covered? |
| Conceptual Understanding | Depth beyond a memorized definition? |
| Explanation Clarity | Clear structure, useful examples? |
| Follow-up Handling | Does the follow-up answer hold up under a probing question? |

### 11.4 Explicit exclusions
Do **not** score subjective, non-derivable qualities like "confidence" or "charisma." Every score must be traceable to something explicit in the candidate's text. If a dimension can't be evaluated from the given input (e.g., no code submitted), it's marked `NOT_APPLICABLE`, not guessed.

### 11.5 Structured Output Contract

All AI evaluation calls must return JSON conforming to a fixed schema (enforced via Spring AI's structured output support, mapped directly to a Java DTO):

```json
{
  "overallScore": 72,
  "categoryScores": [
    { "dimension": "APPROACH_FORMULATION", "score": 60, "applicable": true },
    { "dimension": "COMPLEXITY_ANALYSIS", "score": 40, "applicable": true },
    { "dimension": "CODE_CONSISTENCY", "score": null, "applicable": false }
  ],
  "strengths": [
    { "point": "Correctly identified hash map as the optimal structure.", "evidence": "\"I'd use a hash map to store seen values\"" }
  ],
  "weaknesses": [
    { "point": "Jumped into implementation before stating why the approach improves complexity.", "evidence": "Approach and implementation given in same sentence with no complexity justification." }
  ],
  "missingConcepts": ["Space complexity was never mentioned."],
  "incorrectClaims": [],
  "recommendations": [
    "State the approach and its complexity benefit before describing implementation steps."
  ],
  "followUpQuestion": "You mentioned using a hash map — what's the space complexity trade-off compared to the brute-force approach, and when would you prefer brute force instead?"
}
```

**`overallScore` Computation:** The overall score is the **simple arithmetic mean** of all `applicable` dimension scores (dimensions with `"applicable": false` are excluded from the average, rounded to the nearest integer). The AI's returned `overallScore` is treated as a hint only — the backend always recomputes it from dimension scores and uses the computed value. If the AI-returned `overallScore` deviates from the computed mean by more than 5 points, log a warning for prompt-quality monitoring. This makes the score deterministic, auditable, and consistent across sessions.

### 11.6 Feedback Quality Standard
Bad: *"Improve your explanation."*
Good: *"You identified the HashMap approach correctly, but you started discussing implementation before explaining why it reduces complexity from O(n²) to O(n). State the approach and its justification before moving into implementation."*
Every weakness/recommendation must cite evidence from the candidate's actual text.

### 11.7 Evidence Verification Rule
Before any strength, weakness, or recommendation is displayed:
- Extract the `evidence` string.
- **Verbatim check:** confirm the evidence string is an exact substring of the candidate's submitted text (case-insensitive, after stripping leading/trailing whitespace). If matched, the item is verified.
- **Near-verbatim fallback:** if the exact check fails, compute normalized Levenshtein similarity between the evidence string and the best-matching window of the submitted text of the same length. If similarity ≥ **85%**, treat as verified. (The 85% threshold is the concrete implementation target; tighten to 90% if testing shows clearly hallucinated strings passing through.)
- Log all near-verbatim passes server-side (session ID + evidence string + similarity score) for auditability and future prompt-quality review.
- If neither check passes: **discard the item silently** from displayed feedback. Do not show the user an "unverified" label — it creates confusion. Log the discarded item server-side with the session ID for debugging and model improvement.
- Never present discarded or unverified evidence as fact.

### 11.8 Golden Test Set
A small set of hand-written sample answers (good / mediocre / weak) with expected score direction and key feedback themes is maintained. This set is run as a regression check whenever the prompt or rubric changes. It does not scientifically validate the AI; it catches obvious regressions.

### 11.9 Code Commentary (DSA only)
When code is submitted, the AI may include best-effort observations on obvious issues as part of the consistency check. These observations are clearly labeled as non-authoritative commentary (e.g., "Possible observation — not a correctness judgment"). Users are told plainly to verify functional correctness elsewhere.

### 11.10 Follow-up Evaluation Rubric

The follow-up question is contextually generated from the main session analysis. Its evaluation uses a **simplified, category-agnostic rubric** focused on depth and accuracy under probing — not a repeat of the main session rubric. This applies regardless of which category (DSA, System Design, Conceptual) the main session used.

| Dimension | What's evaluated |
|---|---|
| Accuracy Under Probing | Is the follow-up answer factually correct? |
| Depth of Reasoning | Does the answer go beyond restating the original response? |
| Consistency with Main Response | Is the follow-up answer consistent with the candidate's main session explanation? |
| Completeness | Are the key facets of the follow-up question addressed? |

The follow-up evaluation returns its own `overallScore` (computed as the mean of applicable dimensions, same rule as §11.5), plus its own `strengths`, `weaknesses`, and `recommendations`. These are stored as a separate `FOLLOWUP` row in `analysis_results` and displayed on the results page beneath the main evaluation. Evidence verification (§11.7) applies to follow-up feedback as well.

---

## 12. Feature Prioritization

| Feature | Priority |
|---|---|
| Auth (register/login/JWT) + account deletion | P0 |
| Question bank (curated, seeded, DSA + System Design + Conceptual) | P0 |
| Practice session flow (start → explain → submit) + idempotency | P0 |
| Typed approach explanation | P0 |
| AI evaluation (structured JSON, DSA rubric first) + evidence verification | P0 |
| Results page (scores, strengths/weaknesses, evidence) | P0 |
| One AI-generated follow-up question + its evaluation | P0 |
| Session history (list + detail) | P0 |
| Basic progress dashboard (charts only after ≥3 sessions) | P0 |
| Code submission for DSA (text only, no execution) + labeled commentary | P0 |
| Multi-language code field | P0 |
| `AIProvider` interface + one concrete provider + router | P0 |
| Daily usage cap + input length caps | P0 |
| Golden test set process | P0 |
| Second concrete provider + round-robin | **P2** (explicitly not expected in 3-week window) |
| Provider failover, rate-limit awareness, health checks, usage tracking | P2 |
| Voice recording + speech-to-text input | P1 |
| Multiple follow-up questions / follow-up chains | P1 |
| Difficulty-adaptive question selection | P1 |
| User-provided/pasted custom questions | P1 |
| AI-generated (non-curated) questions | P2 |
| Async analysis (202 + polling) | Documented upgrade path (first post-MVP improvement) |
| Code execution/judging | Explicitly excluded |
| HR/behavioral round evaluation | Explicitly excluded |
| Video/eye-tracking/emotion detection | P2 (unlikely) |
| Mobile app | P2 |

---

## 13. System Architecture

```
[React SPA] ──HTTPS/JWT──▶ [Spring Boot Monolith]
                                  │
                    ┌─────────────┼─────────────────┐
                    ▼             ▼                  ▼
              [MySQL]      [AI Evaluation       [External LLM
             (all app       Service layer]        Provider API]
              data)              │
                                  ▼
                         Structured JSON
                         validation layer
```

Single Spring Boot monolith for MVP — no microservices, no message queue. AI calls are **synchronous with a hard 30-second timeout** for MVP simplicity, with a clear upgrade path to async (see Section 21) if evaluation latency becomes a UX problem. The frontend loading UX (see §15) is designed to be compatible with an async upgrade without a rewrite.

---

## 14. Backend Architecture

**Package structure:**
```
com.interviewcoach
├── controller       (REST endpoints, thin — delegate to services)
├── service           (business logic)
├── repository        (Spring Data JPA interfaces)
├── entity            (JPA entities)
├── dto               (request/response objects — never expose entities directly)
├── security          (JWT filter, Spring Security config, UserDetails impl)
├── exception         (custom exceptions + @ControllerAdvice global handler)
├── config            (AI client config, CORS, Spring AI config)
└── integration       (AI provider client wrapper, prompt templates)
```

**Service boundaries and responsibilities:**
- `AuthenticationService` — registration, login, JWT issuance.
- `QuestionService` — question retrieval/filtering by category/difficulty.
- `PracticeSessionService` — session lifecycle/state transitions.
- `CodeAnalysisService` — packages code + explanation for the consistency-check portion of the AI call (no execution).
- `AIAnalysisService` — builds the rubric-specific prompt, calls the LLM via Spring AI, validates/parses the structured response.
- `FollowUpService` — generates and evaluates the follow-up question/response.
- `ProgressService` — writes per-dimension skill-metric rows, aggregates trend data for the dashboard.

Controllers stay thin (validate input via `@Valid`, delegate, map to DTOs). All cross-cutting exceptions handled by one `@ControllerAdvice` returning a consistent error envelope.

---

## 15. Frontend Architecture

**Pages/components:**
- Login / Register
- Dashboard (progress summary, "start new session" CTA)
- Category selection
- Question list/detail (filtered by category/difficulty)
- Practice session page (prompt display, text area for explanation, optional code input)
- Submission/loading state (while AI evaluates)
- Analysis/results page (scores, strengths/weaknesses, evidence, recommendations)
- Follow-up interaction page
- Practice history (paginated list → detail view)
- Progress dashboard (per-dimension trend charts — simple line/bar charts, no need for elaborate visualization)

Keep styling clean and professional using **vanilla CSS / CSS modules** — this is a practice tool, not a marketing site. No Tailwind CSS dependency is introduced (Tailwind is not in the tech stack). State management: Redux Toolkit for auth/session state; local component state is fine for the practice-session form itself.

**Loading / Submission UX (required spec, not optional polish):** After the user submits a session response, the frontend transitions to a dedicated loading state that:
- Displays a spinner or animated progress bar with the message: *"Evaluating your reasoning — this usually takes up to 30 seconds."*
- Polls `GET /api/sessions/{id}` every **3 seconds** to check for status transition out of `ANALYZING`.
- On status `ANALYZED` → automatically navigates to the results page.
- On status `FAILED`, or if **35 seconds** elapse without a status change → displays: *"Evaluation failed. Your response has been saved — you can retry from your session history."* with a "Go to History" button.
- Never shows a blank screen or leaves the user without feedback on what is happening.

---

## 16. Database Design

```sql
users(id PK, name, email UNIQUE, password_hash, created_at)

interview_questions(
  id PK, category VARCHAR(50) NOT NULL,  -- e.g. 'DSA','SYSTEM_DESIGN','CONCEPTUAL'; VARCHAR not ENUM so new categories are additive (no ALTER TABLE required)
  title, prompt_text, difficulty ENUM('EASY','MEDIUM','HARD'),
  expected_concepts JSON,           -- internal rubric grounding, not shown to user
  created_at
)

practice_sessions(
  id PK, user_id FK -> users, question_id FK -> interview_questions,
  status ENUM('STARTED','RESPONSE_SUBMITTED','ANALYZING','ANALYZED',
              'FOLLOWUP_PENDING','FOLLOWUP_ANSWERED','COMPLETED','FAILED','ABANDONED'),
  failure_reason VARCHAR(255),          -- populated on FAILED; null otherwise; values: TIMEOUT | VALIDATION_FAILED | PROVIDER_ERROR
  started_at, completed_at
)

candidate_responses(
  id PK, session_id FK -> practice_sessions,
  response_type ENUM('APPROACH_EXPLANATION','FOLLOWUP_ANSWER'),
  content_text TEXT, submitted_at
)

code_submissions(
  id PK, session_id FK -> practice_sessions UNIQUE,  -- one submission per session enforced at DB level; re-submissions UPDATE the existing row
  language VARCHAR, code_text TEXT, submitted_at
)

analysis_results(
  id PK, session_id FK -> practice_sessions,
  analysis_type ENUM('MAIN','FOLLOWUP'),
  overall_score INT, category_scores JSON, strengths JSON, weaknesses JSON,
  missing_concepts JSON, incorrect_claims JSON, recommendations JSON,
  raw_ai_response JSON,             -- retained for debugging/audit, not the source of truth for display
  created_at
)

follow_up_questions(
  id PK, session_id FK -> practice_sessions, question_text, created_at
)

skill_metrics(
  id PK, user_id FK -> users, session_id FK -> practice_sessions,
  dimension VARCHAR,                -- e.g. "APPROACH_FORMULATION"
  category VARCHAR(50),             -- denormalized from the session's question category; enables efficient category-filtered trend queries without joins
  score INT, recorded_at
)
```

**Cardinality:** user 1—N sessions; session 1—N responses; session 1—1 (nullable) code submission for MVP (enforced via UNIQUE constraint on `session_id` in `code_submissions` — re-submissions UPDATE the existing row); session 1—N analysis_results (MAIN + FOLLOWUP); session 1—1 follow_up_question; session 1—N skill_metrics (one row per applicable dimension).

**Indexing:** index `practice_sessions(user_id, started_at)` for history queries; index `skill_metrics(user_id, category, dimension, recorded_at)` for category-filtered trend queries.

Do not add tenant/multi-org complexity, soft-delete, or versioning for MVP — genuinely not needed at this scale.

---

## 17. API Specification (representative)

```
POST   /api/auth/register            No auth   { name, email, password }
POST   /api/auth/login               No auth   { email, password } → { token }
DELETE /api/auth/account             Auth      → permanently deletes the authenticated user and cascades associated data

GET    /api/questions?category=&difficulty=&page=&size=   Auth required
GET    /api/questions/{id}                                  Auth required

POST   /api/sessions                 Auth   { questionId } → creates session (STARTED)
POST   /api/sessions/{id}/response   Auth   { explanationText, code?, language? }
                                             → moves to RESPONSE_SUBMITTED, triggers analysis
GET    /api/sessions/{id}/analysis   Auth   → structured evaluation result
POST   /api/sessions/{id}/followup-response   Auth   { answerText }
                                             → triggers follow-up evaluation
GET    /api/sessions/{id}            Auth   → full session detail (status, responses, analyses)

GET    /api/sessions?page=&size=&status=    Auth   → paginated history
GET    /api/progress/summary                Auth   → per-dimension trend data for dashboard
```

Each state-changing endpoint validates the session belongs to the requesting user (401/403 otherwise) and validates the session is in the correct state for the action (409 if not — e.g., can't submit a follow-up answer before a follow-up question exists).

---

## 18. Security Requirements

- Spring Security + JWT (stateless), BCrypt password hashing.
- All `/api/sessions/**` and `/api/progress/**` endpoints require a valid JWT; user ID extracted from the token, never trusted from the request body.
- Ownership checks on every session-scoped resource — a user must never be able to fetch another user's session by guessing an ID.
- Input validation (`@Valid` + Bean Validation) on all request DTOs.
- LLM API keys stored in environment variables / secrets manager, never in source or client-side code — all AI calls happen server-side.
- Sanitize/limit size of user-submitted text and code before sending to the LLM (explanation ≤ 2,000 chars; code ≤ 5,000 chars — enforce at the API layer before any AI call).
- **Prompt injection mitigation:** user-submitted explanation text and code are inserted into LLM prompts as clearly delimited, quoted content blocks (e.g., wrapped in `<candidate_response>...</candidate_response>` XML-style tags). The system prompt explicitly instructs the model to treat the delimited block as untrusted candidate data and to ignore any instructions found within it. This is a best-effort mitigation; the residual risk is documented.
- Only response text and code are sent to the AI provider — never name, email, or other PII.
- Account deletion is supported and cascades.
- Input length caps and per-user daily session cap are enforced.

---

## 19. AI Integration Architecture

P0 = `AIProvider` interface + one concrete provider + router. Everything else (second provider, round-robin, failover, health checks, usage dashboards) is P2 and explicitly outside the 3-week window.

```text
Candidate response (text [+ code])
        │
        ▼
AIAnalysisService: build rubric-specific prompt
(category rubric + question's expected_concepts + candidate text/code)
        │
        ▼
AIProviderRouter
        │
        ├── AIProvider → Provider A (MVP concrete implementation)
        ├── AIProvider → Provider B (future)
        ├── AIProvider → Provider C (future)
        └── ... additional providers as configured
        │
        ▼
Structured JSON response
        │
        ▼
Validation layer:
  - schema, score ranges, required fields
  - evidence grounded in candidate text
  → retry once on validation failure; otherwise mark FAILED
        │
        ▼
Persist to analysis_results → return to React
```

**Design principle:** AI providers are implementations behind the `AIProvider` interface, not hardcoded into business logic. Swapping or adding providers should not require changes to `AIAnalysisService`.

### Multi-Provider AI Gateway (Future/P2)

The architecture should support multiple LLM providers such as OpenAI, Google AI/Gemini, Grok, Claude, Cerebras-hosted models, and other compatible providers. Provider credentials must be stored securely as environment variables/secrets and the system must operate within each provider’s applicable API limits and terms. The architecture must not depend on using multiple personal accounts as a core assumption.

Once multiple concrete providers are implemented, the router may support **round-robin request distribution**, failover, rate-limit awareness, health checks, and provider usage/latency tracking. For example:

```text
Request 1 → Provider A
Request 2 → Provider B
Request 3 → Provider C
Request 4 → Provider A
...
```

This is an architectural extension, not a blocker for the 3-week MVP.

---

## 20. Progress Tracking

- Every analysis writes one `skill_metrics` row per applicable dimension for that session, including a `category` field (denormalized from the question).
- The dashboard is **filtered by category** (DSA / System Design / Conceptual) — dimensions vary per category, so aggregating across categories produces meaningless mixed trend lines. The user selects a category tab; only sessions in that category feed the trend chart.
- Only dimensions that appear in ≥ **3 sessions** within the selected category are shown; dimensions with insufficient data remain hidden with a "keep practicing to unlock" placeholder specific to that dimension.
- The dashboard aggregates per-dimension trends within the selected category (e.g., last 10 DSA sessions' `APPROACH_FORMULATION` scores plotted as a line). `GET /api/progress/summary` accepts an optional `?category=` query param; the frontend passes the selected tab value.
- Keep aggregation simple for MVP: a straightforward average/trend query, not a forecasting or anomaly-detection layer — that complexity isn't justified at this stage and risks the timeline for no MVP-relevant value.

---

## 21. Non-Functional Requirements

- **Reliability:** every AI call has a **hard 30-second timeout**. If the call times out or returns a non-retryable error, the session transitions to `FAILED` with `failure_reason` set to `TIMEOUT`, `VALIDATION_FAILED`, or `PROVIDER_ERROR` as appropriate. Never leave a session stuck in `ANALYZING` indefinitely. The frontend loading state handles `FAILED` explicitly with a recovery message (see §15).
- **Performance:** synchronous AI calls are acceptable for MVP scale; if evaluation latency regularly exceeds ~10 seconds in production, the documented upgrade path is to make analysis async (return `202 Accepted`, poll for status). Not required for MVP — and the polling pattern already specified in §15 is forward-compatible with this upgrade without a frontend rewrite.
- **Consistency:** all API responses follow one envelope shape (`{ data, error }`) for predictable frontend handling.
- **Logging:** structured logs with a correlation ID per session, especially around the AI call boundary (log prompt metadata, not full user PII, and never log API keys).
- **Data privacy:** candidate responses are personal preparation data — no third-party sharing beyond the AI provider call itself; document this plainly for users.
- **Extensibility:** category rubrics, question categories (stored as `VARCHAR(50)` — not a rigid ENUM, see §16), and programming languages should all be additive (new row / new string value), not requiring schema migrations or core logic changes.
- **Session cleanup:** a scheduled job (Spring `@Scheduled`, runs daily) marks sessions stuck in `STARTED` for > 24 hours as `ABANDONED`. Abandoned sessions are excluded from daily cap counts and shown in history as "Incomplete" rather than in the active session queue.
- **Known limitations (document in repo README):** no email verification in MVP (daily cap is the primary abuse guard — known bypass risk via re-registration); no password-reset flow in MVP (users who lose their password lose access to their history — document plainly and plan as an early post-MVP addition).

---

## 22. 3-Week Development Roadmap

Target is 3 weeks. DSA-only reliable loop is the real floor. Weeks 4+ are treated as expected buffer, not failure.

**Week 1 — Foundation**
- Spring Boot + MySQL + JPA
- Spring Security + JWT + ownership checks + account deletion
- Question entity + 15–20 seed questions
- Practice session lifecycle (state machine + idempotency)
- Basic React scaffold + auth pages

**Week 2 — Core practice flow**
- Question selection + practice session pages
- Text + optional code submission
- `AIAnalysisService` with structured output for **DSA only**
- Evidence substring verification
- Results page
- Golden test set created and first regression run

**Week 3 — Breadth, follow-ups, progress, polish**
- System Design + Conceptual rubrics (if time)
- Follow-up question + evaluation
- Progress dashboard (with ≥3-session gate)
- History pages
- Daily cap + length caps
- Error handling, FAILED state, loading UX
- Deployment + README

**Cut order (strict):** System Design/Conceptual → follow-up → progress charts → polish.
**Never cut:** evidence-based feedback structure + evidence verification.

---

## 23. MVP Acceptance Criteria

A new user can, reliably and end-to-end:
1. Register and log in.
2. Select a DSA question.
3. Start a session, type an explanation, optionally submit code.
4. Submit and receive AI-generated, evidence-based reasoning feedback with per-dimension scores (evidence verified as real substrings/near-substrings).
5. Receive and answer one contextual follow-up, and see it evaluated.
6. See the completed session in history.
7. See a basic progress view once ≥ 3 sessions exist (placeholder before that).

If this works reliably for DSA, the MVP is successful.

---

## 24. Testing Strategy

- Unit tests for service-layer logic, especially session state transitions, authorization/ownership checks, structured AI-output validation, evidence verification, daily usage limits, and account-deletion behavior.
- Integration tests for the core session lifecycle using Testcontainers with MySQL where practical.
- Maintain a small golden test set of good, mediocre, and weak sample answers; run it as a regression check whenever prompts or rubrics change. This catches obvious evaluation regressions but is not treated as scientific validation of the model.
- Mock the `AIProvider` in automated tests and CI — never make real LLM API calls in automated tests.
- Run a manual end-to-end pass through the full MVP acceptance criteria before considering the MVP complete.

---
## 25. Deployment Considerations

Deployment platform intentionally left open per your instruction — decide later based on cost/simplicity. Whatever you choose, keep these constraints in mind: environment-based config for the LLM API key and DB credentials (never committed), a single deployable backend artifact (JAR + Docker image), and a static-hosted React build. Don't over-invest in deployment infrastructure before the core loop (Sections 9–19) is solid — a working local Docker Compose setup is sufficient through most of the 3 weeks.

---

## 26. Project Repository Structure

```
technical-interview-coach/
├── backend/
│   └── src/main/java/com/interviewcoach/
│       ├── controller/ service/ repository/ entity/ dto/
│       ├── security/ exception/ config/ integration/
├── frontend/
│   └── src/ (pages/, components/, features/, app/)
├── docs/
│   ├── architecture-diagram.png
│   ├── er-diagram.png
│   ├── api-documentation.md
│   └── ai-evaluation-methodology.md
├── screenshots/
├── README.md
└── .gitignore
```

---

## 27. Documentation Requirements

The repo should document: problem statement and product goals (condensed from this PRD), architecture diagram, database ER diagram, API documentation (endpoint list + sample request/response), the AI evaluation methodology (the rubrics from Section 11, stated plainly — this is your strongest interview talking point, document it well), setup instructions and required environment variables, testing strategy, deployment instructions, known limitations (be honest — e.g., "MVP evaluates typed explanations only; voice input is a planned extension"), and future improvements.

---

## 28. Future Scope

Explicitly deferred beyond MVP: real-time/live interview assistance, video analysis, eye tracking, emotion detection, advanced speech-delivery coaching, custom ML models, real-time collaborative interviews, peer-to-peer mock interviews, recruiter integration, resume builder, job application tracking, large-scale question marketplace, code execution/judging infrastructure, mobile app, and HR/behavioral round evaluation (a deliberately separate product decision, not an MVP gap).

Reasonable near-term P1/P2 additions once the MVP is validated: voice input + speech-to-text, difficulty-adaptive question selection, user-pasted custom questions, and eventually AI-generated questions (only once the curated bank's rubric-grounding approach is proven reliable).

---

## 29. Resume / Technical Value

This project demonstrates, concretely:
- REST API design with Spring Boot, layered architecture (controller/service/repository/DTO), and consistent error handling via `@ControllerAdvice`.
- Spring Security + JWT authentication with per-resource ownership authorization (not just "logged in," but "logged in *and* owns this resource").
- JPA/Hibernate relational modeling across a genuinely non-trivial entity graph (sessions, responses, analyses, follow-ups, skill metrics) with real cardinality decisions to defend.
- **Structured AI output integration and evidence verification** — using Spring AI to constrain and validate LLM responses against a defined schema, with retry/failure handling for malformed output and a programmatic check that feedback evidence actually appears in the candidate's submitted response. This separates the project from simply calling an LLM API: the evaluation rubric, evidence-based feedback requirement, validation layer, and hallucination guard are deliberate engineering decisions you can explain and defend.
- A defensible, narrow product thesis you can state precisely and back with a specific competitive gap (Section 2), rather than a vague "AI-powered X" pitch.

---

## 30. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| **3-week timeline is aggressive** given you're still learning React. | Follow the roadmap's cut-order strictly (Section 22). Ship DSA-only if needed — a reliable single-category loop beats three shallow, buggy ones. |
| **LLM output is unreliable or malformed**, since evaluation quality is the core product risk. | Use a structured output schema, validation layer, one retry where appropriate, and an explicit `FAILED` state. Never silently display malformed data. Maintain a golden test set and regression-check prompts/rubrics before relying on changes. |
| **AI may invent evidence** that was not present in the candidate's response. | Programmatically verify every evidence string against the submitted text using exact or near-substring matching. Discard or flag unverified evidence instead of presenting it as fact. |
| **Evaluation quality is difficult to validate** with limited time. | Constrain the MVP question bank to 15–20 hand-written questions with hand-defined expected reasoning checkpoints, and use the golden test set to catch obvious regressions. |
| **Voice input adds real complexity** (recording, transcription accuracy, noise handling) that could derail the timeline. | Deliberately defer voice input to P1. The MVP uses typed explanations only. |
| **Scope creep toward a generic interview coach** (delivery, behavioral, resume, recruiter features). | Keep the product focused on technical reasoning. Explicitly defer excluded features to Section 28 and follow the strict roadmap cut-order. |
| **AI provider outage, rate limits, or unexpected cost.** | Keep AI integration behind the `AIProvider` interface, enforce per-user daily session and input-length caps, store credentials only in server-side secrets, and keep a second-provider/failover path as a post-MVP extension. |
| **Privacy risk from sending candidate data to external AI providers.** | Send only the minimum response text/code required for evaluation; never send name, email, or other unnecessary PII. Document the AI-provider data flow clearly and support account deletion with cascading data removal. |
| **Progress trends may be noisy with very little data.** | Hide trend charts until the user has at least 3 completed sessions and show a clear placeholder beforehand. Filter trends by category to avoid mixing incompatible dimensions. Keep aggregation simple for MVP. |
| **No email verification — daily cap can be bypassed by re-registering.** | Acknowledge as a known MVP limitation. Document it plainly. Plan email verification as the first post-MVP auth addition. |
| **No password reset flow in MVP.** | Document plainly: users who lose their password lose access to their session history. Plan as an early post-MVP addition. |
| **Unexpected LLM cost spike** from usage volume or oversized prompts. | Per-user daily session cap (10/day) + explicit input length caps (explanation ≤ 2,000 chars, code ≤ 5,000 chars) are the primary guards. Set a hard monthly spend ceiling at the API key level (provider dashboard). Monitor actual per-session token usage in the first week after launch. |
| **Loading UX dead zone — users may think the app is broken during AI evaluation.** | The frontend loading page must show a spinner, an estimated wait message ("up to 30 seconds"), and handle the `FAILED` status with a concrete recovery action (see §15). Never leave a blank or unresponsive screen. |


---

## 31. Final MVP Definition

**A logged-in user selects a DSA question, types their approach explanation, optionally submits code, and receives structured, evidence-based feedback across defined reasoning dimensions (with evidence verified as real substrings/near-substrings of their text) — followed by one AI-generated follow-up question that is itself evaluated — with the session saved to history and reflected in a basic per-dimension progress view once sufficient data exists.**

System Design and Conceptual rubrics are strong outcomes if they ship, not requirements. A fully working narrow product beats a partially working broad one.

---