# BACKEND.md — Spring Boot Architecture & Coding Guidelines

## 1. Layered Architecture

All code is organized into strict layers. Data flows in one direction only — never skip layers.

```
Controller → Service → Repository → Domain
```

| Layer | Responsibility |
|---|---|
| `controller` | HTTP binding, input validation, response status/headers |
| `service` | Business logic, orchestration, transaction boundaries |
| `repository` | Data access only — queries, persistence, existence checks |
| `domain` | Entities, value objects, enums, domain behavior |
| `dto` | Immutable request/response types (Java Records) |
| `exception` | Custom exception classes + `GlobalExceptionHandler` |
| `config` | Spring beans, security, CORS, and other configuration |

**Rules:**
- Controllers never access repositories directly.
- Repositories never contain business logic.
- Services never format HTTP responses or build `ResponseEntity`.
- Domain entities never depend on Spring beans.

---

## 2. Package Structure

```
com.example.app/
├── controller/
├── service/
├── repository/
├── domain/
├── dto/
├── exception/
└── config/
```

---

## 3. Controllers

Controllers are thin HTTP adapters. They validate input and delegate immediately to the service — no business logic here.

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        URI location = URI.create("/api/v1/orders/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }
}
```

**Rules:**
- Always use `@Valid @RequestBody` for inbound request validation — never validate manually in the controller.
- Use `ResponseEntity` only when you need to control status codes or headers. Plain return types are fine for implicit `200 OK`.
- `POST` endpoints that create a new resource must return `201 Created` with a `Location` header pointing to the new resource.
- Use `@PathVariable UUID id` for resource identifiers — let Spring convert the String to UUID automatically.
- Do not catch exceptions in controllers — let `GlobalExceptionHandler` handle them.

---

## 4. Services

Services own all business logic and define transaction boundaries.

```java
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = findOrderOrThrow(id);
        return toResponse(order);
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        if (orderRepository.existsByExternalOrderNumber(request.externalOrderNumber())) {
            throw new ConflictException("Order already exists: " + request.externalOrderNumber());
        }
        Order order = new Order();
        // ... build and save
        return toResponse(orderRepository.save(order));
    }

    private Order findOrderOrThrow(UUID id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Order not found: {}", id);
                return new NotFoundException("Order not found: " + id);
            });
    }

    private OrderResponse toResponse(Order order) {
        // manual mapping
    }
}
```

**Rules:**
- Annotate the service class with `@Transactional` (write default for all methods).
- Override with `@Transactional(readOnly = true)` on every query method — this optimizes DB performance and makes intent explicit.
- Add `@Slf4j` to every service. Log at `error` on not-found and unexpected state, at `info` or `debug` on significant business events.
- Use constructor injection via `@RequiredArgsConstructor` — never `@Autowired` field injection.
- Extract repeated `orElseThrow` + logging patterns into a private helper method (e.g. `findOrderOrThrow(UUID id)`).
- Mapping from entity to response DTO is done via a single private `toResponse()` method per service — no public mapper beans, no MapStruct unless the project explicitly adopts it.
- Rely on Hibernate dirty tracking within `@Transactional` write methods — no need to call `save()` after mutating a managed entity. Document this convention so it is not surprising to readers.
- Never call `save()` inside a `@Transactional(readOnly = true)` method.

---

## 5. Repositories

Repositories are pure data access interfaces. No logic, no orchestration.

```java
public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByExternalOrderNumber(String externalOrderNumber);

    @Query("""
        select o from Order o
        left join fetch o.positions
        where o.status = :status
        """)
    List<Order> findByStatusWithPositions(@Param("status") OrderStatus status);
}
```

**Rules:**
- Extend `JpaRepository<T, UUID>` — UUID primary keys are the standard.
- Use derived query methods (`existsByX`, `findByX`) for simple lookups.
- Use `@Query` with `left join fetch` whenever loading a parent entity that has collections — prevents N+1 queries.
- For optional filter parameters use the `:param is null or field = :param` idiom in JPQL — avoids multiple query paths.
- No business logic, no exception throwing, no logging in repositories.

---

## 6. Domain Entities

Entities represent the core domain model. They may carry domain behavior but have no Spring dependencies.

```java
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderPosition> positions = new ArrayList<>();

    public void addPosition(OrderPosition position) {
        position.setOrder(this);
        this.positions.add(position);
    }

    public boolean hasPositions() {
        return positions != null && !positions.isEmpty();
    }
}
```

**Rules:**
- Use Lombok `@Getter`, `@Setter`, `@NoArgsConstructor` — never use Java records for JPA entities.
- Bidirectional relationship management belongs on entity methods (e.g. `addPosition()`), not in services. This prevents inconsistent in-memory state.
- Domain invariant checks (e.g. `allPackagesHaveTracking()`) belong on entity methods, not services.
- All `@ManyToOne` and `@OneToOne` associations must use `FetchType.LAZY`. Eager fetching is a performance anti-pattern that loads data you may not need.
- Embedded value objects (e.g. `Address`) use `@Embeddable` / `@Embedded` — do not create a separate table for simple value objects with no identity.
- Use `@CreationTimestamp` for audit fields — never set timestamps manually.
- Store enums as strings: `@Enumerated(EnumType.STRING)`. Never use `ORDINAL` — it breaks when enum order changes.
- Initialize collection fields inline (`= new ArrayList<>()`) to avoid `NullPointerException` on unmapped entities.

---

## 7. DTOs

All request and response objects are **Java Records** — immutable, concise, no boilerplate.

```java
// Inbound — validation annotations here
public record CreateOrderRequest(
    @NotBlank String externalOrderNumber,
    @Valid @NotNull AddressDto deliveryAddress,
    @Valid @NotEmpty List<OrderPositionDto> positions
) {}

