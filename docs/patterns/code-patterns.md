# Code-Muster: gut und schlecht

> **Die drei anderen Seiten zeigen, wie man ein System umbaut. Diese zeigt, wie
> die einzelne Klasse danach aussieht. Sechs Paare, jeweils die schlechte
> Fassung, die Korrektur und die Zeile, die den Unterschied trägt.**

Die Beispiele nutzen dieselbe fiktive Order/Customer-Domäne wie die
ausführbaren Muster. Kein echter Kundencode. Anders als die drei Prozessmuster
hat diese Seite kein eigenes Maven-Modul: es sind fokussierte Ausschnitte, kein
lauffähiger Dienst.

---

## 1. Der Umschaltpunkt ist ein Interface, kein `if`

Beim Umschalten zwischen alt und neu landet das Feature-Flag gern mitten in der
Fachlogik. Dann steht die Verzweigung an jeder Aufrufstelle, statt einmal am
Rand.

```java title="schlecht"
public Order createOrder(CreateOrderCommand command) {
    if (featureFlags.isEnabled("new-order-service")) {   // (1)
        return newOrderService.create(command);
    }
    return legacyOrderService.create(command);
}
```

1. Dieselbe Verzweigung wiederholt sich in jeder Methode, die beide Wege kennt.

```java title="gut"
public interface OrderPort {
    Order createOrder(CreateOrderCommand command);
}

// Die Wahl fällt einmal beim Verdrahten, nicht in der Fachlogik.
@ConditionalOnProperty(name = "feature.new-order-service", havingValue = "true")
public class NewOrderAdapter implements OrderPort { /* ... */ }
```

**Warum:** Das Flag ist ein Verdrahtungsdetail. In der Fachlogik ist es ein
zweiter Codepfad, den jeder Test doppelt abdecken muss.

---

## 2. Das Legacy-Modell bleibt hinter dem Anti-Corruption Layer

Der schnellste Weg, den neuen Kern zu vergiften, ist, den alten Rückgabetyp
durchzureichen. Eine `Map` oder ein Legacy-DTO im neuen Dienst zieht das alte
Modell in jede Signatur.

```java title="schlecht"
public Map<String, Object> loadOrder(UUID id) {   // (1)
    return legacyOrderService.find(id);
}
```

1. Der Aufrufer kennt jetzt die Schlüssel der Legacy-`Map`, nicht die Domäne.

```java title="gut"
public Order loadOrder(OrderId id) {
    var legacy = legacyOrderService.find(id.value());
    return orderTranslator.toDomain(legacy);   // (1)
}
```

1. Übersetzt an der Grenze. Hinter dieser Zeile existiert die Legacy-`Map` nicht mehr.

**Warum:** Ein durchgereichter Fremdtyp macht die Grenze unsichtbar. Genau die
Sichtbarkeit ist der Zweck des [Anti-Corruption Layer](anti-corruption-layer.md).

---

## 3. Konstruktor-Injektion, keine Feld-Injektion

Feld-Injektion liest sich kürzer und kostet die Testbarkeit. Die Klasse lässt
sich ohne Spring nicht mehr bauen.

```java title="schlecht"
@Service
public class OrderService {
    @Autowired private OrderPort orderPort;   // (1)
    @Autowired private CustomerPort customerPort;
}
```

1. Lässt sich im Test nicht ohne Spring-Kontext erzeugen.

