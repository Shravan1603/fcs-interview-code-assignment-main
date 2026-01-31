# Project Explanation Guide

This document explains the project structure, all implemented tasks, design decisions, testing strategy, and how to present each part during the case study evaluation.

---

## 1. Project Overview

This is a **Quarkus 3.13.3** Java application that implements a simplified **Warehouse Colocation Management System**. It manages four entities:

| Entity | Purpose |
|---|---|
| **Location** | Geographical place (city) with warehouse constraints (max count, max capacity) |
| **Store** | Physical store where Products are sold to customers |
| **Warehouse** | Storage facility where Products are kept before distribution to Stores |
| **Product** | Goods sold to customers in Stores |

### Architecture

The project follows **Hexagonal Architecture** (Ports and Adapters) for the Warehouse module:

```
REST API (Adapter)  -->  Use Cases (Domain)  -->  Ports (Interfaces)  -->  Database Adapter
WarehouseResourceImpl    CreateWarehouseUseCase    WarehouseStore          WarehouseRepository
                         ArchiveWarehouseUseCase   LocationResolver        LocationGateway
                         ReplaceWarehouseUseCase   CreateWarehouseOp
                                                   ArchiveWarehouseOp
                                                   ReplaceWarehouseOp
```

The Product and Store modules use simpler patterns (PanacheRepository / PanacheEntity).

### Tech Stack

- **Framework**: Quarkus 3.13.3 with RESTEasy Reactive, Hibernate ORM Panache
- **Database**: PostgreSQL (production/dev), H2 (tests)
- **API**: OpenAPI-first code generation for Warehouse endpoints
- **Testing**: JUnit 5, Mockito, REST Assured, JaCoCo
- **Build**: Maven, GitHub Actions CI/CD
- **Health**: SmallRye Health (liveness + readiness)

---

## 2. Tasks Implemented

### Task 1: LocationGateway.resolveByIdentifier() (Must Have)

**File**: `src/main/java/com/fulfilment/application/monolith/location/LocationGateway.java`

**What was the problem?**
The `resolveByIdentifier` method was throwing `UnsupportedOperationException`. The system had a static list of predefined locations but no way to look one up by its identifier.

**What was implemented?**
A simple stream filter on the static `locations` list to find a location by its `identification` field. Returns the matching `Location` or `null` if not found.

**How to explain it:**
> "This was the foundational task. The LocationGateway holds an in-memory list of valid locations (AMSTERDAM-001, EINDHOVEN-001, etc.) each with a max warehouse count and max capacity. I implemented the lookup method using Java Streams -- filter by identifier, findFirst, orElse null. This is a prerequisite for the warehouse validation logic since every warehouse creation or replacement must verify the location exists."

**Key code:**
```java
@Override
public Location resolveByIdentifier(String identifier) {
    return locations.stream()
        .filter(location -> location.identification.equals(identifier))
        .findFirst()
        .orElse(null);
}
```

---

### Task 2: Store - LegacyStoreManagerGateway After DB Commit (Must Have)

**Files changed:**
- `src/main/java/com/fulfilment/application/monolith/stores/StoreEvent.java` (new)
- `src/main/java/com/fulfilment/application/monolith/stores/StoreResource.java` (modified)
- `src/main/java/com/fulfilment/application/monolith/stores/LegacyStoreManagerGateway.java` (modified)

**What was the problem?**
The `StoreResource` was calling `LegacyStoreManagerGateway` directly inside the transaction. If the transaction rolled back after the gateway call, the legacy system would have received data that was never actually committed -- creating data inconsistency between our system and the legacy system.

**What was implemented?**
A **CDI Event-based** solution using `TransactionPhase.AFTER_SUCCESS`:

1. Created a `StoreEvent` class that carries the Store entity and the operation type (CREATED or UPDATED)
2. Modified `StoreResource` to fire CDI events (`storeEvent.fire(...)`) instead of calling the gateway directly
3. Modified `LegacyStoreManagerGateway` to observe these events with `@Observes(during = TransactionPhase.AFTER_SUCCESS)`, ensuring it only executes after the database transaction commits successfully

**How to explain it:**
> "The original code had a race condition: it notified the legacy system before the database commit. If the commit failed, the legacy system would have stale data. I solved this using CDI Events with transactional observers. When a store is created or updated, the resource fires a CDI event. The LegacyStoreManagerGateway observes this event but only processes it during `TransactionPhase.AFTER_SUCCESS` -- meaning the observer method only executes if and when the transaction commits successfully. This is a Quarkus/CDI built-in mechanism and doesn't require any additional frameworks."

