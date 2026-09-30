# Project structure map

> Persistent overview for AI agents. Generated on first run by `/understand` (see `.cursor/commands/understand-project.md`). Update when architecture changes significantly.

## Meta

| Field | Value |
|-------|-------|
| **Updated** | 2026-09-30 |
| **Tool** | cursor |

## Stack

- **Backend:** Java 17, Spring Boot 4.1.1, Spring Security 7 (JWT via jjwt 0.13.0), Spring Data JPA, Flyway 11, MapStruct 1.6.3, Lombok 1.18.38, springdoc-openapi 3.1.1, PostgreSQL
- **Frontend:** React 18 (CRA / react-scripts), Material UI 5, React Router 6, Axios, React Hook Form, Notistack, ApexCharts
- **Infra:** Docker Compose (Postgres 14.6); prod compose also runs frontend + backend containers
- **Domain:** Digital e-wallet — register/login, wallets (IBAN), top-up/withdraw, transfers, transaction history

## Layout

| Path | Purpose |
|------|---------|
| `backend/` | Spring Boot REST API (Maven) |
| `frontend/` | React SPA (Material Kit–based dashboard) |
| `architecture/` | C4 model, ADRs, deployment diagrams |
| `docs/` | Architecture overview, features, evaluation notes |
| `tasks/` | Agent task list (`todo.md`) |
| `docker-compose.yml` | Dev: Postgres only |
| `docker-compose.prod.yml` | Prod: frontend + backend + DB |
| `.env.properties` | Docker/env placeholders (not secrets in docs) |
| `.cursor/` / `.claude/` / `.kiro/` / `.agents/` | AI agent hubs (synced) |

## Entry points

- **Backend:** `backend/src/main/java/com/ros/ewallet/EWalletApplication.java`
- **API controllers:** `AuthController`, `WalletController`, `TransactionController` under `.../controller/`
- **Frontend:** `frontend/src/index.js` → `App.js`; pages under `frontend/src/pages/` (auth, dashboard, wallet, transaction, transfer)
- **Config:** `backend/src/main/resources/application.yml` (+ `application-dev.yml`)
- **Migrations:** `backend/src/main/resources/db/migration/V1__…`–`V5__…`

## Key files

- Security: `config/SecurityConfig.java`, `security/JwtUtils.java`, `security/AuthTokenFilter.java`
- Services: `service/WalletService.java`, `AuthService.java`, `TransactionService.java`
- Domain entities: `domain/entity/{User,Wallet,Transaction,Role,Type}.java`
- IBAN: `validator/ValidIban.java`, `validator/IbanValidator.java`
- Frontend API: `frontend/src/services/{axios,HttpService,AuthService,AuthHeader}.js`
- Docs: `README.md`, `backend/src/main/resources/docs/how_to_run.md`, `docs/overview.md`
- ADR: `architecture/adr/0001-select-spring-boot-as-backend-framework.md`

## Commands

| Action | Command |
|--------|---------|
| DB (dev) | `docker compose up --build` (from repo root) |
| Backend | Open `backend/` in IDE, Java 17, run `EWalletApplication` (or `./mvnw spring-boot:run`) |
| Frontend install | `cd frontend && npm install` |
| Frontend dev | `cd frontend && npm start` (port 3000) |
| Frontend build | `cd frontend && npm run build` |
| Frontend lint | `cd frontend && npm run lint` |
| Backend test | `cd backend && ./mvnw test` |
| Prod stack | `docker compose -f docker-compose.prod.yml up --build` |
| OpenAPI | Backend Swagger UI (springdoc) when API is running |

## Code intelligence

| Item | Status |
|------|--------|
| CodeGraph index | Present — `.codegraph/` (221 files, healthy) |
| Workspace root | `/Volumes/Data/Software Development/Java/e-wallet` |
| OntoSight | `npx royalsolution-ontosight@0.2.1 "/Volumes/Data/Software Development/Java/e-wallet"` |

## Notes

- Layered backend: controller → service → repository; DTOs + MapStruct mappers; global exception handler.
- JWT auth; roles via `RoleType`; wallets keyed by IBAN with custom `@ValidIban`.
- Dev compose starts **Postgres only**; run Spring Boot and React locally. Prod compose builds all three services (frontend `:3000`, backend `:8080`).
- Env vars live in `.env.properties` at repo root (used by Docker). Do not commit real secrets.
- Agent scaffolding (class-ai-agent) is installed; app code is under `backend/` + `frontend/`, not a Node monorepo root.
