# Anti-Corruption Layer

> **Translate between your legacy domain model and the new one at the boundary.
> Prevents legacy naming, types, and broken assumptions from infecting fresh code.**

---

## The problem

Your legacy system has a `Customer` object with fields named `cust_nm`, `cust_addr1`,
and a status encoded as `0/1/2`. Your new service has a `Client` with `name`,
`address`, and a `ClientStatus` enum. They represent the same real-world concept
but with different models.

Without an Anti-Corruption Layer (ACL), one of two things happens:
1. You rename everything in the legacy system — enormous risk, huge diff.
2. The legacy naming leaks into the new system — the "big ball of mud" grows a new head.

The ACL creates an explicit translation boundary. The legacy model stays as-is.
The new model stays clean. The translator lives in one place and is testable in isolation.

---

## When to use it

- Legacy and new system have different domain concepts (different names, different
  types, different cardinality)
- You're calling a third-party API whose model doesn't match your domain
- You're integrating with a legacy database schema you don't own and can't change

**You almost always need this alongside a Strangler Fig or Branch-by-Abstraction.**
Those patterns create the structural separation; the ACL prevents conceptual pollution
across that boundary.

---

## How it works

```
New domain          ACL (translator)       Legacy domain
──────────          ────────────────       ─────────────

Client              ← translate ←          Customer
  .name             ← cust_nm  ←            .cust_nm
  .address          ← cust_addr1 ←          .cust_addr1
  .status           ← "0"→INACTIVE          .status (int)
                       "1"→ACTIVE
                       "2"→SUSPENDED
```

The translator is a plain Java class — no framework, no annotations.
It takes a legacy object and returns a new domain object (or vice versa).
That's it.

---

## Example: Customer → Client translation

The runnable example is in
[`examples/acl/`](https://github.com/marvinrichter/jvm-modernisation-playbook/tree/main/examples/acl).

### Project structure

```
acl/
├── src/main/java/de/marvinrichter/acl/
│   ├── AclApplication.java
│   ├── legacy/
│   │   ├── Customer.java              # Legacy model (unchanged)
│   │   └── LegacyCustomerRepository.java
│   ├── newdomain/
│   │   ├── Client.java                # New domain model
│   │   ├── ClientStatus.java
│   │   └── ClientRepository.java      # Port
│   └── translation/
│       ├── CustomerToClientTranslator.java   # (1) The ACL
│       └── LegacyClientRepositoryAdapter.java # Adapter using the translator
```

### The legacy model (do not modify)

```java title="legacy/Customer.java"
// (1) Legacy object — we don't own this, can't rename it
public class Customer {
    public String custId;
    public String custNm;      // customer name
    public String custAddr1;   // address line 1
    public String custAddr2;   // address line 2
    public int    status;      // 0=inactive, 1=active, 2=suspended
    public String createDt;    // date as "YYYYMMDD" string (yes, really)
}
```

1. Abbreviated field names, numeric status, date as string — classic legacy.

### The new domain model

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
                "Unknown legacy status code: " + code);
        };
    }
}
```

### The Anti-Corruption Layer translator

```java title="translation/CustomerToClientTranslator.java"
@Component
public class CustomerToClientTranslator {

    // (1) Pure translation — no Spring, no database, no side effects
    public Client translate(Customer customer) {
        return new Client(
            UUID.fromString(customer.custId),
            customer.custNm,
            new Address(customer.custAddr1, customer.custAddr2),
            ClientStatus.fromLegacyCode(customer.status),
            parseDate(customer.createDt)     // (2) Translate date format
        );
    }

    public Customer reverseTranslate(Client client) {
        // (3) Reverse translation for write-back to legacy
        var customer = new Customer();
        customer.custId   = client.id().toString();
        customer.custNm   = client.name();
        customer.custAddr1 = client.address().line1();
        customer.custAddr2 = client.address().line2();
        customer.status   = client.status().ordinal();
        customer.createDt = client.joinedDate()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return customer;
    }

