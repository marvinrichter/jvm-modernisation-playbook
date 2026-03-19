# JVM Modernisation Playbook

> **Patterns for migrating legacy JVM monoliths to modern Spring Boot architecture —
> with real code, not diagrams.**

[**Read the playbook →**](https://marvinrichter.github.io/jvm-modernisation-playbook)

---

Most modernisation guidance stops at the diagram. This playbook covers the three
three patterns for JVM modernisation — as runnable
Spring Boot code examples you can clone, run, and adapt.

## Patterns covered

| Pattern | What it solves | Example |
|---------|---------------|---------|
| [Strangler Fig](docs/patterns/strangler-fig.md) | Replacing an HTTP-accessible legacy service route by route | `examples/strangler-fig/` |
| [Branch-by-Abstraction](docs/patterns/branch-by-abstraction.md) | Replacing an in-process class behind a port interface | `examples/branch-by-abstraction/` |
| [Anti-Corruption Layer](docs/patterns/anti-corruption-layer.md) | Translating between legacy and new domain models | `examples/acl/` |

Each example demonstrates both the "before" (legacy code) and "after" (hexagonal
target state) with a feature flag to switch between them.

## Prerequisites

- Java 21+ (examples use Java 21 for broad compatibility)
- Maven 3.9+ (included as `./mvnw` wrapper in each example)
- Docker (for Testcontainers in integration tests — not required for H2-based demos)

## Run an example

```bash
git clone https://github.com/marvinrichter/jvm-modernisation-playbook
cd jvm-modernisation-playbook/examples/strangler-fig

# Run with legacy active (default)
./mvnw spring-boot:run

# Run with new service active
./mvnw spring-boot:run -Dspring-boot.run.arguments="--feature.new-order-service.enabled=true"

# Run all tests
./mvnw verify
```

## The target state

Every "after" example lands on the hexagonal architecture structure from
[`spring-hexagonal-archetype`](https://github.com/marvinrichter/spring-hexagonal-archetype).
Use it to generate new services alongside your legacy system:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=de.marvinrichter \
  -DarchetypeArtifactId=spring-hexagonal-archetype \
  -DarchetypeVersion=LATEST
```

## About

Written by [Marvin Richter](https://marvin-richter.de).

## License

Apache 2.0
