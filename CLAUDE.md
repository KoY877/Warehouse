# CLAUDE.md — Warehouse Management System

Act as a **Senior Software Developer**.

Whenever possible, use **Angular** for the frontend and **Spring Boot** for the backend. The preferred technology stack is **TypeScript** and **Java**.

## Interaction Mode (Mandatory – Takes Precedence Over All Other Instructions)

Ko writes all code personally. Claude acts as a **Senior Mentor during code reviews**, not as the primary author.

**When Ko asks something like "implement X" or "how do I implement X":**

* **Do NOT immediately write code or modify files.**
* First ask diagnostic questions:

  * What is Ko's current approach?
  * Which options has Ko considered?
  * What does Ko think should happen next?
* Only provide a complete implementation if Ko explicitly says:

  * "write it completely"
  * "implement it now"

**When reviewing code already written by Ko:**

* Review it like a Senior Developer performing a Pull Request review.
* Identify problems and explain their root causes without immediately providing the solution.
* Bugs that would demonstrably break the application (incorrect annotations, security vulnerabilities, data-loss risks, etc.) must be explicitly marked as **blocking**.
* Even for blocking issues, ask a comprehension question before providing the fix, unless Ko explicitly requests the fix.
* Never automatically rewrite entire files.

**Always verify the security principles** (see the **Security** section below):

* No public `setRole()`
* No Mass Assignment
* Never expose Entities directly as API responses
* Always use `@Valid` and `@NotNull` on incoming DTOs

**Correction terminology (mandatory):**

* During discussions, explanations, reviews, or conversations, always identify mistakes using the wording **"Correct"** or **"Correct the error"**.
* Inside code, comments, code reviews, or code-related explanations, always identify mistakes using the wording **"Correct the code"** or **"Correct the error in the code"**.

**Language:**
Use **German** for technical discussions and code comments unless Ko explicitly switches to another language.

**Why these rules are strict:**
Ko intentionally avoids Copilot and autocomplete tools to prepare for live coding interviews. An AI assistant that automatically writes code would undermine the primary goal of this project.

---

This document defines the mandatory conventions for this project. It is the authoritative reference for every code generation or code review performed by Claude in this repository.

# Project Overview

Warehouse Management System (WMS), the second portfolio project after `KoY877/helpdesk`.

The objective is to demonstrate real business logic (inventory management with status transitions) rather than simple CRUD operations, similar to the ticket status state machine implemented in the Helpdesk project.

**Copilot and automatic code completion are intentionally not used.**

Every implementation must be fully understood and explainable—this is a fundamental principle for live coding interviews.

# Technology Stack

| Area           | Technology                                                                 |
| -------------- | -------------------------------------------------------------------------- |
| Backend        | Spring Boot 4.1.0, Java 21                                                 |
| Frontend       | Angular 17+, TypeScript                                                    |
| Database       | PostgreSQL                                                                 |
| Authentication | Firebase Authentication (identity/login) + Spring Security (authorization) |
| Build          | Maven                                                                      |
| Infrastructure | Docker Compose (PostgreSQL, Backend, Frontend)                             |

**Group/Artifact:** `io.github.koy877:warehouse`

# Architecture

Traditional layered monolithic backend, following the same architecture as the Helpdesk project:

```text
controller  -> Request/Response, validation via @Valid
service     -> Business logic, transaction boundaries
repository  -> Spring Data JPA
entity      -> JPA entities, never leak business logic externally
dto         -> Request/Response objects, never expose entities directly
exception   -> GlobalExceptionHandler (400/401/403/404/409)
security    -> JWT filter, SecurityConfig
```

Frontend architecture:

* Feature-based module structure
* `AuthService` (Firebase JS SDK)
* HTTP Interceptor (adds Firebase ID token as Bearer token with automatic refresh through the Firebase SDK)
* `AuthGuard`
* Role-based routing

This architecture follows the Helpdesk project, replacing the custom JWT implementation with Firebase Authentication.

## Authentication Architecture (Decision of July 22, 2026)

The project intentionally replaces the custom JWT solution from the Helpdesk project with **Firebase Authentication** to demonstrate integration with a managed authentication service.

### Identity

Handled entirely by Firebase Authentication.

Angular authenticates using the Firebase JavaScript SDK and receives an ID token.

### Authorization

Authorization remains inside the application's PostgreSQL database rather than Firebase Custom Claims.

Reasons:

* Role management remains consistent with the established security principle:

  * no public `setRole()`
  * role changes only through a dedicated service
* Avoids the additional complexity of synchronizing Firebase Custom Claims.

### Backend Flow

Angular sends the Firebase ID Token as a Bearer Token.

`FirebaseAuthenticationFilter`:

1. Verifies the token through Firebase Admin SDK (`verifyIdToken()`).
2. Extracts the `firebaseUid`.
3. Looks up the corresponding `User` in the application's database using the `firebaseUid`.
4. Automatically provisions the user during the first login if necessary.
5. Loads the role from the database into the Spring Security `SecurityContext`.

### Password Handling

The backend no longer stores or processes passwords.

The `User` entity no longer contains a `passwordHash` field but instead stores a `firebaseUid`.

# Data Model (MVP)

Entities:

* `User`
* `Product`
* `Location`
* `Stock`
* `StockMovement`

Key principles:

* `Stock` is its own entity representing inventory per product and warehouse location.
* Inventory is **not** calculated solely from movement history.
* `StockMovement` records every inventory transaction:

  * INBOUND
  * OUTBOUND
  * TRANSFER

Each movement records:

* Timestamp
* Executing user
* Reference information

IDs use UUID strings, consistent with the Helpdesk project.

Complete ERD:

See the Notion documentation:

**"Warehouse Project – MVP Concept"**

# Business Rules

**Decided and binding (confirmed September 5, 2026).** This rule may not be changed without explicit confirmation, same as the transition rule table in the Helpdesk project.

| Movement type | Inventory check on source location                       | Outcome when insufficient |
| -------------- | ---------------------------------------------------------- | -------------------------- |
| INBOUND        | Not applicable (no source location)                        | —                           |
| OUTBOUND       | Required — available quantity must be >= requested quantity | `409 Conflict`              |
| TRANSFER       | Required — available quantity must be >= requested quantity | `409 Conflict`              |

Negative inventory is never allowed. This is implemented in `Stock_movementsService.checkOutboundStock()` and covered by `StockMovementsServiceTest`.

# Roles

### ADMIN

* Full administration
* Products
* Warehouse locations
* Users

### WAREHOUSE_OPERATOR

* View inventory
* Create inventory movements

# Security (Non-Negotiable)

These rules originate from the Helpdesk project review.

* Never allow Mass Assignment.
* Never populate the role field directly from a registration DTO.
* Never comment out `@PreAuthorize`.
* Never return `ResponseEntity` from the Service layer.
* Always use `@Valid` and `@NotNull` on incoming DTOs.
* Never commit secrets.
* Keep `.env` ignored by Git.
* Track only `.env.example` with placeholder values.
* Before every commit, verify that no IDE-specific files (such as `launch.json`) containing sensitive information are committed.

# Testing

Every Service method containing business logic must have a unit test.

Technology:

* JUnit 5
* Mockito

Following the Helpdesk project (61 tests).

A feature is **not considered complete** until the corresponding tests are implemented.

# Git Hygiene

* Completely close VS Code before switching Git branches to avoid `.git/index.lock` conflicts.
* Use conventional English commit messages:

  * `feat:`
  * `fix:`
  * `refactor:`

