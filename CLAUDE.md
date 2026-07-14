# CLAUDE.md — Shipping Mini-API

## 1. Project Overview

WMS Shipping Mini-API is a full-stack monorepo built by Picard Coding Projekt. It implements a simplified warehouse shipping workflow:

**Create Orders → Create Shipments → Label Packages → Pack → Ship**

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.3.2 |
| Frontend | Angular 18.2, TypeScript 5.5 |
| Database | PostgreSQL 16 (Docker) |
| Migrations | Flyway |

---

## 2. Repository Layout

```
shipping-mini-api_refactored_angular_refactored/
├── .env                          # Local DB credentials (git-ignored)
├── docker-compose.yml            # PostgreSQL 16 service
├── pom.xml                       # Maven build descriptor
├── src/
│   ├── main/
│   │   ├── java/com/picard/shipping/
│   │   │   ├── controller/       # REST controllers
│   │   │   ├── service/          # Business logic
│   │   │   ├── repository/       # Spring Data JPA repositories
│   │   │   ├── domain/           # JPA entities + enums
│   │   │   ├── dto/              # Java Records (request/response)
│   │   │   └── exception/        # Custom exceptions + GlobalExceptionHandler
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/     # Flyway SQL migrations
│   └── test/
│       ├── java/com/picard/shipping/
│       └── resources/
│           └── application-test.yml
└── frontend/
    ├── proxy.conf.json           # Dev proxy: /api → localhost:8080
    ├── angular.json
    ├── package.json
    └── src/app/
        ├── models/models.ts      # All shared TypeScript interfaces
        ├── services/             # HTTP services (order, shipment)
        ├── components/           # Shared reusable components
        └── pages/                # Lazy-loaded route pages
```

---

## 3. Running the Project

**Prerequisites:** Java 21, Maven, Node 18+, Docker

```bash
# 1. Start the database
docker compose up -d

# 2. Start the backend (port 8080)
./mvnw spring-boot:run

# 3. Start the frontend (port 4200)
cd frontend
npm install
ng serve
```

The Angular dev server proxies all `/api/**` requests to `http://localhost:8080` via `proxy.conf.json`.

---

## 4. Environment Variables

Credentials are stored in `.env` (git-ignored) and referenced by both `docker-compose.yml` and `application.yml`:

| Variable | Description |
|---|---|
| `DB_URL` | JDBC connection URL |
| `DB_USER` | Database username |
| `DB_PASSWORD` | Database password |

---

## 5. Backend Guidelines

### Architecture

Strict layered architecture — data flows in one direction only:

```
Controller → Service → Repository → Domain
```

Never skip layers (e.g. no repository access from controllers).

### DTOs vs. Entities

