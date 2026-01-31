# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I would refactor toward a consistent repository-based approach across all modules.

Currently we have three different strategies:
- Product: Uses PanacheRepository (ProductRepository) -- a dedicated repository class that
  the resource injects.
- Store: Uses PanacheEntity (active record pattern) -- the entity itself has static methods
  like Store.findById(), Store.listAll(), etc. No separate repository class.
- Warehouse: Uses PanacheRepository (WarehouseRepository) implementing a domain port
  (WarehouseStore) with a separate DbWarehouse entity -- following hexagonal architecture
  with clean separation between domain models and persistence.

My refactoring priorities:

1. Standardize on the Repository pattern (not active record). The Store module's active
   record approach (PanacheEntity) tightly couples the entity to persistence logic, making
   unit testing harder because static methods can't be mocked easily. The Repository pattern
   used by Product and Warehouse is more testable and follows the single responsibility
   principle.

2. Consider adopting the Warehouse module's hexagonal style for Product and Store if the
   domain grows in complexity. The separation of domain model (Warehouse) from persistence
   entity (DbWarehouse) and the use of port interfaces (WarehouseStore) provide better
   decoupling. However, for simpler CRUD modules like Product, the overhead of maintaining
   two model classes may not be justified initially.

3. The Store module is the most urgent to refactor because the active record pattern makes
   it difficult to write isolated unit tests for StoreResource -- you can't mock
   Store.findById() without a running Quarkus test context. Moving to a StoreRepository
   would fix this.

In summary: converge on Repository + domain ports where the business logic warrants it,
and at minimum use PanacheRepository for simpler modules.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
OpenAPI-first (code generation from YAML spec):

Pros:
- Contract-first design ensures the API is well-documented before implementation begins.
- The generated interface acts as a stable contract -- implementors must satisfy it.
- Clients (frontend, other services) can generate their own stubs from the same spec.
- Changes to the API surface are explicit and reviewable in the YAML diff.
- Encourages thinking about the API from the consumer's perspective.

Cons:
- Adds build complexity (code generation plugin, generated source management).
- Debugging is harder when generated code is involved.
- Keeping the YAML in sync with evolving requirements requires discipline.
- The generated interface may not align perfectly with framework idioms (e.g., Quarkus
  annotations, CDI scoping), requiring adapters or workarounds.
- Slower feedback loop during development: change YAML -> regenerate -> implement.

Code-first (Product & Store approach):

Pros:
- Faster to develop and iterate -- write the endpoint directly.
- Full control over annotations, return types, and framework-specific features.
- No tooling or plugin overhead.
- Easier to debug since all code is hand-written.

Cons:
- The API documentation is an afterthought (generated from code annotations if at all).
- Risk of inconsistency between what's documented and what's implemented.
- Harder for consumers to get a reliable contract to code against.
- API design decisions are mixed into implementation details.

My choice:
For a production system with multiple consumers, I'd choose OpenAPI-first. The upfront
investment in defining the contract pays off in reduced integration issues and better
communication between teams. For internal or rapidly prototyping services with few
consumers, code-first is pragmatic.

For this specific codebase, a hybrid approach would work well: use OpenAPI-first for the
core Warehouse API (as it already does) since it has complex business rules, and keep
code-first for simpler CRUD endpoints like Product. Regardless, I'd add Quarkus SmallRye
OpenAPI annotations to the code-first endpoints to at least auto-generate documentation.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority order for this project:

1. Unit tests for domain use cases (highest priority). The CreateWarehouseUseCase,
   ArchiveWarehouseUseCase, and ReplaceWarehouseUseCase contain the core business
   validation logic. These are fast, isolated (mockable dependencies), and cover the
   most critical paths. I'd test every validation rule -- positive, negative, and edge
   cases. These give the highest value-to-cost ratio.

2. Unit tests for gateway/adapter logic. LocationGateway, WarehouseRepository -- test
   the mapping and query logic. LocationGateway is pure logic and trivially testable.
   Repository tests may need a test database (Quarkus DevServices handles this).

3. Integration tests for REST endpoints (@QuarkusTest). These verify the full stack:
   HTTP request -> resource -> use case -> repository -> database. Focus on the happy
   path and key error responses (400, 404). These catch wiring issues that unit tests
   miss (e.g., CDI injection, transaction boundaries, JSON serialization).

4. End-to-end integration tests (@QuarkusIntegrationTest). Run against the packaged
   application to verify the build artifact works. Keep these minimal -- one or two
   smoke tests per major flow.

Strategies to maintain coverage effectiveness over time:

- Enforce a coverage threshold (80%+) via JaCoCo in the CI pipeline. Fail the build
  if coverage drops below the threshold.
- Focus on branch coverage, not just line coverage -- this ensures conditional logic
  (validation branches) is actually tested.
- Treat test failures as build-breaking. No skipping or ignoring tests.
- Write tests alongside implementation (not as an afterthought). Each PR that adds
  business logic should include corresponding unit tests.
- Periodically review coverage reports to identify untested code paths, especially
  in error handling and edge cases.
- Keep integration tests stable by using deterministic test data (import.sql) and
  test ordering where state dependencies exist.
```
