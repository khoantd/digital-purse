# Overview

# E-Wallet Project Documentation  
  
## Overview  
The E-Wallet project is a digital wallet management system designed to facilitate transactions through a robust REST API. This document outlines the project architecture, key decisions, and development guidelines.  
  
## Repository  
E-Wallet source code is hosted on GitHub: \[e-wallet\](https://github.com/khoantd/e-wallet.git)  
  
## Architecture  
  
### Current Framework  
The project will utilize \*\*Spring Boot\*\* as the backend framework to support necessary features:  
  
- \*\*User Authentication:\*\* Secure user login and management.  
- \*\*Transaction Management:\*\* Handle digital wallet transactions efficiently.  
- \*\*IBAN Validation:\*\* Validate International Bank Account Numbers for payments.  
  
### Dependencies  
The core dependencies for the E-Wallet include:  
  
- \*\*PostgreSQL:\*\* Database for storing user and transaction data.  
- \*\*Spring Data JPA:\*\* Simplified data access and manipulation.  
- \*\*Spring Security:\*\* Built-in security measures for user authentication and authorization.  
  
## Architecture Decision Records (ADRs)  
  
### ADR-0001: Select Spring Boot as Backend Framework  
  
- \*\*Context:\*\* The project requires a reliable framework for managing transactions and user data. Consideration was given to various frameworks, leading to the selection of Spring Boot.  
    
- \*\*Decision:\*\* Adopt Spring Boot for backend development due to its efficient development environment and extensive community support.  
  
- \*\*Consequences:\*\*  
  - \*\*Positive:\*\*   
    - Accelerated development cycle with reduced boilerplate code.  
    - Enhanced security with integrated Spring Security.  
    - Simplified database management through Spring Data JPA.  
  
  - \*\*Negative:\*\*   
    - Complexity in managing multiple dependencies as the project scales.  
    - Learning curve associated with the Spring ecosystem for new developers.  
  
- \*\*Follow-ups:\*\*  
  - Assess onboarding processes for developers unfamiliar with Spring Boot.  
  - Monitor application performance and scalability as additional features are integrated.  
  
## Development Guidelines  
  
### Project Structure  
1. \*\*Controllers:\*\* Handle incoming API requests and responses.  
2. \*\*Services:\*\* Encapsulate business logic.  
3. \*\*Repositories:\*\* Manage data interactions with PostgreSQL.  
  
### Testing  
Implement unit and integration tests using JUnit and Mockito to ensure the reliability of application components.  
  
### Deployment  
Follow standard CI/CD practices for deploying the application, leveraging GitHub Actions or similar tools for automated builds and testing.  
  
## Conclusion  
This document serves as a high-level guide for developing the E-Wallet project using Spring Boot. It is critical for the team to stay aligned with the established architecture and decisions while ensuring adherence to best practices in software development.
