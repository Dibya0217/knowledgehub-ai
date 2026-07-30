# KnowledgeHub AI

RAG-powered knowledge management platform. Upload documents, chat with your data, get cited answers.

## Tech Stack

- **Backend**: Spring Boot 4.1.0, Java 25, Spring Security, Spring AI
- **Database**: PostgreSQL 16, Flyway migrations
- **Cache**: Redis 7
- **Vector DB**: Qdrant
- **Auth**: JWT + Google OAuth2
- **Docs**: Swagger UI (SpringDoc OpenAPI)

## Prerequisites

- Java 25+
- Maven 3.9+
- Docker + Docker Compose

## Local Setup

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Copy env file
cp .env.example .env.local
# Edit .env.local with your values (AI API keys, etc.)

# 3. Run backend
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 4. Verify
curl http://localhost:8080/actuator/health
open http://localhost:8080/swagger-ui.html
```

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_URL` | Yes | PostgreSQL JDBC URL |
| `DB_USER` | Yes | Database username |
| `DB_PASS` | Yes | Database password |
| `REDIS_HOST` | Yes | Redis host |
| `REDIS_PORT` | No | Redis port (default: 6379) |
| `REDIS_PASS` | No | Redis password |
| `JWT_SECRET` | Yes | JWT signing secret (min 32 chars) |
| `CORS_ORIGINS` | No | Allowed CORS origins (default: http://localhost:5173) |
| `AI_PROVIDER` | No | AI provider: `ollama` \| `gemini` \| `openai` (default: ollama) |
| `EMBEDDING_PROVIDER` | No | Embedding provider (default: ollama) |
| `OPENAI_API_KEY` | Prod | OpenAI API key |
| `GEMINI_API_KEY` | Dev | Google Gemini API key |
| `OLLAMA_BASE_URL` | Local | Ollama base URL (default: http://localhost:11434) |
| `STORAGE_PATH` | No | File upload directory (default: ./uploads) |
| `GOOGLE_CLIENT_ID` | OAuth | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | OAuth | Google OAuth2 client secret |

## API Docs

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## Docker Services

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Cache + JWT blacklist |
| Qdrant | 6333 | Vector embeddings |