```java title="gut"
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

**Warum:** Der Konstruktor macht die Abhängigkeiten sichtbar und die Felder
`final`. Ein Unit-Test ruft `new OrderService(fake, fake)` und braucht kein
Framework.

---

## 4. Zeit und Zufall sind Abhängigkeiten, keine statischen Aufrufe

Ein `LocalDate.now()` mitten in der Fachlogik bindet das Verhalten an den
Kalendertag. Der Test, der die Monatsgrenze prüft, läuft an 30 von 31 Tagen
grün und einmal rot.

```java title="schlecht"
public boolean isOverdue(Order order) {
    return order.dueDate().isBefore(LocalDate.now());   // (1)
}
```

1. Nicht deterministisch. Das Ergebnis hängt vom Tag ab, an dem der Test läuft.

```java title="gut"
public boolean isOverdue(Order order, Clock clock) {
    return order.dueDate().isBefore(LocalDate.now(clock));   // (1)
}
```

1. Die `Clock` kommt von außen. Der Test setzt einen festen Tag und ist
   reproduzierbar.

**Warum:** Zeit und Zufall sind Eingaben. Als statischer Aufruf sind sie
versteckte Eingaben, und eine versteckte Eingabe macht den Test unzuverlässig.

---

## 5. Kein leerer catch: den Fehler nicht verschlucken

Ein geschluckter Fehler sieht aus wie ein korrektes leeres Ergebnis. Der Aufrufer
kann die beiden Fälle nicht unterscheiden, und der Bug taucht erst weit entfernt auf.

```java title="schlecht"
public Optional<Order> findOrder(OrderId id) {
    try {
        return Optional.of(orderPort.load(id));
    } catch (Exception e) {
        return Optional.empty();   // (1)
    }
}
```

1. „Nicht gefunden" und „Datenbank ist weg" ergeben dasselbe leere `Optional`.

```java title="gut"
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

1. Nur das erwartete Nicht-Vorhandensein wird zum leeren Ergebnis.
2. Der unerwartete Fehler bleibt ein Fehler, mit Kontext.

**Warum:** Ein `catch (Exception)`, der ein leeres Ergebnis zurückgibt, ist eine
stille Degradierung. Fang, was du erwartest, und reich den Rest weiter.

---

## 6. Die Geschäftsregel in der Domäne, nicht im Controller

Validierung im Controller gilt nur für den einen HTTP-Weg. Kommt eine Order
über Kafka oder einen Batch herein, greift sie nicht.

```java title="schlecht"
@PostMapping("/api/orders")
public ResponseEntity<?> create(@RequestBody CreateOrderRequest request) {
    if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {   // (1)
        return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(orderPort.createOrder(request.toCommand()));
}
```

1. Die Regel „Betrag > 0" lebt im Web-Adapter. Jeder andere Eingang umgeht sie.

```java title="gut"
public record CreateOrderCommand(CustomerId customerId, BigDecimal totalAmount) {
    public CreateOrderCommand {
        Objects.requireNonNull(customerId, "customerId darf nicht null sein");
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {    // (1)
            throw new IllegalArgumentException("totalAmount muss positiv sein");
        }
    }
}
```

1. Die Regel steht im Command. Jeder Eingang, der ein Command baut, muss durch sie.

**Warum:** Der Controller übersetzt HTTP in ein Command, mehr nicht. Die Regel
gehört dorthin, wo jeder Weg vorbeikommt.

---

## Im Build erzwingen

Vier der sechs Muster lassen sich als Regel schreiben, die den Build bricht,
statt in einem Review zu hoffen. Das ist der Punkt von
[ArchUnit](https://www.archunit.org/): eine Architekturregel, die ein Test ist.

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

1. Erzwingt Muster 3: Feld-Injektion bricht den Build.
2. Erzwingt Muster 4: `LocalDate.now()` in der Domäne bricht den Build.

Eine Regel im Build ist teurer zu schreiben als ein Kommentar im Review und
billiger, sobald der zweite Entwickler sie unwissentlich verletzt.

---

## Weiterführende Links

- [Branch-by-Abstraction](branch-by-abstraction.md), das Prozessmuster hinter Muster 1
- [Anti-Corruption Layer](anti-corruption-layer.md), das Prozessmuster hinter Muster 2
- [spring-hexagonal-archetype](https://github.com/marvinrichter/spring-hexagonal-archetype), der Zielzustand mit ArchUnit-Regeln im Gerüst
