# API Documentation — Technical Interview Reasoning Coach

All API endpoints follow a standard JSON response wrapper:
```json
{
  "data": { ... },
  "error": "Error message if any"
}
```

---

## Authentication Endpoints

### 1. Register User
- **POST** `/api/auth/register`
- **Auth:** None
- **Request Body:**
  ```json
  {
    "name": "Candidate Name",
    "email": "user@example.com",
    "password": "Password123!",
    "targetRole": "Software Engineer"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "data": {
      "token": "<JWT_TOKEN>"
    },
    "error": ""
  }
  ```

### 2. Login
- **POST** `/api/auth/login`
- **Auth:** None
- **Request Body:**
  ```json
  {
    "email": "user@example.com",
    "password": "Password123!"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "data": {
      "token": "<JWT_TOKEN>"
    },
    "error": ""
  }
  ```

### 3. Delete Account
- **DELETE** `/api/auth/account`
- **Auth:** Bearer JWT
- **Response (200 OK):** Cascades deletion across all practice sessions, responses, analysis results, and skill metrics.

---

## Question Bank Endpoints

### 1. List Questions
- **GET** `/api/questions`
- **Auth:** Bearer JWT
- **Query Parameters:** `category` (DSA | SYSTEM_DESIGN | CONCEPTUAL), `difficulty` (EASY | MEDIUM | HARD)

### 2. Get Question Detail
- **GET** `/api/questions/{id}`
- **Auth:** Bearer JWT

---

## Session Lifecycle Endpoints

### 1. Create Practice Session
- **POST** `/api/sessions`
- **Auth:** Bearer JWT
- **Request Body:** `{ "questionId": 1 }`

### 2. Submit Main Response
- **POST** `/api/sessions/{id}/response`
- **Auth:** Bearer JWT
- **Request Body:**
  ```json
  {
    "explanationText": "Detailed approach explanation...",
    "code": "optional code text",
    "language": "java"
  }
  ```

### 3. Get Analysis Results
- **GET** `/api/sessions/{id}/analysis?type=MAIN`
- **Auth:** Bearer JWT

### 4. Get Follow-Up Question
- **GET** `/api/sessions/{id}/followup`
- **Auth:** Bearer JWT

### 5. Submit Follow-Up Response
- **POST** `/api/sessions/{id}/followup-response`
- **Auth:** Bearer JWT
- **Request Body:** `{ "answerText": "Follow up answer..." }`

### 6. Session History
- **GET** `/api/sessions?page=0&size=15`
- **Auth:** Bearer JWT

---

## Progress Dashboard Endpoints

### 1. Progress Summary
- **GET** `/api/progress/summary?category=DSA`
- **Auth:** Bearer JWT