// Outbound — no validation annotations needed
public record OrderResponse(
    UUID id,
    String externalOrderNumber,
    AddressDto deliveryAddress,
    OrderStatus status,
    Instant createdAt
) {}
```

**Rules:**
- DTOs are always Java Records — never classes with getters/setters.
- Validation annotations (`@NotBlank`, `@Min`, `@Valid`, `@NotNull`, `@NotEmpty`) belong on **inbound request records only**. Response records are serialized outbound — they do not need validation annotations.
- Use `@Valid` on nested records to cascade validation.
- Use projection records (e.g. `ShipmentSummaryDto`) when a full nested response would cause circular serialization.
- Never expose JPA entities directly from controllers — always map to a response DTO.
- Keep all DTO types in the `dto` package — do not define them inline in controllers or services.

---

## 8. SOLID Principles

### S — Single Responsibility

Every class has exactly one reason to change.

- Each controller handles one resource (`OrderController` → orders only).
- Each service owns one domain concept's business logic.
- `GlobalExceptionHandler` is the single place for mapping exceptions to HTTP responses — no `try/catch` with `ResponseEntity` building in controllers or services.
- Configuration classes (`@Configuration`) are separate from business logic.

### O — Open/Closed

Open for extension, closed for modification.

- Status enums are designed for extension — add new statuses without touching existing logic.
- Adding a new exception type means creating a new class and a new `@ExceptionHandler` method. Existing handlers are not touched.
- If a state machine grows beyond 3–4 states, replace `if/else` chains with a transition map or the State pattern so new states do not require modifying existing methods.

### L — Liskov Substitution

Subtypes must honor the contract of their parent.

- Custom exceptions (`NotFoundException`, `ConflictException`, `InvalidStateException`) must not add side effects or change the semantic contract of `RuntimeException`.
- Service implementations must be substitutable for their interfaces if interfaces are introduced.

### I — Interface Segregation

Keep interfaces small and focused.

- Repository interfaces must remain minimal — do not add query methods that only one caller uses when the logic belongs in the service.
- Do not create fat service interfaces that bundle unrelated methods. Split by responsibility.
- Avoid exposing internal entity-access methods (e.g. `getOrderEntity()`) as part of a public API contract — keep them package-private.

### D — Dependency Inversion

Depend on abstractions, not on concretions.

- Always inject dependencies via constructor (`@RequiredArgsConstructor`) — never `@Autowired` field injection. Constructor injection makes dependencies explicit and enables easy testing without a Spring context.
- Services depend on repository interfaces, not on concrete implementations.
- Do not instantiate collaborators with `new` inside a Spring-managed bean.

---

## 9. DRY Principles

Do not Repeat Yourself — every piece of knowledge should have a single, authoritative representation.

**Extract repeated patterns into helpers:**

```java
// Bad — duplicated in getOrder() and getOrderEntity()
orderRepository.findById(id).orElseThrow(() -> {
    log.error("Order not found: {}", id);
    return new NotFoundException("Order not found: " + id);
});

// Good — single private helper
private Order findOrderOrThrow(UUID id) {
    return orderRepository.findById(id)
        .orElseThrow(() -> {
            log.error("Order not found: {}", id);
            return new NotFoundException("Order not found: " + id);
        });
}
```

**Rules:**
- Extract any `orElseThrow` + logging pattern that appears more than once into a private helper.
- The error response envelope is built in one place only — `GlobalExceptionHandler.build()`. Never construct error bodies inline elsewhere.
- `toResponse()` is the single mapping point per service. Never map entity → DTO in the controller or in a second place in the service.
- Pre-check uniqueness with `existsByX()` before saving. Do not rely on catching `DataIntegrityViolationException` as primary flow control — it is a last-resort safety net for race conditions only.
- Shared validation logic that appears on multiple DTOs should be extracted into a custom constraint annotation.

---

## 10. Exception Handling

A consistent, centralized exception strategy makes the API predictable for clients.

### Exception Hierarchy

| Exception | HTTP Status | When to use |
|---|---|---|
| `NotFoundException` | `404 Not Found` | Resource does not exist |
| `ConflictException` | `409 Conflict` | Uniqueness violation, duplicate resource |
| `InvalidStateException` | `422 Unprocessable Entity` | Illegal state transition, unmet precondition |

### GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "Data integrity violation");
    }

    private ResponseEntity<Object> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
```