**Key pattern:**
```java
// StoreResource fires the event inside the transaction
storeEvent.fire(new StoreEvent(store, StoreEvent.Type.CREATED));

// LegacyStoreManagerGateway only processes AFTER commit succeeds
public void onStoreEvent(
    @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreEvent event) {
    switch (event.getType()) {
        case CREATED -> createStoreOnLegacySystem(event.getStore());
        case UPDATED -> updateStoreOnLegacySystem(event.getStore());
    }
}
```

---

### Task 3: Warehouse - Creation, Replacement, and Archive Logic (Must Have)

This was the largest task. It involved implementing the full warehouse lifecycle.

#### 3a. Warehouse Creation

**Files:**
- `CreateWarehouseUseCase.java` -- domain logic
- `WarehouseResourceImpl.java` -- REST adapter
- `WarehouseRepository.java` -- database adapter

**Validations implemented (5 checks):**

| # | Validation | Error if violated |
|---|---|---|
| 1 | Business Unit Code uniqueness | "A warehouse with BU code 'X' already exists" |
| 2 | Location must be valid | "Location 'X' is not a valid location" |
| 3 | Max warehouses at location | "Maximum number of warehouses (N) already reached" |
| 4 | Capacity does not exceed location max | "Would exceed the maximum capacity for location" |
| 5 | Stock does not exceed warehouse capacity | "Stock cannot exceed warehouse capacity" |

**How to explain it:**
> "The creation use case follows the validation-first pattern. Before persisting anything, all five business rules are checked in sequence. Each violation throws a WarehouseValidationException which the REST adapter catches and translates to an HTTP 400 response. The use case depends only on ports (WarehouseStore, LocationResolver) -- not on database or HTTP concerns. This makes it fully unit-testable with Mockito mocks."

#### 3b. Warehouse Archive

**File:** `ArchiveWarehouseUseCase.java`

**Logic:**
1. Find the active warehouse by Business Unit Code
2. If not found, throw a validation exception
3. Set `archivedAt = LocalDateTime.now()` on the warehouse
4. Persist the update

**How to explain it:**
> "Archiving is a soft-delete. We set the archivedAt timestamp instead of physically deleting the record. This preserves the warehouse's history for cost tracking, audit trails, and compliance. The getAll() query in the repository filters by 'archivedAt is null' so archived warehouses don't appear in active listings but remain in the database."

#### 3c. Warehouse Replacement

**File:** `ReplaceWarehouseUseCase.java`

This is the most complex operation. It reuses the Business Unit Code from an old warehouse for a new one.

**Validation sequence (8 steps):**

| Step | Action |
|---|---|
| 1 | Find existing active warehouse by BU code |
| 2 | Validate new capacity >= old stock (capacity accommodation) |
| 3 | Validate new stock == old stock (stock matching) |
| 4 | Validate new location exists |
| 5 | Archive the existing warehouse (set archivedAt) |
| 6 | Check warehouse count at new location (after archiving) |
| 7 | Check total capacity at new location (after archiving) |
| 8 | Create the new warehouse with same BU code |

**How to explain it:**
> "Replacement is essentially archive + create in a single transactional operation. The key subtlety is that location constraints (max warehouses, max capacity) must be checked AFTER archiving the old warehouse -- because if the new warehouse goes to the same location, archiving frees up a slot. The BU code is reused to maintain business continuity and cost history tracking."

#### 3d. REST API Layer

**File:** `WarehouseResourceImpl.java`

Implements the OpenAPI-generated `WarehouseResource` interface with 4 endpoints:

| Endpoint | Method | Description |
|---|---|---|
| `/warehouse` | GET | List all active warehouses |
| `/warehouse` | POST | Create a new warehouse |
| `/warehouse/{id}` | GET | Get warehouse by Business Unit Code |
| `/warehouse/{id}` | DELETE | Archive warehouse by BU Code |
| `/warehouse/{id}/replace` | POST | Replace warehouse |

Each endpoint is `@Transactional` where needed, catches `WarehouseValidationException`, and maps between API beans (generated from OpenAPI) and domain models.

#### 3e. Database Layer

**File:** `WarehouseRepository.java`

Implements `WarehouseStore` port interface using Quarkus Panache:

- `getAll()` -- filters by `archivedAt is null` (only active warehouses)
- `create()` -- maps domain Warehouse to DbWarehouse entity and persists
- `update()` -- finds by BU code and updates all fields
- `remove()` -- deletes the entity
- `findByBusinessUnitCode()` -- queries by BU code with archived filter
- `findActiveByLocation()` -- queries by location with archived filter

