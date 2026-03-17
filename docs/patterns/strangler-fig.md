# Strangler Fig

> **Leite Traffic über ein Spring-Boot-Gateway. Das Legacy-System bedient zunächst alles.
> Neue Funktionalität wird Route für Route eingebunden, bis das Legacy-System ausgehungert
> und gelöscht ist.**

---

## Das Problem

Dein Legacy-Monolith bearbeitet den gesamten HTTP-Traffic. Er funktioniert — aber er ist schwer
zu ändern, langsam zu testen und unabhängig von unabhängigen Features nicht deploybar.
Du musst ihn ersetzen, aber ein Big-Bang-Rewrite ist zu riskant.

Der Strangler Fig installiert am ersten Tag einen Proxy vor dem Legacy-System.
Der gesamte Traffic läuft unverändert durch. Dann migrierst du eine Route nach der anderen:
implementiere die Route im neuen Service, schalte den Proxy um, verifiziere, wiederhole.
Wenn keine Routen mehr übrig sind, löschst du das Legacy.

---

## Wann verwenden?

- Das Legacy-System ist per HTTP erreichbar (REST, SOAP, alter Spring-MVC-Controller)
- Du kannst einen Proxy oder ein Gateway davor schalten
- Du möchtest eine inkrementelle, umkehrbare Migration mit echtem Produktionstraffic
  als Validierungsmechanismus

**Nicht verwenden**, wenn die Legacy-Komponente direkt im Prozess aufgerufen wird
(shared Library, direkte Klasseninstanziierung). Verwende stattdessen
[Branch-by-Abstraction](branch-by-abstraction.md).

---

## Wie es funktioniert

```
Phase 1 — Proxy installieren (keine Verhaltensänderung):

  Client → Gateway ──────────────────→ Legacy-Monolith

Phase 2 — Erste Route migrieren:

  Client → Gateway → /orders (neu) → Neuer Spring-Boot-Service (hexagonal)
                   → alles andere  → Legacy-Monolith

Phase 3 — Alle Routen migrieren:

  Client → Gateway → /orders       → Neuer Service
                   → /products     → Neuer Service
                   → /legacy-ding  → Neuer Service

Phase 4 — Legacy löschen:

  Client → Gateway → Neuer Service (alle Routen)

Phase 5 — Gateway entfernen (optional):

  Client → Neuer Service
```

Das Legacy-System wird „erdrosselt" — wie die Würgefeige (Strangler Fig),
die einen Wirtsbaum umwächst, bis der Wirt verschwunden ist.

---

## Beispiel: Spring-Boot-Gateway

