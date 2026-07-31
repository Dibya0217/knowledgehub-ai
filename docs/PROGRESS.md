# KnowledgeHub AI — Daily Progress Log

> Format: `## YYYY-MM-DD — <short label>`
> Each entry: what was done, decisions made, blockers, next session goal.

---

## 2026-07-25 — Project Scaffold + Phase 1 Dependencies + Monorepo Restructure

### Done
- Initialized Spring Boot 4.1.0 + Java 25 project at repo root
- Moved Spring Boot source into `backend/` subfolder (monorepo layout: `backend/` + `frontend/` as siblings)
  - Moved: `src/`, `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, `HELP.md` → `backend/`
  - Updated `.gitignore`: `target/` → `backend/target/`
  - Updated `docs/PLAN.md` repo structure diagram
- Switched `application.properties` → `application.yml` (better for deeply nested config)
- Created full repository structure:
  - `frontend/`, `docker/{postgres,redis,qdrant,prometheus,grafana}/`
  - `.github/workflows/`, `scripts/`
  - `docs/` stubs: Architecture, API, Database, Security, RAG, Deployment
- Created all 51 backend Java packages under `com.dibya.knowledgehub` with `.gitkeep`
- Added Phase 1 Day 1 dependencies to `pom.xml`:
  - JWT: `jjwt-api/impl/jackson` 0.12.6
  - Redis: `spring-boot-starter-data-redis`
  - Flyway: `flyway-core` + `flyway-database-postgresql`
  - MapStruct: 1.6.3 (processor order: Lombok → MapStruct)
  - Apache Tika: 2.9.2 (`tika-core` + `tika-parsers-standard-package`)
  - Apache POI: `poi-ooxml` 5.3.0
  - OpenAPI: `springdoc-openapi-starter-webmvc-ui` 2.6.0
  - Resilience4j: `resilience4j-spring-boot3` 2.2.0
  - Testcontainers BOM 1.20.4 + `testcontainers:postgresql`
- Fixed broken Spring Initializr test starters → `spring-boot-starter-test` + `spring-security-test`
- Fixed main class: wrong package `knowledgehub_ai` + wrong name → `com.dibya.knowledgehub.KnowledgeHubApplication`

### Decisions
- Spring AI deps deferred to Phase 4 (not needed until embedding/vector work)
- Testcontainers BOM imported explicitly — Spring Boot 4.1.0 parent does not manage `testcontainers:postgresql` version

### Verification
- `./mvnw compile` — PASS
- `./mvnw dependency:resolve` — PASS
- `./mvnw test` — skipped (needs Docker Compose: Postgres + Redis)

### Blockers
- None

### Next Session (Phase 1 — Week 1, Day 1 continued / Day 2)
- `application.yml`, `application-local.yml`, `application-dev.yml`, `application-prod.yml`
- `docker-compose.yml` with Postgres + Redis + Qdrant

---

## 2026-07-26 — Phase 1 Foundation Layer (Week 1 Tasks 2–7)

### Done
- Added `logstash-logback-encoder:8.0` to `pom.xml` for JSON logging
- Created `application.yml` (base config — env vars, no secrets), `application-local.yml` (local dev defaults), `application-dev.yml` (dev/staging), `application-prod.yml` (production)
- Created `logback-spring.xml` — colored human-readable output for local/dev, JSON format (Logstash) for prod, MDC `correlationId` field in all logs
- Created `CorrelationIdFilter` — reads/generates `X-Correlation-ID`, sets MDC, echoes header in response
- Created `ApiResponse<T>` — success wrapper with `success`, `message`, `data`, `correlationId`, `timestamp`
- Created `PagedResponse<T>` — pagination wrapper with `from(Page<T>)` factory
- Created `ErrorResponse` — RFC 7807 Problem Details (`type`, `title`, `status`, `detail`, `instance`, `correlationId`, `timestamp`, `errors`)
- Created custom exceptions: `ResourceNotFoundException`, `ConflictException`, `UnauthorizedException`, `ForbiddenException`, `BadRequestException`, `ValidationException`, `StorageException`, `AiProviderException`
- Created `GlobalExceptionHandler` (`@RestControllerAdvice`) — handles all exception types, no stack traces in responses
- Created `docker-compose.yml` — PostgreSQL 16, Redis 7, Qdrant (latest) with healthchecks and named volumes
- Created `.env.example` — documents all required env vars

### Decisions
- `application-local.yml` disables Flyway (`flyway.enabled: false`) — no migrations exist yet, avoids startup failure
- Logback uses Spring profile conditions — no manual env var needed
- `ErrorResponse` is a custom POJO (not Spring's `ProblemDetail`) for full control over serialization
- `CorrelationIdFilter` placed in `common/filter/` package (infrastructure concern, not security)

### Verification
- `./mvnw compile` — PASS (Lombok/Java 25 warnings only, no errors)

### Blockers
- None

### Next Session (Phase 1 — Week 2)
- `AppConfig` — virtual threads executor, async config, Jackson config
- OpenAPI / Swagger config — Swagger UI at `/swagger-ui.html`
- Flyway setup — `V1__create_users_roles.sql` migration
- `RequestLoggingFilter` — log method, path, status, duration

---

## 2026-07-30 — Phase 1 Week 2 (Days 8–13): Config, Flyway, OpenAPI, Logging, Audit

### Done
- **Day 8** — Created `V1__create_users_roles.sql`: tables `users`, `roles`, `user_roles`, `audit_logs`; seeded `ROLE_USER` + `ROLE_ADMIN`; enabled Flyway in `application-local.yml`
- **Day 9** — Created `AppConfig`: virtual thread executor (`@EnableAsync`), `ObjectMapper` (JavaTimeModule, no date timestamps), `RestClient.Builder` bean
- **Day 10** — Created `OpenApiConfig`: SpringDoc `OpenAPI` bean with JWT `bearerAuth` security scheme, server entries, contact info
- **Day 11** — Created `banner.txt` (ASCII art); added `info.app.*` to `application.yml` for `/actuator/info`
- **Day 12** — Created `RequestLoggingFilter`: logs method, path, status, duration, correlationId, ip on every non-actuator request
- **Day 13** — Created `AuditLog` JPA entity, `AuditLogRepository`, `AuditService` with `@Async` save; created `README.md` skeleton

### Decisions
- `RequestLoggingFilter` skips `/actuator/**` to avoid noise
- `AuditService.log()` is `@Async` — fires-and-forgets using virtual thread executor from `AppConfig`
- `OpenApiConfig` sets global `bearerAuth` security requirement — all endpoints show lock icon in Swagger UI

### Verification
- Run `docker compose up -d` then `mvn spring-boot:run -Dspring-boot.run.profiles=local`
- Flyway runs V1: `\dt` in psql shows 4 tables
- `GET /actuator/health` → `{"status":"UP"}`
- `GET /actuator/info` → app metadata
- `GET /swagger-ui.html` → Swagger UI renders
- Every request log line includes `correlationId` and `duration`

### Blockers
- None

### Next Session (Phase 2 — Week 3)
- User entity + Spring Security config
- JWT filter + token provider
- Register / Login endpoints

---

## 2026-07-30 — Phase 2 Week 3 (Days 15–21): Full JWT Authentication

### Done
- **Day 15** — `User` JPA entity (maps `users` table, ManyToMany roles, `@PreUpdate` for `updatedAt`), `Role` JPA entity
- **Day 16** — `UserRepository` (`findByEmail`, `existsByEmail`), `RoleRepository` (`findByName`), `UserDetailsServiceImpl` (loads by email, maps roles to `GrantedAuthority`), `PasswordEncoder` bean (BCrypt) added to `AppConfig`
- **Day 17** — `JwtService`: `generateAccessToken`, `generateRefreshToken`, `extractEmail`, `isTokenValid`, `isTokenExpired`, `getExpiryMillis` using jjwt 0.12.6
- **Day 18** — `JwtAuthFilter`: reads Bearer header, checks Redis blacklist, validates token, sets `SecurityContextHolder`
- **Day 19** — `SecurityConfig` updated: added `JwtAuthFilter`, `DaoAuthenticationProvider`, `AuthenticationManager`, `/api/v1/auth/**` permitted
- **Day 20** — DTOs (`RegisterRequest`, `LoginRequest`, `RefreshRequest`, `AuthResponse`), `AuthService` (register/login/refresh/logout), `AuthController` (4 endpoints)
- **Day 21** — `V2__create_refresh_tokens.sql`, `RefreshToken` entity + `RefreshTokenRepository`, refresh rotation + Redis JWT blacklist on logout
- Fixed JWT secret to Base64-encoded value in `application-local.yml`

### Decisions
- `RegisterRequest`, `LoginRequest`, `RefreshRequest`, `AuthResponse` use Java records (immutable, concise)
- Refresh token rotation: old token revoked on use, new one issued
- Logout blacklists access token in Redis with TTL = remaining expiry; revokes all user refresh tokens in DB
- `JwtAuthFilter` silently ignores invalid tokens (no exception thrown — lets Spring Security return 401)

### Verification
```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","name":"Test User","password":"password123"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123"}'
```

### Blockers
- None

### Next Session (Phase 2 — Week 4)
- `GET /api/v1/users/me` — current user profile endpoint
- Google OAuth2 callback handler
- Password reset (OTP via Redis)
- Rate limiting on login (Resilience4j)

---

## 2026-07-30 — Phase 2 Week 4: User Profile, OAuth2, Rate Limiting, Password Reset

### Done
- **UserController** — `GET /api/v1/users/me` returns full user profile (id, email, name, provider, emailVerified, createdAt, roles)
- **UserProfileResponse** DTO + **UpdateProfileRequest** DTO
- **UserService** — `getCurrentUser()` loads from `SecurityContextHolder`
- **OAuth2SuccessHandler** — issues JWT on Google OAuth2 callback, creates user if first login
- **RateLimitFilter** — rate limits login attempts via Resilience4j
- **ForgotPasswordRequest** + **ResetPasswordRequest** DTOs (password reset flow)
- **CorsConfig** — explicit CORS bean
- **SecurityConfig** — added `exceptionHandling` with custom `AuthenticationEntryPoint` returning JSON 401 instead of redirecting to Google OAuth page

### Bug Fixed
- `GET /api/v1/users/me` was returning Google OAuth HTML redirect instead of 401 for unauthenticated requests — missing `authenticationEntryPoint` in `SecurityConfig`

### Decisions
- `AuthenticationEntryPoint` returns `{"success":false,"message":"Unauthorized"}` with 401 status — no redirect for API clients

### Verification
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@test.com","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

curl http://localhost:8080/api/v1/users/me -H "Authorization: Bearer $TOKEN"
# Returns: id, email, name, provider, emailVerified, createdAt, roles
```

### Blockers
- None

### Next Session (Phase 2 — Week 4 continued)
- `PUT /api/v1/users/me` — update profile
- Forgot/reset password endpoints (OTP via Redis)
- Email verification flow

---

## 2026-07-31 — Phase 3 Week 5: Document Entities, Storage, Parsers

### Done
- **V4__create_documents.sql** — `documents` + `document_metadata` tables
- **Document** JPA entity + **DocumentMetadata** JPA entity + **DocumentStatus** enum (`PENDING/PROCESSING/READY/FAILED`)
- **DocumentRepository** (`findByUser`, `findByIdAndUser`, `findByUserAndStatus`) + **DocumentMetadataRepository**
- **FileStorageService** interface + **LocalFileStorageService** — saves to `uploads/{userId}/{documentId}/` (S3-ready via interface swap)
- **DocumentParser** interface + **ParsedDocument** record
- 8 parsers: `PdfParser` (PDFBox), `DocxParser` (POI), `TxtParser`, `CsvParser` (commons-csv), `ExcelParser` (POI), `JsonParser` (Jackson), `MarkdownParser`, `TikaFallbackParser`
- **ParserFactory** — Tika MIME auto-detection → selects correct parser
- Added `commons-csv 1.12.0` to pom.xml

### Decisions
- File content stored on disk (not DB) — DB holds path pointer only; `FileStorageService` interface allows S3 swap in Phase 8 with zero controller/service changes
- Chunks deferred to Week 6 (no DB table for chunks — goes to Qdrant in Phase 4)

### Verification
- `./mvnw compile` — PASS

### Blockers
- None

### Next Session (Phase 3 — Week 6)
- `TextChunk` + `ChunkingService`
- `DocumentController` + `DocumentService` (async upload pipeline)
- `GET/DELETE /api/v1/documents` endpoints

---

## 2026-07-31 — Phase 3 Week 6: Document Service, Upload API, Chunking Pipeline

### Done
- **TextChunk** record + **ChunkingService** — recursive character splitter (size=800, overlap=150), splits on `\n\n` → `\n` → `". "` → `" "` → hard split
- **DTOs**: `DocumentResponse`, `DocumentStatusResponse`, `DocumentUploadResponse`
- **DocumentController** — 5 endpoints: `POST /upload`, `GET /`, `GET /{id}`, `GET /{id}/status`, `DELETE /{id}`
- **DocumentService** — sync upload creates DB record + stores file, fires `@Async processDocumentAsync()`: `PENDING → PROCESSING → parse → chunk → save metadata → READY` (or `FAILED`)
- Upload validation: max 50MB, MIME whitelist (PDF, DOCX, XLSX, XLS, CSV, JSON, TXT, MD)
- Delete removes DB record + file from disk

### Decisions
- `MultipartFile` consumed after first read — store file first, then re-read from disk for async parsing
- Chunks held in memory this week — Qdrant persistence wired in Phase 4 (Week 7)

### Verification
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@test.com","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# Upload
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -H "Authorization: Bearer $TOKEN" -F "file=@test.pdf"

# Poll status (PENDING → PROCESSING → READY)
curl http://localhost:8080/api/v1/documents/{id}/status \
  -H "Authorization: Bearer $TOKEN"
```

### Blockers
- None

### Next Session (Phase 4 — Week 7)
- Spring AI BOM + OpenAI/Gemini/Ollama dependencies
- `AiProviderConfig` — conditional beans per `app.ai.provider`
- Qdrant `VectorStoreConfig`
- `EmbeddingService` + `VectorStoreService`
- Wire chunks → embed → upsert to Qdrant

---

## 2026-07-30 — Phase 4 Week 7: Spring AI, Embeddings, Qdrant Vector Store

### Done
- **Day 43** — Added Spring AI 2.0.0 BOM + starters to `pom.xml`: `spring-ai-starter-model-ollama`, `spring-ai-starter-model-openai`, `spring-ai-starter-vector-store-qdrant`
- **Day 44** — `AiProviderConfig`: `@ConditionalOnProperty` selects Ollama (default) or OpenAI `EmbeddingModel` as `@Primary` bean via `app.ai.embedding-provider`
- **Day 45** — `VectorStoreConfig`: empty config class; Qdrant auto-config handles collection creation via `initialize-schema: true`
- **Day 46** — `EmbeddingService`: wraps Spring AI `EmbeddingModel`; `embed(String)` → `float[]`, `embedBatch(List<String>)` → `List<float[]>`
- **Day 47** — `VectorStoreService`: `upsert(chunks)` converts `TextChunk` → Spring AI `Document` with metadata (documentId/userId/filename/index) → `VectorStore.add()`; `search(query, userId, topK)` filtered by userId; `deleteByDocumentId(UUID)` via string filter expression
- **Day 48** — Wired `VectorStoreService` into `DocumentService`: `processDocumentAsync()` calls `vectorStoreService.upsert(chunks)` after chunking; `deleteDocument()` calls `vectorStoreService.deleteByDocumentId(id)` before DB delete
- Added `spring.ai.*` config to `application.yml` (Ollama, OpenAI, Qdrant) and `application-local.yml` (local overrides)

### Decisions
- Spring AI 2.0.0 used (not 1.0.0) — Spring AI 2.x targets Spring Boot 4.x; Spring AI 1.0.0 targets Spring Boot 3.x
- Starter artifact IDs changed in 2.0.0: `spring-ai-starter-model-ollama` (not `spring-ai-ollama-spring-boot-starter`)
- `EmbeddingService` not injected into `DocumentService` — embedding happens inside `VectorStoreService.upsert()` via Spring AI's internal pipeline (Spring AI auto-embeds on `VectorStore.add()`)

### Verification
```bash
# 1. Ensure Ollama running
# ollama pull nomic-embed-text && ollama serve

# 2. Ensure Qdrant running
# docker compose up qdrant -d

# 3. Start app
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 4. Upload a document
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@test.com","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -H "Authorization: Bearer $TOKEN" -F "file=@test.pdf"

# 5. Poll until READY
curl http://localhost:8080/api/v1/documents/{id}/status \
  -H "Authorization: Bearer $TOKEN"

# 6. Verify in Qdrant dashboard
# http://localhost:6333/dashboard → knowledge_vectors → points count > 0
```

### Blockers
- None

### Next Session (Phase 4 — Week 8)
- RAG retrieval service: query → embed → Qdrant search → context assembly
- `POST /api/v1/chat` endpoint
- Conversation history (PostgreSQL)
- Citation tracking

---

## 2026-07-31 — Phase 4 Week 8: RAG Chat API + Conversation History

### Done
- **Day 50** — Flyway migration `V5__create_conversations.sql` (conversations + messages tables with indexes); `Conversation`, `Message`, `MessageRole` JPA entities; `ConversationRepository`, `MessageRepository`
- **Day 51** — `LlmService`: wraps Spring AI `ChatClient`; `call(systemPrompt, messages)` → content string; `@Lazy ChatModel` injection avoids Ollama probe at startup
- **Day 52** — `RagService`: `retrieve(question, userId, topK)` delegates to `VectorStoreService.search()`; `buildContext(docs)` formats chunks as `[Source: filename, chunk N]\n<text>`
- **Day 53** — `PromptBuilder`: system template with `{context}` placeholder; `buildMessages(context, history, question)` returns `SystemMessage + last 10 history + UserMessage`
- **Day 54** — DTOs: `ChatRequest`, `ChatResponse`, `SourceReference`, `ConversationSummary`, `MessageDTO`; `ChatService`: full pipeline — load/create conversation → RAG retrieve → LLM call → persist messages → return with sources
- **Day 55** — `ChatController` at `/api/v1/chat`: POST chat, GET list conversations (paged), GET messages by conversation, DELETE conversation

### Decisions
- `LlmService.call()` separates system prompt from messages — `ChatClient.prompt().system(sp).messages(msgs).call()`
- History capped at 10 messages (5 turns) via `findTop20ByConversationOrderByCreatedAtDesc` + reversed list in service
- `@Lazy ChatModel` injection in `LlmService` mirrors `@Lazy VectorStore` pattern — prevents Ollama probe at startup
- Flyway disabled locally; Hibernate `ddl-auto: update` creates conversations + messages tables automatically

### Verification
```bash
# 1. Start app (Ollama + Qdrant running, doc already READY)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 2. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@test.com","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# 3. First chat (new conversation)
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"What is this document about?","conversationId":null}'

# 4. Follow-up (use conversationId from step 3)
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"Tell me more","conversationId":"<uuid>"}'

# 5. List conversations
curl http://localhost:8080/api/v1/chat -H "Authorization: Bearer $TOKEN"
```

### Blockers
- None

### Next Session (Phase 4 — Week 9)
- Frontend chat UI (React + WebSocket or polling)
- Streaming responses via SSE
- Document management UI

---

<!-- Add new entries above this line, newest first -->
