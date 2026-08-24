# Code patterns: good and bad

> **The other three pages show how to reshape a system. This one shows what the
> single class looks like afterwards. Six pairs, each with the smell, the fix,
> and the line that carries the difference.**

The examples use the same fictional Order/Customer domain as the runnable
patterns. No real client code. Unlike the three process patterns, this page has
no Maven module of its own: these are focused snippets, not a running service.

---

## 1. The seam is an interface, not an `if`

When switching between old and new, the feature flag likes to land in the middle
of the business logic. Then the branch sits at every call site instead of once at
the edge.

```java title="bad"
public Order createOrder(CreateOrderCommand command) {
    if (featureFlags.isEnabled("new-order-service")) {   // (1)
        return newOrderService.create(command);
    }
    return legacyOrderService.create(command);
}
```

1. The same branch repeats in every method that knows both paths.

```java title="good"
public interface OrderPort {
    Order createOrder(CreateOrderCommand command);
}

// The choice is made once, at wiring time, not in the business logic.
@ConditionalOnProperty(name = "feature.new-order-service", havingValue = "true")
public class NewOrderAdapter implements OrderPort { /* ... */ }
```

**Why:** The flag is a wiring detail. In the business logic it is a second code
path that every test has to cover twice.

---

## 2. The legacy model stays behind the Anti-Corruption Layer

The fastest way to poison the new core is to pass the old return type through. A
`Map` or a legacy DTO inside the new service drags the old model into every
signature.

```java title="bad"
public Map<String, Object> loadOrder(UUID id) {   // (1)
    return legacyOrderService.find(id);
}
```

1. The caller now knows the keys of the legacy `Map`, not the domain.

```java title="good"
public Order loadOrder(OrderId id) {
    var legacy = legacyOrderService.find(id.value());
    return orderTranslator.toDomain(legacy);   // (1)
}
```

1. Translated at the boundary. Past this line the legacy `Map` no longer exists.

**Why:** A passed-through foreign type makes the boundary invisible. That
visibility is the whole point of the
[Anti-Corruption Layer](anti-corruption-layer.md).

---

## 3. Constructor injection, not field injection

Field injection reads shorter and costs you testability. The class can no longer
be built without Spring.

```java title="bad"
@Service
public class OrderService {
    @Autowired private OrderPort orderPort;   // (1)
    @Autowired private CustomerPort customerPort;
}
```

1. No way to create an `OrderService` in a test without a Spring context.

```java title="good"
@Service
public class OrderService {
    private final OrderPort orderPort;
    private final CustomerPort customerPort;

    public OrderService(OrderPort orderPort, CustomerPort customerPort) {
        this.orderPort = orderPort;
        this.customerPort = customerPort;
    }
}
```

**Why:** The constructor makes the dependencies visible and the fields `final`. A
unit test calls `new OrderService(fake, fake)` and needs no framework.

---

## 4. Time and randomness are dependencies, not static calls

A `LocalDate.now()` in the middle of the business logic ties the behaviour to the
calendar day. The test that checks the month boundary passes on 30 of 31 days and
fails once.

```java title="bad"
public boolean isOverdue(Order order) {
    return order.dueDate().isBefore(LocalDate.now());   // (1)
}
```

1. Not deterministic. The result depends on the day the test runs.

```java title="good"
public boolean isOverdue(Order order, Clock clock) {
    return order.dueDate().isBefore(LocalDate.now(clock));   // (1)
}
```

1. The `Clock` comes from outside. The test pins a fixed day and is reproducible.

**Why:** Time and randomness are inputs. As a static call they are hidden inputs,
and a hidden input is a flaky test.

---

## 5. No empty catch: fail loud or translate

A swallowed error looks like a correct empty result. The caller cannot tell the
two apart, and the bug surfaces far away.

```java title="bad"
public Optional<Order> findOrder(OrderId id) {
    try {
        return Optional.of(orderPort.load(id));
    } catch (Exception e) {
        return Optional.empty();   // (1)
    }
}
```

1. "Not found" and "the database is gone" produce the same empty `Optional`.

```java title="good"
public Optional<Order> findOrder(OrderId id) {
    try {
        return Optional.of(orderPort.load(id));
    } catch (OrderNotFoundException e) {
        return Optional.empty();               // (1)
    } catch (PersistenceException e) {
        throw new OrderLookupFailed(id, e);    // (2)
    }
}
```

1. Only the expected absence becomes an empty result.
2. The unexpected failure stays a failure, with context.

**Why:** A `catch (Exception)` that returns an empty result is a silent
degradation. Catch what you expect and let the rest be loud.

---

## 6. The business rule in the domain, not the controller

Validation in the controller covers only the one HTTP path. When an order arrives
over Kafka or a batch, it does not apply.

```java title="bad"
@PostMapping("/api/orders")
public ResponseEntity<?> create(@RequestBody CreateOrderRequest request) {
    if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {   // (1)
        return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(orderPort.createOrder(request.toCommand()));
}
```

1. The rule "amount > 0" lives in the web adapter. Every other entry point bypasses it.

```java title="good"
public record CreateOrderCommand(CustomerId customerId, BigDecimal totalAmount) {
    public CreateOrderCommand {
        Objects.requireNonNull(customerId, "customerId must not be null");
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {    // (1)
            throw new IllegalArgumentException("totalAmount must be positive");
        }
    }
}
```

1. The rule sits in the command. Every entry point that builds a command goes through it.

**Why:** The controller translates HTTP into a command, nothing more. The rule
belongs where every path passes.

---

## Enforce it in the build

Four of the six patterns can be written as a rule that breaks the build instead
of hoping a review catches it. That is the point of
[ArchUnit](https://www.archunit.org/): an architecture rule that is a test.

```java title="ArchitectureTest.java"
@Test
void controller_depends_on_the_port_not_the_adapter() {
    noClasses().that().resideInAPackage("..adapter.in..")
        .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
        .check(importedClasses);
}

@Test
void no_field_injection() {
    noFields().should().beAnnotatedWith(Autowired.class)   // (1)
        .check(importedClasses);
}

@Test
void no_wall_clock_in_the_domain() {
    noClasses().that().resideInAPackage("..domain..")
        .should().callMethod(LocalDate.class, "now")       // (2)
        .check(importedClasses);
}
```

1. Enforces pattern 3: field injection breaks the build.
2. Enforces pattern 4: `LocalDate.now()` in the domain breaks the build.

A rule in the build is more expensive to write than a comment in a review, and
cheaper the moment the second developer breaks it without knowing.

---

## Further reading

- [Branch-by-Abstraction](branch-by-abstraction.md), the process pattern behind pattern 1
- [Anti-Corruption Layer](anti-corruption-layer.md), the process pattern behind pattern 2
- [spring-hexagonal-archetype](https://github.com/marvinrichter/spring-hexagonal-archetype), the target state with ArchUnit rules in the scaffold