**Rules:**
- `GlobalExceptionHandler` (`@RestControllerAdvice`) is the **only** place that maps exceptions to HTTP responses.
- Never use `try/catch` in controllers or services to build `ResponseEntity` error responses.
- Always throw a typed domain exception from services — never throw `RuntimeException` directly.
- Never throw or declare checked exceptions from service methods.
- Never catch and swallow exceptions — always rethrow or convert to a typed exception.
- The `DataIntegrityViolationException` handler is a safety net (e.g. race conditions bypassing pre-checks) — return a generic message, never expose internal DB details.
- Uniform error envelope in every error response: `{ timestamp, status, error, message }`.

---

## 11. Transaction Rules

- Annotate the service class with `@Transactional` — this is the write default for all methods.
- Override with `@Transactional(readOnly = true)` on every method that only reads data. This enables DB-level optimizations and signals intent clearly.
- Set `spring.jpa.open-in-view: false` in `application.yml`. Never rely on the Open Session in View pattern — all lazy-loading must complete inside the service transaction.
- Hibernate dirty tracking: within a `@Transactional` write method, changes to managed entities are automatically flushed at commit. No explicit `save()` call is needed after mutating a managed entity. **Document this** in your team conventions so it is not invisible to readers.
- Never call a mutating operation inside a `@Transactional(readOnly = true)` method.
- Cross-service calls: if `ServiceA` calls `ServiceB`, both must be transactional. The default Spring propagation (`REQUIRED`) means they share the same transaction — this is correct behavior.

```yaml
# application.yml
spring:
  jpa:
    open-in-view: false
```

---

## 12. Database & Flyway

- Schema is **fully owned by Flyway**. Never use `ddl-auto: create`, `update`, or `create-drop` in any non-test profile.
- Production and staging: `ddl-auto: validate` — Hibernate validates the schema against entities but does not modify it.
- Tests only: `ddl-auto: create-drop` with Flyway disabled.
- All primary keys are UUIDs. Use `@GeneratedValue` with the PostgreSQL `gen_random_uuid()` default.
- Unique constraints: pick **one** convention and stick to it. Prefer named `@UniqueConstraint` on `@Table` (managed by Flyway) over `unique = true` on `@Column`. Do not use both — it creates redundant DB constraints.
- Index all foreign key columns — unindexed FK columns cause full table scans on joins.
- All FK columns should be `nullable = false` unless the relationship is explicitly optional by design.
- Store enum values as strings (`@Enumerated(EnumType.STRING)`) in the DB.
- Never write raw SQL in services or controllers — use JPQL or derived query methods.

```java
// Good — named constraint, Flyway-managed
@Table(name = "orders", uniqueConstraints = {
    @UniqueConstraint(name = "uq_orders_external_order_number", columnNames = "external_order_number")
})

// Avoid — anonymous, redundant if @UniqueConstraint is already present
@Column(unique = true)
private String externalOrderNumber;
```

---

## 13. Testing Strategy

| Test type | Tooling | Scope |
|---|---|---|
| Integration tests | `@SpringBootTest` + Testcontainers | Full HTTP stack against a real DB in a container |
| Controller slice tests | `@WebMvcTest` + `@MockBean` | HTTP binding, validation, status codes — no real DB |
| Service unit tests | `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` | Business logic in isolation |

### Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
}
```

- All integration tests extend a single `AbstractIntegrationTest` base class.
- Use `@ServiceConnection` — no manual datasource configuration needed in the test profile.
- Test profile (`application-test.yml`): Flyway disabled, `ddl-auto: create-drop`.
- Never connect to a real external database in tests.

### Controller Slice Tests

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean OrderService orderService;

    @Test
    void createOrder_returns201WithLocation() throws Exception {
        // ...
    }
}
```

- Test HTTP status codes, `Location` headers, and validation rejection (400).
- Assert on the error envelope structure: `timestamp`, `status`, `error`, `message`.
- Do not test business logic here — mock the service.

### Service Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks OrderService orderService;
    @Mock OrderRepository orderRepository;

    @Test
    void createOrder_throwsConflict_whenExternalNumberExists() {
        when(orderRepository.existsByExternalOrderNumber("ORD-1")).thenReturn(true);
        assertThatThrownBy(() -> orderService.createOrder(...))
            .isInstanceOf(ConflictException.class);
    }
}
```

- Test all business rule branches: happy path, each exception case, each state guard.
- Use `AssertJ` (`assertThatThrownBy`, `assertThat`) — not `assertThrows` from JUnit 5.
- Do not load a Spring context in unit tests — `@ExtendWith(MockitoExtension.class)` only.
