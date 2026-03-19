# JVM Modernisation Playbook

> **Patterns for migrating legacy JVM monoliths to modern Spring Boot architecture —
> with real code, not diagrams.**

---

Most modernisation guidance stops at the diagram. A neat Strangler Fig drawing, a
three-layer hexagon, a "before/after" box-and-arrow slide. Then you're on your own when
the legacy `OrderService` is 2,000 lines, has no tests, and six teams depend on it.

This playbook covers the three patterns for JVM modernisation — not as theory,
but as runnable before/after code examples you can clone, adapt, and run today.

---

## What's in here

=== "Strangler Fig"

    Route traffic through a Spring Boot gateway. The legacy system handles everything
    at first. New capability is wired in path-by-path until the legacy is starved out
    and deleted.

    **When to use:** The legacy system is accessed via HTTP and can sit behind a proxy.

    [Go to pattern →](patterns/strangler-fig.md)

=== "Branch-by-Abstraction"

    Extract the component you want to replace behind a port interface. Put both old and
    new implementations behind it. Toggle via `@ConditionalOnProperty`. Delete the
    legacy once confidence is high.

    **When to use:** The component is a class / library called directly — not via HTTP.

    [Go to pattern →](patterns/branch-by-abstraction.md)

=== "Anti-Corruption Layer"

    Translate between your legacy domain model and the new one at the boundary.
    Prevents legacy naming, types, and broken assumptions from infecting fresh code.

    **When to use:** Legacy and new system have different domain concepts that must
    coexist during migration.

    [Go to pattern →](patterns/anti-corruption-layer.md)

---

## The target state

The [`spring-hexagonal-archetype`](https://github.com/marvinrichter/spring-hexagonal-archetype)
is the destination every example in this playbook targets.
If you want to see the structure before walking through the migrations, start there.

---

## Who this is for

- **Engineering managers** evaluating migration approaches before committing to one.
- **Tech leads** who know *what* pattern to use but need to see the Spring Boot
  specifics to feel confident proposing it to the team.
- **Senior developers** who want a reference implementation alongside the theory.

---

## Who wrote this

I'm [Marvin Richter](https://marvin-richter.de), a software architect. These are
patterns I've applied in real projects — not theoretical constructs assembled for a talk.
