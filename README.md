# Intensify — Technical Interview Reasoning Coach

> A platform that evaluates **technical reasoning** during interview practice — not just whether you solved the problem, but whether you *explained your thinking, justified your choices, and discussed trade-offs*.

---

## Project Structure

```
Intensify/
├── backend/          ← Spring Boot 4.1 (Java 21)
├── frontend/         ← React.js (Week 2)
├── docs/             ← Architecture, ER diagram, API docs
└── PRD.md            ← Product Requirements Document
```

---

## Backend Setup (Week 1 — Active)

### Prerequisites
- Java 21
- MySQL 8+ running locally
- An OpenAI API key (or any compatible provider)

### Environment Variables
Create a `.env` or set these in your IDE run configuration:

```
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
JWT_SECRET=<any-256+-bit-random-string>
OPENAI_API_KEY=sk-...
```

### Run Locally
```bash
cd backend
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.  
MySQL database `intensify_db` is created automatically on first run.  
The question bank (45 questions) is seeded automatically if the DB is empty.

---

## API Overview

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | None | Register new user |
| POST | `/api/auth/login` | None | Login, get JWT |
| DELETE | `/api/auth/account` | JWT | Delete account + all data |
| GET | `/api/questions` | JWT | List questions (filter by `category`, `difficulty`) |
| GET | `/api/questions/{id}` | JWT | Get question detail |
| POST | `/api/sessions` | JWT | Start a practice session |
| POST | `/api/sessions/{id}/response` | JWT | Submit explanation (+ optional code) |
| GET | `/api/sessions/{id}/analysis` | JWT | Get AI evaluation result |
| GET | `/api/sessions/{id}/followup` | JWT | Get follow-up question |
| POST | `/api/sessions/{id}/followup-response` | JWT | Submit follow-up answer |
| GET | `/api/sessions/{id}` | JWT | Get session status |
| GET | `/api/sessions` | JWT | List session history |
| GET | `/api/progress/summary?category=DSA` | JWT | Progress trend data |

All responses follow the envelope: `{ "data": ..., "error": "" }`

---

## Architecture

```
React SPA ──HTTPS/JWT──▶ Spring Boot Monolith
                              │
                ┌─────────────┼──────────────────┐
                ▼             ▼                   ▼
           [MySQL]    [AIAnalysisService]   [OpenAI API]
                      (30s timeout, retry,  
                       evidence verification,
                       structured JSON output)
```

---

## Key Design Decisions

- **Evidence Verification (PRD §11.7):** Every AI-generated feedback item is verified against the candidate's actual text using exact substring matching (case-insensitive) with an 85% Levenshtein similarity fallback. Unverified evidence is discarded silently — never shown to the user.
- **Deterministic Scoring (PRD §11.5):** `overallScore` is always recomputed by the backend as the mean of applicable dimension scores. The AI's returned value is a hint only.
- **Prompt Injection Mitigation (PRD §18):** Candidate text is wrapped in `<candidate_response>` XML tags with an explicit system-prompt instruction to ignore any instructions found inside.
- **FAILED State:** Sessions that fail AI evaluation transition to `FAILED` with a `failure_reason` (`TIMEOUT` | `VALIDATION_FAILED` | `PROVIDER_ERROR`). Users see a recovery message on the frontend.

---

## Known Limitations (MVP)

- No email verification — daily cap (10 sessions/day) is the primary abuse guard.
- No password reset flow — document and plan as early post-MVP.
- AI evaluation uses typed explanations only; voice input is planned for P1.
- Code is not executed/judged — consistency check only.

---

## Development Roadmap

| Week | Focus |
|------|-------|
| **Week 1** | ✅ Backend foundation: entities, security (JWT), question seeder, session lifecycle, AI integration |
| **Week 2** | Core practice flow + React frontend |
| **Week 3** | Follow-up, progress dashboard, polish, deployment |
