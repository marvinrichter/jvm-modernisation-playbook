# Branch-by-Abstraction

> **Extract the component you want to replace behind a port interface. Put both old and
> new implementations behind it. Toggle via `@ConditionalOnProperty`. Delete the
> legacy once confidence is high.**

---

## The problem

A Spring `@Service` has grown into a 1,500-line class with hardcoded JPA queries,
mixed business rules, and no tests. Every other service depends on it directly.
You can't rewrite it in a branch without a months-long merge nightmare.
You need to replace it while production traffic keeps flowing.

---

## When to use it

- The component is called directly (not via HTTP) — a Spring bean, a utility class,
  a library dependency
- You can define a clear interface for what the component does
- You want to run old and new implementations in parallel for safety

**Use [Strangler Fig](strangler-fig.md) instead** if the component is accessed
via HTTP and can be proxied at the network level.

---

## How it works

```
Step 1 — Extract an interface from the legacy class:

  OrderService (concrete class, 1500 lines)
  ↓
  OrderPort (interface)
  LegacyOrderService implements OrderPort   ← unchanged legacy code behind interface

Step 2 — Implement the new version:

  NewOrderService implements OrderPort      ← new hexagonal implementation

Step 3 — Toggle via feature flag:

  @ConditionalOnProperty("feature.new-order-service")
  LegacyOrderService  OR  NewOrderService

Step 4 — Delete legacy:

  Remove LegacyOrderService + feature flag
  Rename OrderPort → OrderService (or keep as a port — your call)
```

At no point is production code broken. Both implementations are deployed together;
the flag controls which one handles requests.

---

## Example: Replacing a legacy order service

