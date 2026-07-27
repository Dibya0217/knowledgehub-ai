# KnowledgeHub AI — Master Build Plan

## Context

Portfolio project demonstrating enterprise-grade Java GenAI backend skills.
Target roles: Java GenAI backend engineer.
**Current state:** Spring Boot 4.1.0 + Java 25 lives in `backend/` subfolder. Frontend will be in `frontend/` (Phase 6).
**Time budget:** 1–1.5 hrs/day weekdays, 2–4 hrs/day weekends (~12 hrs/week).
**Estimated completion:** ~13 weeks from start.

---

## Architecture Overview

```
React Frontend
     │
REST API + SSE (/api/v1/*)
     │
Spring Boot Application (Java 25)
     │
┌────────────────────────────────────────┐
│  Auth  │  Chat Service  │  Doc Service │
└────────────────────────────────────────┘
     │           │               │
PostgreSQL   Spring AI       Apache Tika
           (ChatClient)      + Chunker
                │               │
         ┌──────┴──────┐     Qdrant
         │      │      │        │
      OpenAI Gemini Ollama   Embedding
         └──────┬──────┘     (Configurable)
                │
            AI Response
                │
              Redis
           (Chat Memory
            + Cache
            + JWT Blacklist)
```

## LLM Strategy

Provider selected via config — zero code change to switch.

| Environment | Provider | Cost |
|---|---|---|
| Local dev | Ollama | Free |
| Dev/Demo | Gemini | Low |
| Production | OpenAI | Paid |

```yaml
# application-local.yml
app:
  ai:
    provider: ollama
    embedding-provider: ollama
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| AI | Spring AI (ChatClient abstraction) |
| Vector DB | Qdrant |
| LLM | OpenAI / Gemini / Ollama (pluggable) |
| Database | PostgreSQL |
| Cache | Redis |
| Parsing | Apache Tika, PDFBox, Apache POI |
| Migrations | Flyway |
| Mapping | MapStruct |
| Security | Spring Security + JWT + OAuth2 (Google) |
| Observability | Actuator + Micrometer + Prometheus + Grafana |
| Tracing | OpenTelemetry + correlation IDs |
| Frontend | React + TypeScript + Vite + Material UI |
| Containers | Docker + Docker Compose |
| CI/CD | GitHub Actions |
| Cloud | AWS EC2 + S3 |

---

## Repository Structure (Target)

```
knowledgehub-ai/
├── backend/                      ← Spring Boot app
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   └── .mvn/
├── frontend/                     ← React app (Phase 6)
├── docs/
│   ├── PLAN.md                   ← this file
│   ├── Architecture.md
│   ├── API.md
│   ├── Database.md
│   ├── Security.md
│   ├── RAG.md
│   └── Deployment.md
├── docker/
│   ├── postgres/
│   ├── redis/
│   ├── qdrant/
│   ├── prometheus/
│   └── grafana/
├── .github/workflows/
├── scripts/
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## Backend Package Structure (Target)

```
com.dibya.knowledgehub
├── config/                    ← AppConfig, WebConfig, AsyncConfig
├── common/
│   ├── constants/
│   ├── enums/
│   ├── util/
│   ├── response/              ← ApiResponse<T>, PagedResponse<T>
│   └── helper/
├── exception/                 ← GlobalExceptionHandler (RFC 7807)
│   ├── GlobalExceptionHandler
│   ├── ErrorResponse
│   └── custom exceptions
├── security/
│   ├── config/                ← SecurityConfig
│   ├── jwt/                   ← JwtService, JwtFilter
│   ├── filter/
│   ├── service/               ← UserDetailsServiceImpl
│   ├── handler/
│   └── permission/
├── auth/
│   ├── controller/
│   ├── dto/
│   ├── entity/                ← RefreshToken
│   ├── repository/
│   └── service/
├── user/
│   ├── controller/
│   ├── dto/
│   ├── entity/                ← User
│   ├── repository/
│   └── service/
├── role/                      ← Role entity, enum ROLE_ADMIN / ROLE_USER
├── document/
│   ├── controller/
│   ├── dto/
│   ├── entity/                ← Document, DocumentMetadata
│   ├── repository/
│   └── service/
├── parser/                    ← DocumentParser interface + impls
│   ├── PdfParser
│   ├── DocxParser
│   ├── CsvParser
│   ├── ExcelParser
│   ├── JsonParser
│   └── MarkdownParser
├── chunk/                     ← ChunkingService, TextChunk
├── embedding/                 ← EmbeddingService (Spring AI abstraction)
├── vector/                    ← VectorStoreService (Qdrant)
├── rag/                       ← RagService — orchestrates retrieval
├── prompt/                    ← PromptBuilder
├── ai/                        ← AiProviderConfig (OpenAI/Gemini/Ollama beans)
├── llm/                       ← LlmService wrapping ChatClient
├── memory/                    ← ConversationMemoryService (Redis)
├── conversation/              ← Conversation, Message entities
├── citation/                  ← CitationExtractor
├── chat/
│   ├── controller/
│   ├── dto/
│   └── service/
├── storage/                   ← FileStorageService (local → S3)
├── cache/                     ← CacheService
├── monitoring/                ← custom metrics
├── audit/                     ← AuditLog entity + service
├── scheduler/
├── notification/
├── mapper/                    ← MapStruct mappers
├── validation/                ← custom validators
└── KnowledgeHubApplication.java
```

