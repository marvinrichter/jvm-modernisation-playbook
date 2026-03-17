# Branch-by-Abstraction

> **Extrahiere die zu ersetzende Komponente hinter eine Port-Schnittstelle. Lege alte und neue
> Implementierung dahinter. Schalte per `@ConditionalOnProperty` um. Lösche die Legacy-Implementierung,
> sobald das Vertrauen groß genug ist.**

---

## Das Problem

Ein Spring-`@Service` ist zu einer 1.500-Zeilen-Klasse angewachsen, mit fest verdrahteten
JPA-Queries, gemischten Geschäftsregeln und keinen Tests. Jeder andere Service hängt direkt
davon ab. Du kannst ihn in einem Branch nicht neu schreiben, ohne einen monatelangen
Merge-Alptraum zu riskieren. Du musst ihn ersetzen, während der Produktionstraffic weiterläuft.

---

## Wann verwenden?

- Die Komponente wird direkt im Prozess aufgerufen (nicht per HTTP) — ein Spring-Bean,
  eine Utility-Klasse, eine Library-Abhängigkeit
- Du kannst eine klare Schnittstelle für das definieren, was die Komponente tut
- Du möchtest alte und neue Implementierung parallel betreiben, um Sicherheit zu gewinnen

**Verwende stattdessen [Strangler Fig](strangler-fig.md)**, wenn die Komponente per HTTP
erreichbar ist und auf Netzwerkebene dahinter ein Proxy gesetzt werden kann.

---

## Wie es funktioniert

```
Schritt 1 — Interface aus der Legacy-Klasse extrahieren:

  OrderService (konkrete Klasse, 1500 Zeilen)
  ↓
  OrderPort (Interface)
  LegacyOrderService implements OrderPort   ← unveränderter Legacy-Code hinter Interface

Schritt 2 — Neue Version implementieren:

  NewOrderService implements OrderPort      ← neue hexagonale Implementierung

Schritt 3 — Per Feature-Flag umschalten:

  @ConditionalOnProperty("feature.new-order-service")
  LegacyOrderService  ODER  NewOrderService

Schritt 4 — Legacy löschen:

  LegacyOrderService + Feature-Flag entfernen
```

Zu keinem Zeitpunkt ist der Produktionscode kaputt. Beide Implementierungen werden zusammen
deployed; das Flag bestimmt, welche Anfragen bearbeitet.

---

## Beispiel: Legacy-Order-Service ersetzen