The runnable example is in
[`examples/branch-by-abstraction/`](https://github.com/marvinrichter/jvm-modernisation-playbook/tree/main/examples/branch-by-abstraction).

### Project structure

```
branch-by-abstraction/
├── src/main/java/de/marvinrichter/bba/
│   ├── BranchByAbstractionApplication.java
│   ├── before/
│   │   └── LegacyOrderService.java           # Original: direct JPA, no interface
│   └── after/
│       ├── application/
│       │   ├── OrderPort.java                # (1) The extracted port interface
│       │   └── CreateOrderCommand.java
│       ├── domain/
│       │   ├── Order.java
│       │   └── OrderStatus.java
│       └── adapter/
│           ├── in/web/
│           │   └── OrderController.java      # Depends on OrderPort, not the impl
│           └── out/persistence/
│               ├── JpaOrderAdapter.java      # (2) New implementation behind the port
│               └── OrderJpaEntity.java
```

### Before: the legacy service

```java title="before/LegacyOrderService.java"
@Service
public class LegacyOrderService {

    // (1) Mixed concerns: business logic + persistence
    @Autowired
    private OrderJpaRepository orderJpaRepository;

    public Map<String, Object> createOrder(String customerId, BigDecimal amount) {
        // (2) Business rule buried in infrastructure layer
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        var entity = new OrderEntity();
        entity.setCustomerId(customerId);
        entity.setAmount(amount);
        entity.setStatus("PENDING"); // (3) Status as magic string
        entity.setCreatedAt(LocalDateTime.now());
        orderJpaRepository.save(entity);

        return Map.of(
            "orderId", entity.getId(),
            "status",  entity.getStatus()
        );
    }
}
```

1. Business logic and JPA are in the same class — impossible to test without a database.
2. The business rule (amount > 0) belongs in the domain, not the service layer.
3. Status as a magic string — no type safety.

### Step 1 — Extract the port interface

```java title="after/application/OrderPort.java"
public interface OrderPort {
    Order createOrder(CreateOrderCommand command);
}
```

```java title="after/application/CreateOrderCommand.java"
public record CreateOrderCommand(String customerId, BigDecimal totalAmount) {
    public CreateOrderCommand {
        Objects.requireNonNull(customerId, "customerId must not be null");
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("totalAmount must be positive");
        }
    }
}
```

The business rule (amount > 0) now lives in the command — tested once,
enforced everywhere.

### Step 2 — Wrap the legacy service behind the interface

```java title="Wrapping LegacyOrderService (transitional)"
@Service
@ConditionalOnProperty(name = "feature.new-order-service", havingValue = "false",
                       matchIfMissing = true) // (1) Default: legacy
public class LegacyOrderAdapter implements OrderPort {

    private final LegacyOrderService legacyOrderService;

    public LegacyOrderAdapter(LegacyOrderService legacyOrderService) {
        this.legacyOrderService = legacyOrderService;
    }

    @Override
    public Order createOrder(CreateOrderCommand command) {
        // (2) Translate — ACL pattern inside the adapter
        var result = legacyOrderService.createOrder(
            command.customerId(), command.totalAmount());
        return new Order(
            UUID.fromString(result.get("orderId").toString()),
            command.customerId(),
            command.totalAmount(),
            OrderStatus.PENDING
        );
    }
}
```

1. Active when `feature.new-order-service=false` (the default — no change in production).
2. The adapter translates between the legacy return type (`Map`) and the new `Order` domain object.

### Step 3 — Implement the new version

```java title="after/adapter/out/persistence/JpaOrderAdapter.java"
@Repository
@ConditionalOnProperty(name = "feature.new-order-service", havingValue = "true")
public class JpaOrderAdapter implements OrderPort {

    private final OrderJpaRepository orderJpaRepository;

    public JpaOrderAdapter(OrderJpaRepository orderJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
    }

    @Override
    public Order createOrder(CreateOrderCommand command) {
        var entity = new OrderJpaEntity(
            UUID.randomUUID(),
            command.customerId(),
            command.totalAmount(),
            OrderStatus.PENDING
        );
        orderJpaRepository.save(entity);
        return entity.toDomain();
    }
}
```

### Step 4 — The controller depends only on the port

```java title="after/adapter/in/web/OrderController.java"
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderPort orderPort; // (1) depends on the interface

    public OrderController(OrderPort orderPort) {
        this.orderPort = orderPort;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request) {
        var order = orderPort.createOrder(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }
}
```

1. The controller is unchanged whether the legacy or new implementation is active.

### Step 5 — Flip the flag

```properties title="application.properties"
# Set to true to activate the new JPA adapter (default: false = legacy)
feature.new-order-service=false
```

Flip to `true` in staging, run your test suite, monitor, flip to `true` in production.

### Step 6 — Delete the legacy

Once the new implementation has been running in production for a sufficient period:

1. Delete `LegacyOrderService` and `LegacyOrderAdapter`
2. Remove the `@ConditionalOnProperty` from `JpaOrderAdapter` (it's now always active)
3. Rename `JpaOrderAdapter` to `OrderRepository` or keep the port pattern — your call
4. Delete the feature flag from `application.properties`

---

## Migration checklist

- [ ] Extract a port interface from the legacy class (no behaviour change)
- [ ] Wrap the legacy class behind the interface (still no behaviour change)
- [ ] Update callers to depend on the interface (not the concrete class)
- [ ] Implement the new version behind the same interface
- [ ] Gate with `@ConditionalOnProperty` — default: legacy active
- [ ] Run both implementations in integration tests (test each explicitly)
- [ ] Enable new implementation in staging — run full test suite
- [ ] Enable in production — monitor for 48–72 hours
- [ ] If stable: remove legacy implementation and feature flag

---

## Common pitfalls

**Extracting an interface that mirrors the implementation, not the intent.**
An interface with `saveToDatabase(Order order)` is not a port — it's a leaky
abstraction that couples callers to the storage mechanism. Use `saveOrder(Order)`.

**Leaving both implementations active indefinitely.**
The feature flag is a migration tool, not a permanent configuration option.
Set a calendar reminder: if the flag isn't flipped within 4 weeks, investigate why.

**Not testing the legacy wrapper.**
The `LegacyOrderAdapter` translation code is where bugs hide. Write explicit
unit tests for it — especially for data types that differ between legacy and new models.

---

## Further reading

- [Martin Fowler: Branch By Abstraction](https://martinfowler.com/bliki/BranchByAbstraction.html)
- [spring-hexagonal-archetype](https://github.com/marvinrichter/spring-hexagonal-archetype) — the target state this migration lands on
