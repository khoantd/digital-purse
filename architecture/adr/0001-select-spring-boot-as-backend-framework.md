---
number: 1
status: proposed
---

---
number: 1
title: Select Spring Boot as Backend Framework
status: proposed
decided_at: 2026-09-15
linked_views: []

# ADR-0001: Select Spring Boot as Backend Framework

## Context
The E-Wallet project is a full stack web application designed to manage digital wallet transactions utilizing a REST API. Based on the referenced README, we have identified several key requirements, such as user authentication, support for IBAN validation, transaction management, and dependency on multiple services like PostgreSQL, Spring Data JPA, and Spring Security.

## Decision
We propose using Spring Boot as the backend framework for the E-Wallet project due to its rapid development capabilities, extensive support for RESTful applications, and robust security features. Spring Boot's ability to manage dependencies via Maven, as shown in the `pom.xml`, and its active community support align well with our project needs.

## Consequences

### Positive
- Fast development lifecycle and reduced boilerplate code through Spring Boot's conventions.
- Integrated security features with Spring Security, enabling secure user authentication.
- Support for using Spring Data JPA to simplify database interactions and management.

### Negative
- Potential complexity in managing multiple Spring Boot dependencies and configurations as project scales.
- Requires familiarity with Spring ecosystem, which may involve a learning curve for new team members.

### Follow-ups
- Assess the onboarding and training requirements for team members less familiar with Spring Boot.
- Monitor the actual performance and scalability of the application as features are added.
