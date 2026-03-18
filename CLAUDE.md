# CLAUDE.md — jvm-modernisation-playbook

3 runnable patterns for modernising JVM monoliths to hexagonal Spring Boot architecture. Bilingual DE/EN site via MkDocs. Each pattern has a working code example and a step-by-step guide.

## Key commands

```bash
# MkDocs site (local preview)
pip install mkdocs-material
mkdocs serve          # http://localhost:8000

# Build the site
mkdocs build

# Run a pattern example
cd examples/strangler-fig
./mvnw spring-boot:run

cd examples/branch-by-abstraction
./mvnw spring-boot:run

cd examples/acl
./mvnw spring-boot:run

# Run tests for an example
cd examples/<pattern>
./mvnw verify
```

## Structure

```
docs/                        — MkDocs content (source for the site)
  patterns/
    strangler-fig.md
    branch-by-abstraction.md
    anti-corruption-layer.md
  de/                        — German translations
  en/                        — English content (default)
examples/
  strangler-fig/             — Spring Boot gateway routing old → new paths
  branch-by-abstraction/     — Service extracted behind interface with feature flag
  acl/                       — Domain translation layer between legacy and new model
mkdocs.yml                   — Site config, navigation, i18n
```

## Patterns covered

| Pattern | When to use |
|---------|-------------|
| Strangler Fig | Component accessible via HTTP — route traffic through a proxy |
| Branch-by-Abstraction | In-process class/library called directly — extract interface, feature flag |
| Anti-Corruption Layer | Legacy and new domains have different models — translate at boundary |

## Conventions

- Each example demonstrates "before" (legacy code) and "after" (hexagonal target)
- Feature flag switches between legacy and new implementation at runtime
- Target state in examples references/reuses `spring-hexagonal-archetype` structure
- All examples use the Order/Customer domain (fictional — no real client data)
- Site is bilingual: German default, English alternate. Both must be kept in sync
- Companion blog posts are drafted in `~/Work/marvin-richter/blog/drafts/` — not in this repo

## Blog series schedule

| Date | Slug | Status |
|------|------|--------|
| 2026-03-31 | strangler-fig-spring-boot | scheduled |
| 2026-04-14 | branch-by-abstraction-spring-boot | draft needed |
| 2026-04-28 | anti-corruption-layer-spring-boot | draft needed |