Das ausführbare Beispiel befindet sich in
[`examples/branch-by-abstraction/`](https://github.com/marvinrichter/jvm-modernisation-playbook/tree/main/examples/branch-by-abstraction).

### Projektstruktur

```
branch-by-abstraction/
├── src/main/java/de/marvinrichter/bba/
│   ├── BranchByAbstractionApplication.java
│   ├── before/
│   │   └── LegacyOrderService.java           # Original: direktes JPA, kein Interface
│   └── after/
│       ├── application/
│       │   ├── OrderPort.java                # (1) Das extrahierte Port-Interface
│       │   └── CreateOrderCommand.java
│       ├── domain/
│       │   ├── Order.java
│       │   └── OrderStatus.java
│       └── adapter/
│           ├── in/web/
│           │   └── OrderController.java      # Hängt von OrderPort ab, nicht der Impl.
│           └── out/persistence/
│               └── JpaOrderAdapter.java      # (2) Neue Implementierung hinter dem Port
```

### Vorher: der Legacy-Service

```java title="before/LegacyOrderService.java"
@Service
public class LegacyOrderService {

    @Autowired
    private OrderJpaRepository orderJpaRepository; // (1) Gemischte Zuständigkeiten

    public Map<String, Object> createOrder(String customerId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) { // (2) Geschäftsregel in Infrastruktur
            throw new IllegalArgumentException("Betrag muss positiv sein");
        }
        var entity = new LegacyOrderEntity(customerId, amount);
        orderJpaRepository.save(entity);
        return Map.of(
                "orderId", entity.getId(),
                "status",  entity.getStatus()); // (3) Status als Magic String
    }
}
```

1. Geschäftslogik und JPA in einer Klasse — ohne Datenbank nicht testbar.
2. Die Geschäftsregel (Betrag > 0) gehört in die Domäne, nicht in den Service.
3. Status als Magic String — keine Typsicherheit.

### Schritt 1 — Port-Interface extrahieren

```java title="after/application/OrderPort.java"
public interface OrderPort {
    Order createOrder(CreateOrderCommand command);
}
```

```java title="after/application/CreateOrderCommand.java"
public record CreateOrderCommand(String customerId, BigDecimal totalAmount) {
    public CreateOrderCommand {
        Objects.requireNonNull(customerId, "customerId darf nicht null sein");
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("totalAmount muss positiv sein");
        }
    }
}
```

Die Geschäftsregel (Betrag > 0) lebt jetzt im Command — einmal getestet,
überall durchgesetzt.

### Schritt 2 — Legacy-Service hinter Interface kapseln

```java title="LegacyOrderAdapter (Übergangsschritt)"
@Service
@ConditionalOnProperty(name = "feature.new-order-service", havingValue = "false",
                       matchIfMissing = true) // (1) Standard: Legacy aktiv
public class LegacyOrderAdapter implements OrderPort {

    private final LegacyOrderService legacyOrderService;

    @Override
    public Order createOrder(CreateOrderCommand command) {
        // (2) Übersetzung — ACL-Muster innerhalb des Adapters
        var result = legacyOrderService.createOrder(
                command.customerId(), command.totalAmount());
        return new Order(
                (java.util.UUID) result.get("orderId"),
                command.customerId(),
                command.totalAmount(),
                OrderStatus.PENDING);
    }
}
```

1. Aktiv wenn `feature.new-order-service=false` (Standard — keine Änderung in Produktion).
2. Der Adapter übersetzt zwischen dem Legacy-Rückgabetyp (`Map`) und dem neuen `Order`-Domänenobjekt.

### Schritt 3 — Neue Version implementieren

```java title="after/adapter/out/persistence/JpaOrderAdapter.java"
@Repository
@ConditionalOnProperty(name = "feature.new-order-service", havingValue = "true")
public class JpaOrderAdapter implements OrderPort {

    private final OrderJpaRepository jpaRepository;

    @Override
    public Order createOrder(CreateOrderCommand command) {
        var order = Order.create(command.customerId(), command.totalAmount());
        jpaRepository.save(new OrderJpaEntity(order));
        return order;
    }
}
```

### Schritt 4 — Der Controller hängt nur vom Port ab

```java title="after/adapter/in/web/OrderController.java"
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderPort orderPort; // (1) hängt vom Interface ab

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        var order = orderPort.createOrder(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }
}
```

1. Der Controller ist unverändert — egal ob Legacy- oder neue Implementierung aktiv ist.

### Schritt 5 — Flag umschalten

```properties title="application.properties"
# false (Standard) = LegacyOrderAdapter aktiv
# true             = JpaOrderAdapter aktiv
feature.new-order-service=false
```

In Staging auf `true` setzen, Testsuite ausführen, überwachen, in Produktion auf `true` setzen.

### Schritt 6 — Legacy löschen

Sobald die neue Implementierung ausreichend lange stabil in Produktion läuft:

1. `LegacyOrderService` und `LegacyOrderAdapter` löschen
2. `@ConditionalOnProperty` aus `JpaOrderAdapter` entfernen (ist jetzt immer aktiv)
3. Feature-Flag aus `application.properties` entfernen

---

## Migrations-Checkliste

- [ ] Port-Interface aus der Legacy-Klasse extrahieren (keine Verhaltensänderung)
- [ ] Legacy-Klasse hinter dem Interface kapseln (immer noch keine Verhaltensänderung)
- [ ] Aufrufer so aktualisieren, dass sie vom Interface abhängen (nicht von der konkreten Klasse)
- [ ] Neue Version hinter demselben Interface implementieren
- [ ] Mit `@ConditionalOnProperty` absichern — Standard: Legacy aktiv
- [ ] Beide Implementierungen in Integrationstests ausführen
- [ ] Neue Implementierung in Staging aktivieren — vollständige Testsuite ausführen
- [ ] In Produktion aktivieren — 48–72 Stunden überwachen
- [ ] Wenn stabil: Legacy-Implementierung und Feature-Flag entfernen

---

## Häufige Fehler

**Ein Interface extrahieren, das die Implementierung widerspiegelt, nicht die Absicht.**
Ein Interface mit `saveToDatabase(Order order)` ist kein Port — es ist eine undichte
Abstraktion, die Aufrufer an den Speichermechanismus koppelt. Verwende `saveOrder(Order)`.

**Beide Implementierungen dauerhaft aktiv lassen.**
Das Feature-Flag ist ein Migrationswerkzeug, keine dauerhafte Konfigurationsoption.
Setze dir einen Kalendertermin: Wenn das Flag nicht innerhalb von 4 Wochen umgeschaltet
wird, untersuche warum.

**Den Legacy-Wrapper nicht testen.**
Der Übersetzungscode in `LegacyOrderAdapter` ist der Ort, an dem Bugs versteckt sind.
Schreibe explizite Unit-Tests dafür — besonders für Datentypen, die zwischen Legacy-
und neuem Modell unterschiedlich sind.

---

## Weiterführende Links

- [Martin Fowler: Branch By Abstraction](https://martinfowler.com/bliki/BranchByAbstraction.html)
- [spring-hexagonal-archetype](https://github.com/marvinrichter/spring-hexagonal-archetype) — der Zielzustand, auf den diese Migration hinführt
