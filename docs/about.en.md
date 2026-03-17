# About

## Why this playbook exists

Modernisation projects fail not because the patterns are unknown — they fail because the
jump from "I understand Strangler Fig" to "I know how to wire this into our Spring Boot
codebase" is enormous and under-documented.

I wrote this to close that gap. Every pattern here maps to real code I've written on
client engagements. The examples are minimal but complete — no hand-waving,
no "implementation left as an exercise for the reader".

## The companion archetype

This playbook's "after" state is the
[`spring-hexagonal-archetype`](https://github.com/marvinrichter/spring-hexagonal-archetype).
Run:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=de.marvinrichter \
  -DarchetypeArtifactId=spring-hexagonal-archetype \
  -DarchetypeVersion=LATEST
```

...to generate a Spring Boot project with the hexagonal structure that the migration
patterns in this playbook target.

## About the author

I'm **Marvin Richter**, a freelance architect based in Germany specialising in:

- JVM modernisation (Spring Boot, Quarkus, Micronaut)
- Hexagonal / ports-and-adapters architecture
- Kubernetes-native deployment
- Platform engineering

I've helped teams migrate from legacy Spring MVC monoliths to well-structured,
observable, and testable Spring Boot services.

If you're planning a modernisation — or wondering whether a rewrite is actually
necessary — I'm happy to talk.

**[marvin-richter.de →](https://marvin-richter.de)**

## Contributing

Found an error? Have a pattern to add?
[Open an issue](https://github.com/marvinrichter/jvm-modernisation-playbook/issues)
or submit a PR — the `docs/` directory is the place to start.
