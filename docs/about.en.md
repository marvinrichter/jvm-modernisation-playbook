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

I'm **Marvin Richter**, a freelance architect based in Germany. Most of my clients come
to me when a legacy Spring MVC monolith has grown to the point where a feature that
should take a day takes two weeks. I help them figure out whether that's a rewrite, a
migration, or a structural change — and then I do it with them.

If you're planning a modernisation — or not yet sure whether you need one —
**[marvin-richter.de →](https://marvin-richter.de)**

## Contributing

Found an error? Have a pattern to add?
[Open an issue](https://github.com/marvinrichter/jvm-modernisation-playbook/issues)
or send a PR — the `docs/` directory is the place to start.
