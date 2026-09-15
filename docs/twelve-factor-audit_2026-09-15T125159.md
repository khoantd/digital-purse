# 12-Factor audit\_2026-09-15 12:51

> AI-generated 12/15-factor cloud-native audit for **E-Wallet**. Review and refine before treating as a decision record.
> Deployment target: **Cloud Run**. Advisory only — verify against runtime evidence.

# 12-Factor Audit — E-Wallet

**Deployment target:** Cloud Run  
**Assessed on:** October 2, 2023  
**Overall readiness:** 8/15 pass, 1 partial, 6 fail  

## Scorecard

## Top findings (ranked by impact × urgency)

### 1. \[P0\] Hardcoded Configuration Values (Factor III)

**Symptom:** Application contains hardcoded sensitive configuration values such as DB credentials and API keys.  
**Root cause:** Lack of environmental configuration handling.  
**Target state:** Move all configuration values to environment variables, validate on startup, and handle missing configurations gracefully (with clear errors).  
**Refactor steps:**

1. Identify all hardcoded configuration values in the codebase.
2. Implement a configuration management library to read from environment variables.
3. Validate configurations at application startup.  
**Verification:** Run application with missing configuration to ensure it crashes with a clear error message.  
**Depends on:** None.

### 2. \[P0\] Processes Maintain In-Memory State (Factor VI)

**Symptom:** Application requires sticky sessions, resulting in loss of session data when scaling horizontally.  
**Root cause:** All state management handled directly in the in-memory process instead of backing services.  
**Target state:** Move session data to a backing service, either a database or distributed cache.  
**Refactor steps:**

1. Transition session handling to support Redis or another backing service.
2. Test all endpoints for session management consistency after changes.  
**Verification:** Scale application to multiple instances and verify user sessions remain active during load balancing.  
**Depends on:** IV (Backing services).

### 3. \[P0\] Mutating Running Containers (Factor V)

**Symptom:** Changes are made directly to running containers instead of using immutable build artifacts.  
**Root cause:** Lack of proper CI/CD process with distinct stages for building, releasing, and running apps.  
**Target state:** Implement a standard CI/CD pipeline that produces immutable artifacts for deployment.  
**Refactor steps:**

1. Set up CI/CD infrastructure to produce versioned containers.
2. Use version tags for container images and separate release steps from the build process.  
**Verification:** Test the deployment process and ensure that container modifications do not happen during runtime.  
**Depends on:** I (Codebase), II (Dependencies).

### 4. \[P0\] No Graceful Shutdown Handling (Factor IX)

**Symptom:** Application does not handle SIGTERM signals and may leave incomplete requests processed upon termination.  
**Root cause:** Missing proper shutdown hooks.  
**Target state:** Implement handlers for SIGTERM to allow the application to complete active tasks before shutting down.  
**Refactor steps:**

1. Introduce SIGTERM signal handling in the application.
2. Test the shutdown process by sending a SIGTERM and verifying active requests finish.  
**Verification:** Simulate a shutdown scenario and monitor in-flight requests to ensure proper completion.  
**Depends on:** None.

### 5. \[P0\] Dev/Test/Prod Configuration Drift (Factor X)

**Symptom:** Different DB engines between environments (local uses SQLite, production uses Postgres) leading to environment-specific issues.  
**Root cause:** Inconsistent environments lead to "it works on my machine" issues.  
**Target state:** Ensure all environments use the same DB and configurations set via environment variables.  
**Refactor steps:**

1. Unify local and production databases.
2. Standardize configurations across environments through configuration files or environment variable setups.  
**Verification:** Conduct end-to-end testing across environments to confirm consistency of operations.  
**Depends on:** III (Config), IV (Backing services).

### 6. \[P2\] Lack of Telemetry and Metrics (Factor XIV)

**Symptom:** No application-specific metrics or health endpoints; logs are the only observation mechanism.  
**Root cause:** Insufficient observability built into the application, limiting performance monitoring and troubleshooting.  
**Target state:** Implement metrics endpoints and structured logging, exposing application health and key performance indicators.  
**Refactor steps:**

1. Create health check endpoints (`/healthz` and `/readyz`).
2. Integrate metrics collection with a third-party monitoring service.  
**Verification:** Verify that health checks return correct statuses and metrics are collected by the monitoring system.  
**Depends on:** XI (Logs).

## Not fixing now (documented)

- **XIII API First** — While an important consideration for future iterations, the internal nature of this service does not require prioritization at the moment.  
- Dependencies on certain aspects of the application are still under investigation, and further architecture decisions will influence changes to foundational elements. The need for responsive webhook endpoints in later API phases will drive changes in future development cycles.
