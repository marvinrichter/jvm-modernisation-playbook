# Contributing to jvm-modernisation-playbook

The playbook covers three production-proven patterns for modernising JVM monoliths. Contributions that sharpen the examples or documentation are welcome — scope is intentionally narrow.

## Before you open a PR

- Check [open issues](https://github.com/marvinrichter/jvm-modernisation-playbook/issues) to avoid duplicate work
- For significant changes, open an issue first to discuss the approach
- Keep PRs small and focused — one concern per PR

## Prerequisites

- Java 21+
- Maven 3.9+ (each example ships an `./mvnw` wrapper)
- Docker (required for Testcontainers integration tests)

## Setup

```bash
git clone https://github.com/marvinrichter/jvm-modernisation-playbook.git
cd jvm-modernisation-playbook

# Run all three examples
cd examples/strangler-fig && ./mvnw verify
cd ../branch-by-abstraction && ./mvnw verify
cd ../acl && ./mvnw verify
```

## Project structure

```
docs/patterns/          — MkDocs content (DE default, .en.md English variants)
examples/
  strangler-fig/        — Spring Boot gateway routing old → new paths
  branch-by-abstraction/ — Service extracted behind interface with feature flag
  acl/                  — Domain translation layer between legacy and new model
mkdocs.yml              — Site config, navigation, i18n
```

## What belongs here (and what doesn't)

**In scope:**
- Improvements to the three existing examples (code clarity, correctness, idiomatic Spring Boot)
- Documentation fixes or translation improvements (German default, English `.en.md` variants)
- Bug fixes where `./mvnw verify` fails
- Dependency updates (Spring Boot, Testcontainers, ArchUnit)

**Out of scope:**
- New patterns — open a discussion first; the playbook is intentionally focused on three
- Framework changes (e.g. switching from Maven to Gradle)
- Production-concern additions (auth, schema migration, Docker Compose) — examples must stay minimal

## Example conventions

- Each example demonstrates "before" (legacy code) and "after" (hexagonal target state)
- A feature flag switches between implementations at runtime
- The "after" state references the structure from [spring-hexagonal-archetype](https://github.com/marvinrichter/spring-hexagonal-archetype)
- All examples use the Order/Customer domain (fictional — no real client data)

## Documentation conventions

- Site is bilingual: German default (`.md`), English alternate (`.en.md`)
- Both language variants must be kept in sync when you update content
- Preview locally: `pip install mkdocs-material && mkdocs serve`

## Commit format

[Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add integration test to acl example
fix: correct feature flag default in branch-by-abstraction
docs: update strangler-fig German pattern guide
chore: bump Spring Boot to 3.4.4
```

## Reporting security issues

Do not open a public GitHub issue for security vulnerabilities. Email marvin@marvin-richter.de directly.
