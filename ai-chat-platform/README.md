# ai-chat-platform

Production-ready starter monorepo for an AI chat platform with React, TypeScript, Vite, Spring Boot 3, Java 21, Supabase, OpenAI, WebSocket, Zustand, TailwindCSS, and Docker.

## Structure

- `frontend/`: Vite React app with TailwindCSS, React Router, Zustand, Axios, protected routes, and chat UI.
- `backend/`: Spring Boot REST API with JWT auth scaffolding, WebSocket/STOMP chat endpoint, Supabase placeholders, OpenAI service structure, and message model.

## Prerequisites

- Node.js 20+
- Java 21
- Maven 3.9+
- Docker Desktop or Docker Engine
- Supabase project URL and service key
- OpenAI API key

## Environment

Copy the examples and fill in real values:

```bash
cp frontend/.env.example frontend/.env
cp backend/.env.example backend/.env
```

## Run Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:5173`.

## Run Backend

```bash
cd backend
mvn spring-boot:run
```

The backend runs at `http://localhost:8080`.

## Docker

```bash
docker compose up --build
```

Frontend: `http://localhost:5173`
Backend: `http://localhost:8080`

For backend-only local development without connecting to Supabase PostgreSQL:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml up backend --build
```

## API Samples

- `POST /api/auth/login`: mock login that returns a signed JWT.
- `GET /api/messages/{chatId}`: returns in-memory sample messages.
- `POST /api/ai/reply`: creates a placeholder AI reply.
- WebSocket endpoint: `/ws`
- STOMP publish: `/app/chat.send`
- STOMP subscribe: `/topic/chats/{chatId}`

## Architecture Notes

The starter separates feature packages by business capability. Frontend folders group UI, state, transport, and domain types. Backend packages isolate auth, chat, message, AI, WebSocket, and infrastructure configuration so each area can evolve without turning into a shared utility bucket.

Supabase and OpenAI clients are intentionally wrapped behind services. That keeps external providers replaceable and makes testing easier as the application grows.
