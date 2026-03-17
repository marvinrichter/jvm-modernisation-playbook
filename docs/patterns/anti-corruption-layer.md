# Anti-Corruption Layer

> **Übersetze zwischen dem Legacy-Domänenmodell und dem neuen an der Grenze.
> Verhindert, dass Legacy-Benennung, -Typen und -Annahmen frischen Code infizieren.**

---

## Das Problem

Dein Legacy-System hat ein `Customer`-Objekt mit Feldern wie `cust_nm`, `cust_addr1`
und einem als Integer kodierten Status. Dein neuer Service hat einen `Client`
mit `name`, `address` und einem `ClientStatus`-Enum. Beide repräsentieren dasselbe
reale Konzept — aber mit unterschiedlichen Modellen.

Ohne Anti-Corruption Layer (ACL) passiert eines von zwei Dingen:
1. Du benennst alles im Legacy-System um — enormes Risiko, riesiger Diff.
2. Die Legacy-Benennung sickert in den neuen Service — der „Big Ball of Mud" bekommt einen neuen Kopf.

Der ACL schafft eine explizite Übersetzungsgrenze. Das Legacy-Modell bleibt unverändert.
Das neue Modell bleibt sauber. Der Übersetzer lebt an einem einzigen Ort und ist isoliert testbar.

---

## Wann verwenden?

- Legacy und neues System haben unterschiedliche Domänenkonzepte (unterschiedliche Namen,
  Typen, Kardinalität)
- Du rufst eine Drittanbieter-API auf, deren Modell nicht zu deiner Domäne passt
- Du integrierst ein Legacy-Datenbankschema, das du nicht besitzt und nicht ändern kannst

**Du brauchst dieses Muster fast immer in Kombination mit Strangler Fig oder Branch-by-Abstraction.**
Jene Muster schaffen die strukturelle Trennung; der ACL verhindert konzeptuelle Kontamination
über diese Grenze hinweg.

---

## Wie es funktioniert

```
Neue Domäne          ACL (Übersetzer)        Legacy-Domäne
────────────         ─────────────────       ─────────────────

Client               ← übersetzen ←          Customer
  .name              ← cust_nm   ←             .cust_nm
  .address           ← cust_addr1 ←            .cust_addr1
  .status            ← "0"→INACTIVE            .status (int)
                        "1"→ACTIVE
                        "2"→SUSPENDED
```

Der Übersetzer ist eine einfache Java-Klasse — kein Framework, keine Annotationen.
Er nimmt ein Legacy-Objekt und gibt ein neues Domänenobjekt zurück (oder umgekehrt).
Das ist alles.

---

## Beispiel: Customer → Client-Übersetzung