---

## 3. Testing Strategy

### Test Classes and Coverage

| Test Class | Type | Tests | What it covers |
|---|---|---|---|
| `LocationGatewayTest` | Unit | 7 | All known locations, non-existent, edge cases |
| `CreateWarehouseUseCaseTest` | Unit (Mockito) | 8 | All 5 validations + happy path + edge cases |
| `ArchiveWarehouseUseCaseTest` | Unit (Mockito) | 3 | Happy path, not found, timestamp |
| `ReplaceWarehouseUseCaseTest` | Unit (Mockito) | 8 | All validations + capacity exceeded + same location |
| `WarehouseEndpointTest` | Integration (@QuarkusTest) | 11 | Full REST API + DB integration |
| `StoreEndpointTest` | Integration (@QuarkusTest) | 13 | CRUD + PATCH + error cases |
| `ProductEndpointTest` | Integration (@QuarkusTest) | 10 | CRUD + error cases |
| **Total** | | **60** | |

### Test Results

```
Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### JaCoCo Code Coverage

| Metric | Result | Target |
|---|---|---|
| **Instruction Coverage** | **89.7%** | 80%+ |
| **Line Coverage** | **90.4%** | 80%+ |
| **Branch Coverage** | 72.6% | -- |

### How to explain the testing approach:

> "I followed a pyramid testing strategy:
>
> **Unit tests** (Mockito) for the three warehouse use cases -- these test all business validation rules in isolation. Each validation branch has at least one positive and one negative test. Dependencies are mocked so tests run in milliseconds.
>
> **Integration tests** (@QuarkusTest with REST Assured) for all REST endpoints. These verify the full stack: HTTP request through CDI injection, transaction management, JPA persistence, to database. The test database is H2 in-memory (configured in test application.properties) so no Docker is needed.
>
> **Code coverage** is tracked with JaCoCo (quarkus-jacoco extension). We achieved 89.7% instruction coverage and 90.4% line coverage, exceeding the 80% target."

### Test Infrastructure Decisions

- **H2 for tests**: Used H2 in-memory database with `MODE=PostgreSQL` to avoid requiring Docker for tests. This makes the CI pipeline simpler and tests run faster.
- **Byte Buddy experimental**: Added `net.bytebuddy.experimental=true` to handle Java 23 compatibility with Quarkus 3.13.3's Hibernate.
- **Ordered tests**: Integration tests use `@TestMethodOrder(OrderAnnotation.class)` because they share database state (e.g., create before delete).

---

## 4. Additional Implementations

### Health Checks (Good to Have)

**Dependency added**: `quarkus-smallrye-health`

This provides automatic health check endpoints:

| Endpoint | Purpose |
|---|---|
| `/q/health` | Combined health status |
| `/q/health/live` | Liveness probe (is the app running?) |
| `/q/health/ready` | Readiness probe (can it serve requests?) |

Quarkus automatically includes a datasource health check that verifies the database connection is alive.

**How to explain it:**
> "I added SmallRye Health for production readiness. The liveness probe tells Kubernetes the JVM is running. The readiness probe checks if the database connection is healthy. No custom health check code was needed -- Quarkus auto-discovers the datasource and adds a readiness check."

### CI/CD Pipeline (Good to Have)

**File**: `.github/workflows/ci.yml`

The pipeline:
1. Runs on push/PR to main/master branches
2. Starts a PostgreSQL 13.3 service container
3. Sets up JDK 17 (Temurin) with Maven cache
4. Runs `./mvnw clean verify`
5. Uploads JaCoCo coverage report as artifact
6. Uploads Surefire test results as artifact

**How to explain it:**
> "The CI pipeline uses GitHub Actions. It spins up a PostgreSQL service container matching the prod config (port 15432, quarkus_test/quarkus_test), builds the project, and runs all tests. Coverage and test reports are uploaded as artifacts for review. This ensures every PR is validated before merge."

---

## 5. Architecture Questions (QUESTIONS.md)

### Q1: Database access patterns -- would you refactor?

**Key points to mention:**
- Three different patterns exist: PanacheRepository (Product), PanacheEntity/Active Record (Store), Hexagonal with ports (Warehouse)
- Would standardize on Repository pattern -- active record makes unit testing harder (can't mock static methods)
- Warehouse's hexagonal approach is ideal for complex domains but overkill for simple CRUD
- Priority: refactor Store module away from active record first

### Q2: OpenAPI-first vs Code-first

**Key points to mention:**
- OpenAPI-first: contract-driven, better for multi-team/multi-consumer APIs, adds build complexity
- Code-first: faster iteration, easier debugging, risk of undocumented drift
- Recommendation: OpenAPI-first for core business APIs (Warehouse), code-first for simple CRUD (Product)
- Add SmallRye OpenAPI annotations to code-first endpoints for auto-generated docs

### Q3: Testing prioritization

**Key points to mention:**
- Priority 1: Unit tests for domain use cases (highest value-to-cost ratio)
- Priority 2: Integration tests for REST endpoints (catch wiring issues)
- Priority 3: End-to-end tests (minimal smoke tests)
- Enforce 80%+ coverage threshold in CI
- Focus on branch coverage for validation logic

---

## 6. Case Study Scenarios (CASE_STUDY.md)

### Scenario 1: Cost Allocation and Tracking
- Key challenge: shared resource attribution across warehouses/stores
- Solution approach: Activity-Based Costing (ABC)
- Ask: What decisions should cost data inform?

### Scenario 2: Cost Optimization
- Strategies: inventory placement optimization, utilization balancing, demand-driven replenishment
- Prioritize by: data analysis first, then high-impact/low-effort items
- Ask: What % does each cost category represent?

### Scenario 3: Financial System Integration
- Why: single source of truth, timely decisions, audit compliance
- How: canonical data model + event-driven integration (Kafka) + idempotency
- Ask: What financial systems exist? Acceptable latency?

### Scenario 4: Budgeting and Forecasting
- Key: driver-based budgeting (cost = f(volume, SKU count, warehouse size))
- Need: 12-18 months historical data, scenario modeling, variance tracking
- Ask: Is there a planning cycle?

### Scenario 5: Cost Control in Warehouse Replacement
- Preserving cost history is critical for ROI calculation and trend analysis
- The `archivedAt` design supports this -- soft-delete preserves records
- Budget for: transition overlap costs, stock transfer costs, ramp-up inefficiency

---

## 7. How to Run the Project

### Prerequisites
- Java 17+ (Temurin/Adoptium recommended)
- Maven 3.9+
- PostgreSQL (for dev mode)

### Run Tests
```bash
cd java-assignment
mvn test
```

### Run in Dev Mode
```bash
cd java-assignment
mvn quarkus:dev
```
Then open http://localhost:8080 in browser.

### Available Endpoints
| URL | Description |
|---|---|
| http://localhost:8080 | Product Management UI (index.html) |
| http://localhost:8080/product | Product REST API |
| http://localhost:8080/store | Store REST API |
| http://localhost:8080/warehouse | Warehouse REST API |
| http://localhost:8080/q/health | Health check |

### View Coverage Report
After running tests, open: `java-assignment/target/jacoco-report/index.html`

---

## 8. Key Design Decisions Summary

| Decision | Rationale |
|---|---|
| CDI Events for Store-Legacy sync | Guarantees post-commit execution via TransactionPhase.AFTER_SUCCESS |
| Soft-delete for warehouse archiving | Preserves history for cost tracking and audit compliance |
| Validation-first in use cases | All business rules checked before any persistence, clear error messages |
| H2 for test profile | No Docker dependency for tests, faster CI |
| OpenAPI code generation for Warehouse | Contract-first design for the most complex API |
| Hexagonal architecture for Warehouse | Clean separation of domain logic from infrastructure concerns |
| WarehouseValidationException | Single exception type for all business rule violations, caught at REST layer |

---

## 9. What Was NOT Implemented

| Item | Reason |
|---|---|
| **BONUS task** (Product-Warehouse-Store association) | Marked as "nice to have" in the assignment; focused on must-have tasks first |
| Custom health checks | SmallRye Health auto-discovers datasource; no custom checks needed for this scope |
| API authentication/authorization | Not in scope for this assignment |

---

## 10. Challenges Encountered and Solutions

| Challenge | Solution |
|---|---|
| Docker not available for Quarkus DevServices (test profile) | Added H2 in-memory database with `MODE=PostgreSQL` for test profile |
| Java 23 incompatibility with Byte Buddy in Quarkus 3.13.3 | Added `net.bytebuddy.experimental=true` system property to surefire plugin |
| OpenAPI-generated interface returns 200 for POST (not 201) | Adjusted test expectations to match framework behavior |
| Ensuring LegacyStoreManagerGateway runs after DB commit | Used CDI transactional observers instead of direct method calls |
| Port 8080 conflicts in dev mode | Killed existing process or configured alternative port |
| PostgreSQL database not found in dev mode | Created `quarkus_test` database via psql before starting |