- **DTOs** are Java Records — immutable, no `get`-prefixed accessors.
- **JPA Entities** use Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`) — never use records for entities.
- No MapStruct — mapping is done via private `toResponse()` methods inside service classes.

### Transactions

- Annotate at the **service class level** with `@Transactional`.
- Override with `@Transactional(readOnly = true)` on all query methods.
- `open-in-view: false` — never rely on lazy-loading outside of a transaction boundary.

### Domain Behavior

- Business rules and invariants belong on **entity methods** (e.g. `Shipment.allPackagesHaveTracking()`, `Order.addPosition()`).
- Services orchestrate — they do not duplicate domain logic.

### State Machine

- Shipment status transitions (`CREATED → PACKED → SHIPPED`) are strictly enforced in `ShipmentService`.
- Illegal transitions throw `InvalidStateException` (HTTP 422). Never bypass state checks.

### Exception Handling

All exceptions are mapped by `GlobalExceptionHandler` (`@RestControllerAdvice`) to a uniform error envelope:

```json
{
  "timestamp": "...",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "..."
}
```

| Exception | HTTP Status |
|---|---|
| `NotFoundException` | 404 |
| `ConflictException` | 409 |
| `InvalidStateException` | 422 |
| `MethodArgumentNotValidException` | 400 |
| `DataIntegrityViolationException` | 409 |

### Database / Flyway

- Schema is fully managed by **Flyway**. Never use `ddl-auto: create` or `update` outside of tests.
- Use `left join fetch` in JPQL queries to avoid N+1 problems (see `ShipmentRepository.search()`).
- All primary keys are UUIDs (`gen_random_uuid()`).

---

## 6. Backend Testing

| Test type | Approach |
|---|---|
| Integration tests | Extend `AbstractIntegrationTest` — `@SpringBootTest(RANDOM_PORT)` + Testcontainers PostgreSQL |
| Controller tests | `@WebMvcTest` + `@MockBean` for HTTP layer isolation |
| Service unit tests | `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` |

- Test profile (`application-test.yml`): Flyway disabled, `ddl-auto: create-drop` — Hibernate manages schema.
- Do not connect to a real external database unless extending `AbstractIntegrationTest`.

---

## 7. Frontend Guidelines

### Component Architecture

- **Standalone components only** — never create or add `NgModule`.
- All page components are **lazy-loaded** via `loadComponent` in `app.routes.ts`.

### Dependency Injection

- Use the **`inject()` function** — never constructor injection.

### Route Parameters

- Use **`input.required<string>()`** signal-based inputs for route-bound parameters.
- Enabled globally via `withComponentInputBinding()` in `app.config.ts`.

### Services

- All services use `providedIn: 'root'` (tree-shakeable singletons).
- HTTP methods return `Observable<T>` — never subscribe inside a service.

### Error Handling

```typescript
this.service.doSomething().subscribe({
  next: (data) => { ... },
  error: (err) => {
    this.apiError = err.error as ApiError;
  }
});
```

Always cast `err.error as ApiError` and display `apiError.message` in the template.

### Styling

- Use **CSS custom properties** for all design tokens (`--accent`, `--border`, `--text-muted`, `--font-mono`, etc.).
- No inline hex/rgb values in component styles — reference variables only.
- No external CSS framework.

### UI Language

- All UI-facing strings are in **German** (`Aufträge`, `Sendungen`, etc.).
- Code (variables, methods, routes) remains in English.

---

## 8. Frontend Models

All TypeScript interfaces live in a single file:

```
frontend/src/app/models/models.ts
```

- Mirror backend DTOs exactly when adding new types.
- Do not define types inline in components or services.
- `ShipmentStatus` is a union type: `'CREATED' | 'PACKED' | 'SHIPPED'`.

---

## 9. API Contract

| Description | Method | Path |
|---|---|---|
| Create order | `POST` | `/api/v1/orders` |
| List all orders | `GET` | `/api/v1/orders` |
| Get order by ID | `GET` | `/api/v1/orders/{id}` |
| Create shipment for order | `POST` | `/api/v1/shipments/order/{orderId}` |
| Get shipment by ID | `GET` | `/api/v1/shipments/{id}` |
| Search/filter shipments | `GET` | `/api/v1/shipments?status=&carrier=` |
| Label a package | `PATCH` | `/api/v1/shipments/{id}/packages/{pkgId}` |
| Pack shipment | `POST` | `/api/v1/shipments/{id}/pack` |
| Ship shipment | `POST` | `/api/v1/shipments/{id}/ship` |

Frontend services use relative URLs (e.g. `/api/v1/orders`) — the dev proxy handles the host resolution.

---

## 10. Further Reading

For detailed coding guidelines and architecture decisions, see the dedicated docs:

| Document | Scope |
|---|---|
| [`BACKEND.md`](./BACKEND.md) | Java / Spring Boot — layered architecture, SOLID & DRY principles, transactions, exception handling, Flyway, testing |
| [`FRONTEND.md`](./FRONTEND.md) | Angular / TypeScript — component architecture, routing, services, state management, SOLID & DRY principles, styling, testing |
