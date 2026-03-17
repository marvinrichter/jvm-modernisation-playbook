# Migration Patterns

Most JVM modernisation work reduces to three structural problems: the legacy code is
behind HTTP, or it is a class called directly, or the domain models do not match.
There is a pattern for each — and each is paired with a runnable Spring Boot example
showing the exact code transformation, not just the concept.

---

## Pattern map

| Pattern | Situation | Risk | Typical duration |
|---------|-----------|------|-----------------|
| [Strangler Fig](strangler-fig.md) | HTTP-accessible legacy; route-by-route replacement | Low — legacy still handles all traffic initially | Weeks to months per route |
| [Branch-by-Abstraction](branch-by-abstraction.md) | In-process class/service replacement | Medium — both implementations must be maintained in parallel | Days to weeks per component |
| [Anti-Corruption Layer](anti-corruption-layer.md) | Different domain models between legacy and new | Low — translation is explicit and testable | 1–3 days to establish; ongoing as model diverges |

---

## How to combine them

These patterns are not mutually exclusive. A typical modernisation uses all three:

1. **Strangler Fig** — routes traffic to a new Spring Boot service.
2. **Branch-by-Abstraction** — replaces internal services within that new system
   as the legacy code is extracted incrementally.
3. **Anti-Corruption Layer** — sits at the boundary between new and legacy
   services to prevent domain model pollution.

The order matters: the Strangler Fig controls what reaches the legacy system at all.
Branch-by-Abstraction restructures the internals of the new service.
The ACL keeps the two domain models from bleeding into each other.
