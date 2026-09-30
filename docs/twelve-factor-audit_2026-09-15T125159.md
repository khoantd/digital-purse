# 12-Factor Audit — E-Wallet

> AI-generated 12/15-factor cloud-native audit for **E-Wallet**. Review and refine before treating as a decision record.  
> Deployment target: **Cloud Run**. Advisory only — verify against runtime evidence.

**Assessed on:** October 2, 2023  
**Overall readiness:** 8/15 pass, 1 partial, 6 fail  

## Scorecard

## Top Findings (Ranked by Impact × Urgency)

### 1. \[P0\] Hardcoded Configuration Values (Factor III)

- **Symptom:** Application contains hardcoded sensitive configuration values such as database credentials and API keys.
- **Root cause:** Lack of environmental configuration handling.
- **Target state:** All configuration values should be moved to environment variables. Validate on startup and handle missing configurations gracefully with clear error messages.

**Refactor Steps:**

1. Identify all hardcoded configuration values in the codebase.
2. Implement a configuration management library to read from environment variables.
3. Validate configurations during application startup.

**Verification:** Run the application with missing configuration to ensure it crashes with a clear error message.  
**Depends on:** None.

### 2. \[P0\] Processes Maintain In-Memory State (Factor VI)

- **Symptom:** Application requires sticky sessions, leading to the loss of session data when scaling horizontally.
- **Root cause:** In-memory state management instead of utilizing backing services.
- **Target state:** Move session data to a backing service such as a database or distributed cache.

**Refactor Steps:**

1. Transition session handling to support Redis or another backing service.
2. Test all endpoints for session management consistency after changes.

**Verification:** Scale the application to multiple instances and verify that user sessions remain active during load balancing.  
**Depends on:** IV (Backing services).

### 3. \[P0\] Mutating Running Containers (Factor V)

- **Symptom:** Changes are made directly to running containers instead of using immutable build artifacts.
- **Root cause:** Absence of a proper CI/CD process.
- **Target state:** Implement a standard CI/CD pipeline that produces immutable artifacts for deployment.

**Refactor Steps:**

1. Set up CI/CD infrastructure to produce versioned containers.
2. Use version tags for container images and separate release steps from the build process.

**Verification:** Test the deployment process to confirm that container modifications do not occur during runtime.  
**Depends on:** I (Codebase), II (Dependencies).

### 4. \[P0\] No Graceful Shutdown Handling (Factor IX)

- **Symptom:** Application does not handle SIGTERM signals, risking incomplete request processing upon termination.
- **Root cause:** Missing proper shutdown hooks.
- **Target state:** Implement handlers for SIGTERM to allow the application to complete active tasks before shutting down.

**Refactor Steps:**

1. Introduce SIGTERM signal handling in the application.
2. Test the shutdown process by sending a SIGTERM and confirming that active requests finish.

**Verification:** Simulate a shutdown scenario and monitor in-flight requests to ensure proper completion.  
**Depends on:** None.

### 5. \[P0\] Dev/Test/Prod Configuration Drift (Factor X)

- **Symptom:** Different database engines across environments (local uses SQLite, production uses PostgreSQL) leading to environment-specific issues.
- **Root cause:** Inconsistent environments cause "It works on my machine" problems.
- **Target state:** Ensure all environments use the same database and configurations set via environment variables.

**Refactor Steps:**

1. Unify local and production databases.
2. Standardize configurations across environments using configuration files or environment variable setups.

**Verification:** Conduct end-to-end testing across environments to confirm operational consistency.  
**Depends on:** III (Config), IV (Backing services).

### 6. \[P2\] Lack of Telemetry and Metrics (Factor XIV)

- **Symptom:** No application-specific metrics or health endpoints; current observability relies solely on logs.
- **Root cause:** Insufficient observability, limiting performance monitoring and troubleshooting.
- **Target state:** Implement metrics endpoints and structured logging to expose application health and key performance indicators.

**Refactor Steps:**

1. Create health check endpoints (`/healthz` and `/readyz`).
2. Integrate metrics collection with a third-party monitoring service.

**Verification:** Verify that health checks return correct statuses and that metrics are being collected by the monitoring system.  
**Depends on:** XI (Logs).

## Not Fixing Now (Documented)

- **XIII API First**: While this is a critical consideration for future iterations, the internal nature of this service does not necessitate prioritization at this time.  
- Dependencies on certain aspects of the application are under investigation, and further architectural decisions will influence foundational changes. The need for responsive webhook endpoints in later API phases will drive changes in future development cycles.
