# Strangler Fig

> **Route traffic through a Spring Boot gateway. The legacy system handles everything
> at first. New capability is wired in path-by-path until the legacy is starved out
> and deleted.**

---

## The problem

Your legacy monolith handles all HTTP traffic. It works — but it's hard to change,
slow to test, and impossible to deploy independently of unrelated features.
You need to replace it, but a big-bang rewrite is too risky.

The Strangler Fig installs a proxy in front of the legacy system on day one.
All traffic passes through it unchanged. Then you migrate one route at a time:
implement the route in the new service, flip the proxy, verify, repeat.
When no routes remain, you delete the legacy.

---

## When to use it

- The legacy system is accessible via HTTP (REST, SOAP, old Spring MVC controllers)
- You can install a proxy or gateway in front of it
- You want incremental, reversible migration with production traffic as the
  validation mechanism

**Do not use it** if the legacy component is called in-process (shared library,
direct class instantiation). Use [Branch-by-Abstraction](branch-by-abstraction.md) instead.

---

## How it works

```
Phase 1 — Install the proxy (zero behaviour change):

  Client → Gateway ──────────────────→ Legacy monolith

Phase 2 — Migrate one route:

  Client → Gateway → /orders (new) → New Spring Boot service (hexagonal)
                   → everything else → Legacy monolith

Phase 3 — Migrate all routes:

  Client → Gateway → /orders       → New service
                   → /products     → New service
                   → /legacy-thing → New service

Phase 4 — Delete legacy:

  Client → Gateway → New service (all routes)

Phase 5 — Remove the gateway (optional):

  Client → New service
```

The legacy system is "strangled" — like the strangler fig vine that grows around
a host tree until the host is gone.

---

## Example: Spring Boot gateway

The runnable example is in
[`examples/strangler-fig/`](https://github.com/marvinrichter/jvm-modernisation-playbook/tree/main/examples/strangler-fig).

### Project structure

```
strangler-fig/
├── src/main/java/de/marvinrichter/stranglerfig/
│   ├── StranglerFigApplication.java          # Gateway application
│   ├── gateway/
│   │   └── RoutingConfig.java                # Route configuration — flip here
│   ├── legacy/
│   │   └── LegacyOrderController.java        # Old: procedural, no ports
│   └── newservice/
│       ├── adapter/in/web/OrderController.java  # New: hexagonal inbound adapter
│       ├── application/OrderUseCase.java        # New: port interface
│       ├── application/CreateOrderCommand.java
│       └── domain/Order.java                   # New: pure domain object
```

### Step 1 — The legacy controller (what you're replacing)

```java title="legacy/LegacyOrderController.java"
@RestController
@RequestMapping("/api/orders")
public class LegacyOrderController {

    // (1) Direct JPA — no ports, no abstraction
    @Autowired
    private EntityManager em;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody Map<String, Object> body) {

        // (2) Business logic mixed with persistence — classic legacy smell
        var order = new OrderEntity();
        order.setCustomerId((String) body.get("customerId"));
        order.setTotalAmount(new BigDecimal(body.get("totalAmount").toString()));
        order.setStatus("PENDING");
        em.persist(order);

        return ResponseEntity.ok(Map.of("orderId", order.getId(), "status", "PENDING"));
    }
}
```

1. No port interface — controller wires directly to JPA.
2. Business rule ("status = PENDING") buried in the controller layer.

### Step 2 — The new hexagonal implementation

```java title="newservice/domain/Order.java"
public record Order(
        UUID id,
        String customerId,
        BigDecimal totalAmount,
        OrderStatus status
) {
    public static Order create(String customerId, BigDecimal totalAmount) {
        return new Order(UUID.randomUUID(), customerId, totalAmount, OrderStatus.PENDING);
    }
}
```

```java title="newservice/application/OrderUseCase.java"
public interface OrderUseCase {
    Order createOrder(CreateOrderCommand command);
}
```

```java title="newservice/adapter/in/web/OrderController.java"
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderUseCase orderUseCase; // (1) depends on port, not implementation

    public OrderController(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request) {
        var order = orderUseCase.createOrder(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }
}
```

1. Controller depends on the `OrderUseCase` port — not on JPA.

### Step 3 — The routing configuration

```java title="gateway/RoutingConfig.java"
@Configuration
public class RoutingConfig {

    // (1) Feature flag — flip this to migrate route-by-route
    @Value("${feature.new-order-service.enabled:false}")
    private boolean newOrderServiceEnabled;

    @Bean
    public RouterFunction<ServerResponse> orderRoutes(
            OrderController newController,
            LegacyOrderController legacyController) {

        if (newOrderServiceEnabled) {
            // (2) Route to new hexagonal service
            return route(POST("/api/orders"), newController::createOrder);
        } else {
            // (3) Keep routing to legacy
            return route(POST("/api/orders"), legacyController::createOrder);
        }
    }
}
```

1. A single boolean controls which implementation handles the route.
2. When `true`, traffic goes to the new hexagonal controller.
3. When `false` (default), legacy continues unchanged.

### Step 4 — Flip the flag

```properties title="application.properties"
# Flip to true when you're ready to migrate /api/orders to the new service
feature.new-order-service.enabled=false
```

In production, manage this via Spring Cloud Config, environment variables,
or a feature flag service (LaunchDarkly, Unleash). The code path is identical.

---

## Migration checklist

- [ ] Install the gateway in front of the legacy system (all traffic passes through unchanged)
- [ ] Verify: production traffic is unaffected, latency is acceptable
- [ ] Implement the first route in the new service (with tests)
- [ ] Enable the feature flag in staging — run smoke tests
- [ ] Enable in production — monitor error rate and latency for 48 hours
- [ ] If stable: mark the legacy route as deprecated
- [ ] Repeat for each route
- [ ] When all routes are migrated: delete the legacy code
- [ ] When the gateway is trivial (one route): consider removing it

---

## Common pitfalls

**Forgetting to handle legacy data formats.** The new service may need an
[Anti-Corruption Layer](anti-corruption-layer.md) to translate legacy request/response
shapes. Don't let the legacy API shape leak into your new domain model.

**Running both implementations against the same database.** During migration, the
legacy and new service may share a schema. Use separate schemas or a read model
to prevent coupling.

**Leaving the proxy in place permanently.** The proxy adds latency and a maintenance
burden. Once migration is complete, route clients directly to the new service.

**Migrating too many routes at once.** Migrate one route at a time. The point of the
Strangler Fig is incremental risk reduction — don't defeat it by flipping everything
at once.

---

## Further reading

- [Martin Fowler: Strangler Fig Application](https://martinfowler.com/bliki/StranglerFigApplication.html)
- [spring-hexagonal-archetype](https://github.com/marvinrichter/spring-hexagonal-archetype) — the target state this migration lands on