Das ausführbare Beispiel befindet sich in
[`examples/acl/`](https://github.com/marvinrichter/jvm-modernisation-playbook/tree/main/examples/acl).

### Projektstruktur

```
acl/
├── src/main/java/de/marvinrichter/acl/
│   ├── AclApplication.java
│   ├── legacy/
│   │   ├── Customer.java                  # Legacy-Modell (unverändert)
│   │   └── LegacyCustomerJpaRepository.java
│   ├── newdomain/
│   │   ├── Client.java                    # Neues Domänenmodell
│   │   ├── ClientStatus.java
│   │   └── ClientRepository.java          # Port
│   └── translation/
│       ├── CustomerToClientTranslator.java      # (1) Der ACL
│       └── LegacyClientRepositoryAdapter.java   # Adapter, der den Übersetzer nutzt
```

### Das Legacy-Modell (nicht modifizieren)

```java title="legacy/Customer.java"
// (1) Legacy-Objekt — gehört uns nicht, kann nicht umbenannt werden
public class Customer {
    public String custId;
    public String custNm;      // Kundenname
    public String custAddr1;   // Adresszeile 1
    public String custAddr2;   // Adresszeile 2
    public int    status;      // 0=inaktiv, 1=aktiv, 2=gesperrt
    public String createDt;    // Datum als "yyyyMMdd"-String
}
```

1. Abgekürzte Feldnamen, numerischer Status, Datum als String — klassisches Legacy.

### Das neue Domänenmodell

```java title="newdomain/Client.java"
public record Client(
        UUID id,
        String name,
        Address address,
        ClientStatus status,
        LocalDate joinedDate
) {}
```

```java title="newdomain/ClientStatus.java"
public enum ClientStatus {
    INACTIVE, ACTIVE, SUSPENDED;

    public static ClientStatus fromLegacyCode(int code) {
        return switch (code) {
            case 0 -> INACTIVE;
            case 1 -> ACTIVE;
            case 2 -> SUSPENDED;
            default -> throw new IllegalArgumentException(
                    "Unbekannter Legacy-Statuscode: " + code);
        };
    }
}
```

### Der Anti-Corruption Layer Übersetzer

```java title="translation/CustomerToClientTranslator.java"
@Component
public class CustomerToClientTranslator {

    // (1) Reine Übersetzung — kein Spring, keine Datenbank, keine Seiteneffekte
    public Client translate(Customer customer) {
        return new Client(
                UUID.fromString(customer.custId),
                customer.custNm,
                new Address(customer.custAddr1, customer.custAddr2),
                ClientStatus.fromLegacyCode(customer.status),
                parseDate(customer.createDt)     // (2) Datumsformat übersetzen
        );
    }

    public Customer reverseTranslate(Client client) {
        // (3) Rückübersetzung für Schreibzugriff auf Legacy
        var customer = new Customer();
        customer.custId    = client.id().toString();
        customer.custNm    = client.name();
        customer.custAddr1 = client.address().line1();
        customer.custAddr2 = client.address().line2();
        customer.status    = client.status().ordinal();
        customer.createDt  = client.joinedDate()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return customer;
    }

    private LocalDate parseDate(String yyyymmdd) {
        return LocalDate.parse(yyyymmdd, DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
```

1. Der Übersetzer ist eine pure Funktion — nimmt `Customer`, gibt `Client`. Kein IO.
2. Datumsformatkonvertierung ist explizit und getestet.
3. Rückübersetzung wird benötigt, wenn der neue Service in die Legacy-Datenbank schreibt.

### Der Adapter verbindet alles

```java title="translation/LegacyClientRepositoryAdapter.java"
@Repository
public class LegacyClientRepositoryAdapter implements ClientRepository {

    private final LegacyCustomerJpaRepository legacyRepo;
    private final CustomerToClientTranslator translator;

    @Override
    public Optional<Client> findById(UUID id) {
        return legacyRepo
                .findByCustId(id.toString())          // (1) Legacy-Repo aufrufen
                .map(translator::translate);           // (2) An der Grenze übersetzen
    }

    @Override
    public Client save(Client client) {
        var customer = translator.reverseTranslate(client); // (3) Zurückübersetzen
        legacyRepo.save(customer);
        return client;
    }
}
```

1. Das Legacy-Repo verwendet String-IDs — der Adapter übernimmt die UUID ↔ String-Konvertierung.
2. Übersetzung passiert an der Adaptergrenze — die Domäne sieht nie `Customer`.
3. Schreibzugriff auf Legacy erfordert Rückübersetzung.

---

## TypeScript-BFF-Beispiel

Dasselbe Prinzip gilt in einem TypeScript-Backend-for-Frontend (BFF), das während
der Migration sowohl eine Legacy-API als auch einen neuen Service aufruft:

```typescript title="bff/clientAdapter.ts"
interface LegacyCustomerResponse {
    cust_id: string;
    cust_nm: string;
    cust_addr1: string;
    status: 0 | 1 | 2;
}

interface Client {
    id: string;
    name: string;
    address: string;
    status: 'INACTIVE' | 'ACTIVE' | 'SUSPENDED';
}

const LEGACY_STATUS_MAP: Record<0 | 1 | 2, Client['status']> = {
    0: 'INACTIVE',
    1: 'ACTIVE',
    2: 'SUSPENDED',
};

// Der ACL — Übersetzung an der Grenze
function translateCustomer(legacy: LegacyCustomerResponse): Client {
    return {
        id: legacy.cust_id,
        name: legacy.cust_nm,
        address: legacy.cust_addr1,
        status: LEGACY_STATUS_MAP[legacy.status],
    };
}

// BFF-Service — Konsumenten sehen nur Client, nie LegacyCustomerResponse
export async function getClient(id: string): Promise<Client> {
    const response = await fetch(`/legacy/api/customers/${id}`);
    const legacy: LegacyCustomerResponse = await response.json();
    return translateCustomer(legacy); // (1) Übersetzung an der Netzwerkgrenze
}
```

1. Das BFF übersetzt einmal an der Grenze. Alle nachgelagerten Komponenten verwenden `Client`.

---

## Den ACL testen

Der Übersetzer ist das Wertvollste, was du testen kannst — hier passieren Datenverlust
oder Datenverfälschung. Da er keine Abhängigkeiten hat, ist er trivial per Unit-Test testbar:

```java title="CustomerToClientTranslatorTest.java"
class CustomerToClientTranslatorTest {

    private final CustomerToClientTranslator translator = new CustomerToClientTranslator();

    @Test
    void übersetzt_aktiven_customer_zu_client() {
        var customer = new Customer();
        customer.custId   = UUID.randomUUID().toString();
        customer.custNm   = "Acme GmbH";
        customer.custAddr1 = "Hauptstraße 1";
        customer.status   = 1;
        customer.createDt = "20240101";

        var client = translator.translate(customer);

        assertThat(client.name()).isEqualTo("Acme GmbH");
        assertThat(client.status()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(client.joinedDate()).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    @Test
    void wirft_bei_unbekanntem_statuscode() {
        var customer = new Customer();
        customer.custId   = UUID.randomUUID().toString();
        customer.status   = 99; // unbekannter Code

        assertThatThrownBy(() -> translator.translate(customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }
}
```

---

## Migrations-Checkliste

- [ ] Neues Domänenmodell definieren (saubere Namen, korrekte Typen, keine Legacy-Abkürzungen)
- [ ] Übersetzerklasse erstellen (pure Funktion — kein IO, kein Spring)
- [ ] Unit-Tests für den Übersetzer schreiben (alle Statuscodes, Edge Cases, Fehlerpfade)
- [ ] Adapter erstellen, der das Legacy-Repository über den Übersetzer kapselt
- [ ] Verifizieren: alle Aufrufer des Legacy-Codes gehen jetzt durch den Adapter
- [ ] Überwachen: Übersetzungsfehler in Produktion loggen (unbekannte Statuscodes, Parse-Fehler)
- [ ] Aufräumen: nach Entfernung des Legacy-Systems den Übersetzer löschen

---

## Häufige Fehler

**Das Legacy-Modell am ACL vorbeisickern lassen.**
Wenn ein Feld aus `Customer` in einem Use-Case oder Domänenobjekt auftaucht,
hat der ACL ein Loch. Erzwinge die Grenze mit ArchUnit:

```java
@Test
void keine_legacy_typen_in_der_domaene() {
    classes()
        .that().resideInAPackage("..newdomain..")
        .should().onlyDependOnClassesThat()
            .resideOutsideOfPackage("..legacy..")
        .check(importedClasses);
}
```

**Bidirektionale Übersetzer schreiben, wenn sie nicht benötigt werden.**
Wenn der neue Service nie zurück in das Legacy-System schreibt, implementiere
`reverseTranslate` nicht. YAGNI gilt.

**In Domänenobjekten übersetzen.**
Der Übersetzer gehört in die Adapterschicht — nicht in `Client`, nicht in Use-Cases.
Domänenobjekte müssen unwissend über Legacy-Strukturen bleiben.

---

## Weiterführende Links

- [Martin Fowler: Anti-Corruption Layer](https://martinfowler.com/eaaCatalog/corruptionLayer.html)
- [DDD — Anti-Corruption Layer Pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/anti-corruption-layer)
- [spring-hexagonal-archetype](https://github.com/marvinrichter/spring-hexagonal-archetype) — der Zielzustand, auf den diese Migration hinführt