---

## Database Schema

### PostgreSQL Tables

```sql
users (id, email, password_hash, name, provider, enabled, email_verified, created_at, updated_at)
roles (id, name)
user_roles (user_id, role_id)
refresh_tokens (id, token, user_id, expires_at, revoked, created_at)
documents (id, user_id, filename, file_type, file_size, storage_path, status, created_at)
document_metadata (id, document_id, page_count, word_count, language, title, author)
conversations (id, user_id, title, created_at, updated_at)
messages (id, conversation_id, role, content, created_at)
message_citations (id, message_id, document_id, chunk_id, page_number, excerpt)
audit_logs (id, user_id, action, resource, resource_id, ip_address, timestamp)
settings (id, user_id, key, value, updated_at)
```

### Qdrant Collection: `knowledge_vectors`

```json
{
  "documentId": "uuid",
  "chunkId": "uuid",
  "userId": "uuid",
  "filename": "string",
  "page": 2,
  "text": "chunk text content",
  "metadata": {}
}
```

### Redis Keys

```
jwt:blacklist:{token_hash}         TTL = token expiry
chat:memory:{conversation_id}      TTL = 1 hour
cache:user:{user_id}               TTL = 5 min
rate:limit:{user_id}:{endpoint}    TTL = 1 min
otp:{email}                        TTL = 5 min
```

---

## RAG Pipeline Detail

```
1. Upload file
2. Validate (type, size, virus scan placeholder)
3. Store raw file → local/S3
4. Extract text → Apache Tika / specific parser
5. Clean text (strip noise, normalize whitespace)
6. Chunk (RecursiveCharacterSplitter, size=800, overlap=150)
7. Attach metadata (documentId, userId, filename, page, chunkId)
8. Generate embeddings → Spring AI EmbeddingModel (configurable)
9. Store vectors → Qdrant
10. Mark document status = READY

--- Query Flow ---
11. User sends question
12. Generate question embedding
13. Qdrant similarity search (top-K=5, threshold=0.75)
14. Optionally filter by userId (per-user isolation)
15. Build prompt: system + context chunks + conversation history + question
16. Call Spring AI ChatClient (→ active provider)
17. Stream response via SSE
18. Extract citations from retrieved chunks
19. Persist message + citations
20. Update Redis conversation memory
```

---

## API Design

All endpoints under `/api/v1/`