Das ausführbare Beispiel befindet sich in
[`examples/strangler-fig/`](https://github.com/marvinrichter/jvm-modernisation-playbook/tree/main/examples/strangler-fig).

### Projektstruktur

```
strangler-fig/
├── src/main/java/de/marvinrichter/stranglerfig/
│   ├── StranglerFigApplication.java
│   ├── legacy/
│   │   └── LegacyOrderController.java        # Alt: prozedural, keine Ports
│   └── newservice/
│       ├── adapter/in/web/OrderController.java   # Neu: hexagonaler Inbound-Adapter
│       ├── application/OrderUseCase.java          # Neu: Port-Schnittstelle
│       ├── application/CreateOrderCommand.java
│       └── domain/Order.java                      # Neu: reines Domänenobjekt
```

### Schritt 1 — Der Legacy-Controller (was wir ersetzen)

```java title="legacy/LegacyOrderController.java"
@RestController
@RequestMapping("/api/orders")
@ConditionalOnProperty(name = "feature.new-order-service.enabled",
        havingValue = "false", matchIfMissing = true) // (1) Standard: Legacy aktiv
public class LegacyOrderController {

    private final OrderJpaRepository orderJpaRepository; // (2) direkte JPA-Abhängigkeit

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody Map<String, Object> body) {

        // (3) Geschäftslogik und Persistenz gemischt
        var entity = new OrderEntity(
                (String) body.get("customerId"),
                new BigDecimal(body.get("totalAmount").toString()));
        orderJpaRepository.save(entity);

        return ResponseEntity.ok(Map.of(
                "orderId", entity.getId(),
                "status",  entity.getStatus(),
                "source",  "legacy"));
    }
}
```

1. Per `@ConditionalOnProperty` aktiv, wenn Feature-Flag auf `false` steht.
2. Keine Port-Abstraktion — direkter JPA-Zugriff aus dem Controller.
3. Geschäftsregel (`status = PENDING`) in der Infrastrukturschicht vergraben.

### Schritt 2 — Die neue hexagonale Implementierung

```java title="newservice/domain/Order.java"
public record Order(UUID id, String customerId, BigDecimal totalAmount, OrderStatus status) {
    public static Order create(String customerId, BigDecimal totalAmount) {
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Betrag muss positiv sein");
        }
        return new Order(UUID.randomUUID(), customerId, totalAmount, OrderStatus.PENDING);
    }
}
```

```java title="newservice/adapter/in/web/NewOrderController.java"
@RestController
@RequestMapping("/api/orders")
@ConditionalOnProperty(name = "feature.new-order-service.enabled", havingValue = "true")
public class NewOrderController {

    private final OrderUseCase orderUseCase; // (1) hängt vom Port ab, nicht von JPA

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        var order = orderUseCase.createOrder(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }
}
```

1. Der Controller kennt keine JPA-Details — nur den `OrderUseCase`-Port.

### Schritt 3 — Das Feature-Flag umschalten

```properties title="application.properties"
# false (Standard) = Legacy-Controller aktiv
# true             = Neuer hexagonaler Controller aktiv
feature.new-order-service.enabled=false
```

Setze den Wert auf `true` in Staging, führe deine Tests aus, überprüfe, deploy in Produktion.
Der URL-Vertrag (`/api/orders`) bleibt unverändert — Clients bemerken den Wechsel nicht.
Spring registriert immer nur einen der beiden Controller als Bean.

---

## Migrations-Checkliste

- [ ] Gateway vor dem Legacy-System installieren (gesamter Traffic läuft unverändert durch)
- [ ] Verifizieren: Produktionstraffic unverändert, Latenz akzeptabel
- [ ] Erste Route im neuen Service implementieren (mit Tests)
- [ ] Feature-Flag in Staging aktivieren — Smoke-Tests ausführen
- [ ] In Produktion aktivieren — Fehlerrate und Latenz 48 Stunden beobachten
- [ ] Wenn stabil: Legacy-Route als deprecated markieren
- [ ] Für jede weitere Route wiederholen
- [ ] Wenn alle Routen migriert: Legacy-Code löschen

---

## Häufige Fehler

**Legacy-Datenformate vergessen.** Der neue Service benötigt möglicherweise einen
[Anti-Corruption Layer](anti-corruption-layer.md), um Legacy-Request-/Response-Formate
zu übersetzen. Lass nicht zu, dass das Legacy-API-Format in dein neues Domänenmodell einsickert.

**Beide Implementierungen gegen dieselbe Datenbank betreiben.** Während der Migration
könnten Legacy und neuer Service dasselbe Schema verwenden. Nutze separate Schemas oder
ein Read-Model, um Kopplung zu vermeiden.

**Den Proxy dauerhaft bestehen lassen.** Der Proxy erhöht die Latenz und den Wartungsaufwand.
Leite Clients nach Abschluss der Migration direkt zum neuen Service.

**Zu viele Routen auf einmal migrieren.** Migriere eine Route nach der anderen.
Der Sinn des Strangler Fig ist inkrementelle Risikoreduktion — untergrabe das nicht,
indem du alles auf einmal umschaltest.

---

## Weiterführende Links

- [Martin Fowler: Strangler Fig Application](https://martinfowler.com/bliki/StranglerFigApplication.html)
- [spring-hexagonal-archetype](https://github.com/marvinrichter/spring-hexagonal-archetype) — der Zielzustand, auf den diese Migration hinführt
