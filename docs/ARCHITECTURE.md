# E-Wallet Architecture

## Overview

The E-Wallet project is a full-stack web application designed to facilitate digital wallet transactions via a REST API. It focuses on critical functionalities such as user authentication, IBAN validation, transaction management, and leverages various services, including PostgreSQL, Spring Data JPA, and Spring Security.

## Context

Key constraints influencing the architecture include scalability, security, and performance requirements. Stakeholders involve users, developers, and system administrators. The quality attributes that are central to the design include maintainability, usability, and security.

## Key Decisions

For detailed decision records, refer to the ADRs in the `architecture/adr/` directory. Notable decisions include:

- **ADR-0001**: Select Spring Boot as the Backend Framework

## Structure

The architecture consists of major components:

- **Frontend**: User interface for interaction with the E-Wallet.
- **Backend (Spring Boot)**: Handles business logic, user authentication, and transactions.
- **Database (PostgreSQL)**: Stores user data and transaction records.
- **External Services**: Integrates with services for IBAN validation and payment processing.

Refer to the architecture views for a comprehensive illustration of the system components.

## Cross-Cutting Concerns

### Security

- Utilizes Spring Security for user authentication and authorization.

### Observability

- Implementation of logging and monitoring tools to track application performance.

### Deployment

- The application must support containerization for seamless deployment across environments.

### Operational Notes

- Assess potential scaling issues with multiple dependencies as the project grows. Monitor team members' familiarity with Spring Boot to facilitate smoother onboarding and training.