### Auth
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password
GET    /api/v1/auth/verify-email?token=
GET    /api/v1/oauth2/callback/google
```

### Documents
```
POST   /api/v1/documents/upload
GET    /api/v1/documents
GET    /api/v1/documents/{id}
DELETE /api/v1/documents/{id}
GET    /api/v1/documents/{id}/status
```

### Chat
```
POST   /api/v1/chat
GET    /api/v1/chat/stream          (SSE)
GET    /api/v1/chat/history
GET    /api/v1/chat/history/{conversationId}
DELETE /api/v1/chat/history/{conversationId}
```

### Users
```
GET    /api/v1/users/me
PUT    /api/v1/users/me
GET    /api/v1/users/settings
PUT    /api/v1/users/settings
```

### Admin
```
GET    /api/v1/admin/users
DELETE /api/v1/admin/users/{id}
GET    /api/v1/admin/documents
GET    /api/v1/admin/statistics
```

### Observability
```
GET    /actuator/health
GET    /actuator/metrics
GET    /actuator/prometheus
```

---

## Security Checklist

- [x] JWT access token (15 min expiry)
- [x] Refresh token rotation
- [x] JWT blacklist in Redis on logout
- [x] BCrypt password hashing
- [x] Google OAuth2 login
- [x] RBAC (ROLE_USER, ROLE_ADMIN)
- [x] CORS configured
- [x] Rate limiting (Resilience4j / Redis)
- [x] Security headers (CSP, HSTS, X-Frame-Options)
- [x] Input validation (@Valid everywhere)
- [x] Global exception handler — no stack traces to client
- [x] SQL injection protection via JPA
- [x] Per-user document isolation in Qdrant queries
- [x] Audit logging for sensitive actions
- [x] File upload validation (type, size)

---

## Day-by-Day Build Schedule

> **Legend:** WD = weekday (1–1.5 hrs) | WE = weekend day (2–4 hrs)
> Start date: Week 1 = Day 1

---

### PHASE 1 — Foundation (Week 1–2)

**Goal:** App boots clean, logging works, errors handled consistently, Docker up.

#### Week 1

| Day | Type | Task |
|-----|------|------|
| 1 Mon | WD | ✅ Restructure pom.xml — add missing deps (Flyway, MapStruct, Tika, OpenAPI, Resilience4j, JWT, Redis) |
| 2 Tue | WD | ✅ Create `application.yml`, `application-local.yml`, `application-dev.yml`, `application-prod.yml` profiles |
| 3 Wed | WD | ✅ Setup Logback (`logback-spring.xml`) — JSON logs + correlation ID MDC filter |
| 4 Thu | WD | ✅ Create `ApiResponse<T>` wrapper, `PagedResponse<T>`, `ErrorResponse` (RFC 7807 Problem Details) |
| 5 Fri | WD | ✅ `GlobalExceptionHandler` — handle validation, not found, unauthorized, generic 500 |
| 6 Sat | WE | ✅ Custom exception classes (`ResourceNotFoundException`, `ConflictException`, `UnauthorizedException`, etc.) |
| 7 Sun | WE | ✅ `docker-compose.yml` — PostgreSQL + Redis + Qdrant containers. Verify app connects |

#### Week 2

| Day | Type | Task |
|-----|------|------|
| 8 Mon | WD | Flyway setup — `V1__create_users_roles.sql` migration |
| 9 Tue | WD | `AppConfig` — virtual threads executor, async config, Jackson config |
| 10 Wed | WD | OpenAPI / Swagger config — Swagger UI accessible at `/swagger-ui.html` |
| 11 Thu | WD | `banner.txt`, app name/version in properties, health actuator endpoint smoke test |
| 12 Fri | WD | `RequestLoggingFilter` — log method, path, status, duration, correlation ID |
| 13 Sat | WE | `AuditLog` entity + `AuditService` stub. Write `README.md` skeleton with badges |
| 14 Sun | WE | **Phase 1 review:** app starts, Swagger loads, logs structured, DB connected, Docker Compose works |

---

### PHASE 2 — Authentication & Security (Week 3–4)

**Goal:** Register, login, refresh token, Google OAuth2, RBAC all working end-to-end.

#### Week 3

| Day | Type | Task |
|-----|------|------|
| 15 Mon | WD | `Role` entity + enum. `User` entity with roles (ManyToMany). `V2__create_auth_tables.sql` |
| 16 Tue | WD | `UserRepository`, `RoleRepository`. Seed roles on startup (`ApplicationRunner`) |
| 17 Wed | WD | `JwtService` — generate access token, refresh token, validate, extract claims |
| 18 Thu | WD | `JwtAuthFilter` — OncePerRequestFilter, set SecurityContext |
| 19 Fri | WD | `SecurityConfig` — permit `/auth/**`, `/oauth2/**`, `/actuator/health`. Protect rest |
| 20 Sat | WE | `AuthController` — `POST /auth/register` + `POST /auth/login`. Return access + refresh tokens |
| 21 Sun | WE | `RefreshToken` entity + `V3__create_refresh_tokens.sql`. `POST /auth/refresh` endpoint |

#### Week 4

| Day | Type | Task |
|-----|------|------|
| 22 Mon | WD | `POST /auth/logout` — blacklist access token in Redis, revoke refresh token |
| 23 Tue | WD | Google OAuth2 config in `application.yml`. `OAuth2SuccessHandler` — issue JWT on OAuth2 login |
| 24 Wed | WD | `POST /auth/forgot-password` — generate OTP, store in Redis, return 200 (stub email) |
| 25 Thu | WD | `POST /auth/reset-password` — validate OTP, update password hash |
| 26 Fri | WD | `UserDetailsServiceImpl`, `UserController` — `GET /users/me`, `PUT /users/me` |
| 27 Sat | WE | Rate limiting with Resilience4j or Redis counter on `/auth/login` (max 5/min per IP) |
| 28 Sun | WE | **Phase 2 review:** Postman test full auth flow. JWT works. Google login works. Rate limit triggers |

---

### PHASE 3 — Document Upload & Parsing (Week 5–6)

**Goal:** Upload PDF/DOCX/CSV/Excel/JSON/MD, extract text, chunk, store in DB.

#### Week 5

| Day | Type | Task |
|-----|------|------|
| 29 Mon | WD | `Document` entity + `DocumentMetadata` entity. `V4__create_documents.sql` |
| 30 Tue | WD | `FileStorageService` — save file to `/uploads/{userId}/{documentId}/` (local first) |
| 31 Wed | WD | `DocumentParser` interface. `PdfParser` using Apache PDFBox (text + page tracking) |
| 32 Thu | WD | `DocxParser` using Apache POI. `TxtParser` using plain reader |
| 33 Fri | WD | `CsvParser`, `ExcelParser` using Apache POI |
| 34 Sat | WE | `JsonParser`, `MarkdownParser`. `TikaFallbackParser` for unknown types |
| 35 Sun | WE | `ParserFactory` — select correct parser by MIME type. Unit test each parser |

#### Week 6

| Day | Type | Task |
|-----|------|------|
| 36 Mon | WD | `TextChunk` value object. `ChunkingService` — recursive character splitter (size=800, overlap=150) |
| 37 Tue | WD | Attach metadata to each chunk (documentId, userId, filename, page, chunkId, timestamp) |
| 38 Wed | WD | `DocumentController` — `POST /documents/upload` (multipart). Validate file type + size |
| 39 Thu | WD | `DocumentService` — async upload pipeline: store → parse → chunk → save chunks to DB placeholder |
| 40 Fri | WD | `GET /documents`, `GET /documents/{id}`, `DELETE /documents/{id}` |
| 41 Sat | WE | Document status enum (`PENDING`, `PROCESSING`, `READY`, `FAILED`). `GET /documents/{id}/status` |
| 42 Sun | WE | **Phase 3 review:** upload PDF, see status go PENDING→PROCESSING→READY. Chunks saved to DB |

---

### PHASE 4 — Spring AI, Embeddings, Qdrant, RAG (Week 7–8)

**Goal:** Embed document chunks, store in Qdrant, retrieve relevant context for a query.

#### Week 7

| Day | Type | Task |
|-----|------|------|
| 43 Mon | WD | Add Spring AI BOM + dependencies (spring-ai-openai, spring-ai-vertex-ai-gemini, spring-ai-ollama) |
| 44 Tue | WD | `AiProviderConfig` — conditional beans: create `ChatModel` and `EmbeddingModel` based on `app.ai.provider` |
| 45 Wed | WD | Configure Qdrant Docker container + `VectorStoreConfig` using Spring AI Qdrant integration |
| 46 Thu | WD | `EmbeddingService` — `List<float[]> embed(List<String> texts)` wrapping Spring AI `EmbeddingModel` |
| 47 Fri | WD | `VectorStoreService` — `upsert(chunks)`, `search(queryEmbedding, userId, topK)`, `delete(documentId)` |
| 48 Sat | WE | Wire embedding into document pipeline: after chunking → embed → upsert to Qdrant |
| 49 Sun | WE | Test: upload document, verify vectors appear in Qdrant (via Qdrant dashboard on port 6333) |

#### Week 8

| Day | Type | Task |
|-----|------|------|
| 50 Mon | WD | `PromptBuilder` — system prompt template + inject retrieved context chunks + conversation history |
| 51 Tue | WD | `LlmService` — `call(prompt): String` and `stream(prompt): Flux<String>` via Spring AI `ChatClient` |
| 52 Wed | WD | `RagService` — `retrieve(question, userId): List<TextChunk>` orchestrates embed → search |
| 53 Thu | WD | Wire: `RagService` → `PromptBuilder` → `LlmService` = full RAG query pipeline |
| 54 Fri | WD | Add similarity threshold filter (discard chunks below 0.75 score). Add hybrid search option |
| 55 Sat | WE | `prompts/system-prompt.st` — Spring AI `PromptTemplate`. Externalize prompt text |
| 56 Sun | WE | **Phase 4 review:** ask question in test, get AI answer backed by document chunks |

---

### PHASE 5 — Chat, Streaming, Memory, Citations (Week 9–10)

**Goal:** Full chat API with streaming SSE, conversation history, Redis memory, source citations.

#### Week 9

| Day | Type | Task |
|-----|------|------|
| 57 Mon | WD | `Conversation` entity + `Message` entity + `MessageCitation` entity. `V5__create_conversations.sql` |
| 58 Tue | WD | `ConversationRepository`, `MessageRepository`. `ConversationService` CRUD |
| 59 Wed | WD | `ConversationMemoryService` — store last N messages in Redis per conversation. Auto-expire |
| 60 Thu | WD | `ChatController` — `POST /chat` (blocking response). Wire full pipeline end-to-end |
| 61 Fri | WD | `GET /chat/stream` — `text/event-stream` SSE endpoint. `Flux<String>` from `LlmService.stream()` |
| 62 Sat | WE | `CitationExtractor` — map retrieved chunks back to response. Return `List<CitationDto>` |
| 63 Sun | WE | Persist `MessageCitation` rows when message saved |

#### Week 10

| Day | Type | Task |
|-----|------|------|
| 64 Mon | WD | `GET /chat/history` — paginated conversation list for current user |
| 65 Tue | WD | `GET /chat/history/{conversationId}` — messages with citations |
| 66 Wed | WD | `DELETE /chat/history/{conversationId}` — delete conversation + messages + Redis memory |
| 67 Thu | WD | Token count estimator — trim conversation memory if approaching LLM context window limit |
| 68 Fri | WD | Error handling for LLM failures — fallback message, retry with Resilience4j |
| 69 Sat | WE | Integration test: upload doc → ask question → verify streamed response + citations returned |
| 70 Sun | WE | **Phase 5 review:** full backend MVP complete. All endpoints manually tested via Postman |

---

### PHASE 6 — React Frontend (Week 11–12)

**Goal:** Functional UI — auth, document upload, streaming chat, history, admin panel.

#### Week 11

| Day | Type | Task |
|-----|------|------|
| 71 Mon | WD | `frontend/` scaffold with Vite + React + TypeScript. Install MUI, TanStack Query, Axios, React Router, React Hook Form, Zod |
| 72 Tue | WD | Axios instance with JWT interceptor (auto-attach token, handle 401 refresh) |
| 73 Wed | WD | Auth pages: Login, Register, Forgot Password. React Hook Form + Zod validation |
| 74 Thu | WD | `AuthContext` — store tokens, user info. Protected routes |
| 75 Fri | WD | Layout: sidebar nav, top bar, theme toggle (dark/light) |
| 76 Sat | WE | Chat page: message list, input box, send button. Connect to `POST /chat` |
| 77 Sun | WE | Streaming: connect to SSE `/chat/stream`. Render tokens as they arrive. Typing animation |

#### Week 12

| Day | Type | Task |
|-----|------|------|
| 78 Mon | WD | Document upload page: React Dropzone, upload progress, status polling |
| 79 Tue | WD | Document list page: table with filename, status, delete button |
| 80 Wed | WD | Citations display: below each AI response, show source filename + page |
| 81 Thu | WD | Conversation history sidebar — list conversations, click to load messages |
| 82 Fri | WD | Profile / Settings page — update name, provider preference, theme |
| 83 Sat | WE | Admin dashboard: users table, document stats, Recharts usage graph |
| 84 Sun | WE | **Phase 6 review:** test full flow in browser — register, upload PDF, chat, see citations |

---

### PHASE 7 — Redis Caching, Monitoring, Security Hardening (Week 13)

**Goal:** Production-ready observability, caching, rate limiting, audit logs complete.

| Day | Type | Task |
|-----|------|------|
| 85 Mon | WD | Spring Cache on hot endpoints (`/users/me`, document list). Redis as cache provider |
| 86 Tue | WD | Prometheus config. Add custom Micrometer metrics: chat requests/min, embedding latency, RAG latency |
| 87 Wed | WD | Grafana dashboard — import Spring Boot dashboard + add custom panels |
| 88 Thu | WD | OpenTelemetry tracing setup — `traceId` in every log line, propagate to MDC |
| 89 Fri | WD | Security headers filter: CSP, HSTS, X-Frame-Options, X-Content-Type-Options |
| 90 Sat | WE | Complete `AuditService` — log document upload, delete, login, admin actions |
| 91 Sun | WE | **Phase 7 review:** Grafana shows metrics. Logs have traceId. Audit log table populated |

---

### PHASE 8 — Testing, Docker, CI/CD, Deploy (Week 14)

**Goal:** Tests written, Docker images built, CI pipeline green, deployed to AWS.

| Day | Type | Task |
|-----|------|------|
| 92 Mon | WD | Testcontainers setup. Integration test: `AuthControllerTest` — register + login |
| 93 Tue | WD | Integration test: `DocumentServiceTest` — upload + parse + chunk |
| 94 Wed | WD | Integration test: `ChatServiceTest` — mock LLM, real Qdrant via Testcontainers |
| 95 Thu | WD | Unit tests: `JwtService`, `ChunkingService`, `PromptBuilder`, `CitationExtractor` |
| 96 Fri | WD | Backend `Dockerfile`. Frontend `Dockerfile` + Nginx config |
| 97 Sat | WE | Full `docker-compose.yml` — all 7 services: app + frontend + postgres + redis + qdrant + prometheus + grafana |
| 98 Sun | WE | GitHub Actions: `backend.yml` (build → test → docker push). `frontend.yml` (lint → build → docker push) |
| 99 Mon | WD | AWS: EC2 instance, install Docker, pull images, set env vars via `.env`, start compose |
| 100 Tue | WD | S3 bucket for file storage. Swap `FileStorageService` local → S3 impl |
| 101 Wed | WD | Nginx reverse proxy config (SSL termination, route `/api` → backend, `/` → frontend) |
| 102 Thu | WD | Final docs: `Architecture.md`, `API.md`, `Database.md`. Update `README.md` with screenshots |
| 103 Fri | WD | **PROJECT COMPLETE** — portfolio-ready, deployed, documented |

---

## Progress Tracker

| Phase | Status | Target Week | Done |
|-------|--------|-------------|------|
| 1 — Foundation | Not Started | Week 1–2 | [ ] |
| 2 — Authentication | Not Started | Week 3–4 | [ ] |
| 3 — Document Upload | Not Started | Week 5–6 | [ ] |
| 4 — AI / RAG Pipeline | Not Started | Week 7–8 | [ ] |
| 5 — Chat & Streaming | Not Started | Week 9–10 | [ ] |
| 6 — React Frontend | Not Started | Week 11–12 | [ ] |
| 7 — Monitoring & Cache | Not Started | Week 13 | [ ] |
| 8 — Testing & Deploy | Not Started | Week 14 | [ ] |

---

## Key Decisions

| Decision | Choice | Reason |
|----------|--------|--------|
| LLM abstraction | Spring AI ChatClient | Swap provider via config only |
| Local LLM | Ollama | Zero cost dev loop |
| Vector DB | Qdrant | Native Spring AI support, production-grade |
| Auth | JWT + refresh rotation | Stateless, Redis blacklist on logout |
| Chunking | Recursive character split, 800/150 | Balance context vs embedding cost |
| Streaming | SSE (not WebSocket) | Simpler, no handshake, chat-only use case |
| File storage | Local → S3 on deploy | Interface abstraction, swap impl |
| DB migrations | Flyway | Version-controlled schema |
| Exception format | RFC 7807 Problem Details | Enterprise standard |
| API versioning | `/api/v1/` prefix | Future-proof |

---

## Dependencies to Add to pom.xml (Phase 1, Day 1)

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Flyway -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.6.3</version>
    <scope>provided</scope>
</dependency>

<!-- Apache Tika -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.2</version>
</dependency>
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>2.9.2</version>
</dependency>

<!-- Apache POI (Excel/DOCX) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.3.0</version>
</dependency>

<!-- OpenAPI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>

<!-- Resilience4j -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>

<!-- Testcontainers -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring AI (add in Phase 4) -->
<!-- BOM in <dependencyManagement> -->
<!-- spring-ai-openai-spring-boot-starter -->
<!-- spring-ai-vertex-ai-gemini-spring-boot-starter -->
<!-- spring-ai-ollama-spring-boot-starter -->
<!-- spring-ai-qdrant-store-spring-boot-starter -->
```

---

## Notes

- Spring Boot 4.1.0 requires Spring AI 1.x — verify BOM version compatibility before Phase 4.
- Spring Boot 4.x test starter naming changed — already reflected in current `pom.xml`.
- Virtual threads enabled via `spring.threads.virtual.enabled=true` in `application.yml`.
- Keep `application-local.yml` git-ignored if it contains API keys. Use `.env.example` as reference.
- Per-user Qdrant filtering: always pass `userId` as payload filter on vector search to enforce data isolation.