    private LocalDate parseDate(String yyyymmdd) {
        return LocalDate.parse(yyyymmdd, DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
```

1. The translator is a pure function — it takes a `Customer`, returns a `Client`. No IO.
2. Date format conversion is explicit and tested.
3. Reverse translation is needed if the new service also writes back to legacy storage.

### The adapter wires it together

```java title="translation/LegacyClientRepositoryAdapter.java"
@Repository
public class LegacyClientRepositoryAdapter implements ClientRepository {

    private final LegacyCustomerRepository legacyRepo;
    private final CustomerToClientTranslator translator;

    public LegacyClientRepositoryAdapter(
            LegacyCustomerRepository legacyRepo,
            CustomerToClientTranslator translator) {
        this.legacyRepo = legacyRepo;
        this.translator = translator;
    }

    @Override
    public Optional<Client> findById(UUID id) {
        return legacyRepo
            .findByCustId(id.toString())                // (1) Call legacy repo
            .map(translator::translate);                // (2) Translate at the boundary
    }

    @Override
    public Client save(Client client) {
        var customer = translator.reverseTranslate(client); // (3) Translate back
        legacyRepo.save(customer);
        return client;
    }
}
```

1. Legacy repo uses string IDs — the adapter handles the UUID ↔ string conversion.
2. Translation happens at the adapter boundary — the domain never sees `Customer`.
3. Writing back to legacy requires reverse translation.

---

## TypeScript BFF example

The same ACL principle applies in a TypeScript Backend-for-Frontend (BFF) that
calls both a legacy API and a new service during migration.

```typescript title="bff/clientAdapter.ts"
// Legacy API response shape (don't modify)
interface LegacyCustomerResponse {
  cust_id: string;
  cust_nm: string;
  cust_addr1: string;
  status: 0 | 1 | 2;
}

// New domain model (clean)
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

// The ACL — translation at the boundary
function translateCustomer(legacy: LegacyCustomerResponse): Client {
  return {
    id: legacy.cust_id,
    name: legacy.cust_nm,
    address: legacy.cust_addr1,
    status: LEGACY_STATUS_MAP[legacy.status],
  };
}

// BFF service — consumers only see Client, never LegacyCustomerResponse
export async function getClient(id: string): Promise<Client> {
  const response = await fetch(`/legacy/api/customers/${id}`);
  const legacy: LegacyCustomerResponse = await response.json();
  return translateCustomer(legacy); // (1) Translation at the network boundary
}
```

1. The BFF translates once at the boundary. All downstream components use `Client`.
   If the legacy API changes field names, only `translateCustomer` needs updating.

---

## Testing the ACL

The translator is the highest-value thing to test — it's where data loss or corruption
happens. Because it has no dependencies, it's trivially unit-testable:

```java title="CustomerToClientTranslatorTest.java"
class CustomerToClientTranslatorTest {

    private final CustomerToClientTranslator translator = new CustomerToClientTranslator();

    @Test
    void translates_active_customer_to_client() {
        var customer = new Customer();
        customer.custId   = "00000000-0000-0000-0000-000000000001";
        customer.custNm   = "Acme Corp";
        customer.custAddr1 = "123 Main St";
        customer.custAddr2 = "";
        customer.status   = 1;
        customer.createDt = "20240101";

        var client = translator.translate(customer);

        assertThat(client.name()).isEqualTo("Acme Corp");
        assertThat(client.status()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(client.joinedDate()).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    @Test
    void throws_on_unknown_status_code() {
        var customer = new Customer();
        customer.custId   = "00000000-0000-0000-0000-000000000001";
        customer.status   = 99; // unknown code

        assertThatThrownBy(() -> translator.translate(customer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("99");
    }
}
```

---

## Migration checklist

- [ ] Define the new domain model (clean names, proper types, no legacy abbreviations)
- [ ] Create the translator class (pure function — no IO, no Spring)
- [ ] Write unit tests for the translator covering all status codes, edge cases, and error paths
- [ ] Create the adapter that wraps the legacy repository using the translator
- [ ] Verify: all callers of the legacy code now go through the adapter
- [ ] Monitor: log translation errors in production (unknown status codes, parse failures)
- [ ] Deprecate: once the legacy system is removed, delete the translator

---

## Common pitfalls

**Letting the legacy model leak past the ACL.**
If a field from `Customer` appears in a use case or domain object, the ACL has a hole.
Enforce the boundary with ArchUnit:

```java
@Test
void no_legacy_types_in_domain() {
    classes()
        .that().resideInAPackage("..newdomain..")
        .should().onlyDependOnClassesThat()
            .resideOutsideOfPackage("..legacy..")
        .check(importedClasses);
}
```

**Writing bi-directional translators when you don't need them.**
If the new service never writes back to the legacy system, don't implement
`reverseTranslate`. YAGNI applies.

**Translating inside domain objects.**
The translator belongs in the adapter layer — not in `Client`, not in use cases.
Keep domain objects unaware of legacy structures.

---

## Further reading

- [Martin Fowler: Anti-Corruption Layer](https://martinfowler.com/eaaCatalog/corruptionLayer.html)
- [DDD — Anti-Corruption Layer pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/anti-corruption-layer)
- [spring-hexagonal-archetype](https://github.com/marvinrichter/spring-hexagonal-archetype) — the target state this migration lands on
