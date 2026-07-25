# KnowledgeHub AI — Daily Progress Log

> Format: `## YYYY-MM-DD — <short label>`
> Each entry: what was done, decisions made, blockers, next session goal.

---

## 2026-07-25 — Project Scaffold + Phase 1 Dependencies

### Done
- Initialized Spring Boot 4.1.0 + Java 25 project at repo root
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

<!-- Add new entries above this line, newest first -->
