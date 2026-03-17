# Schnellstart

## Voraussetzungen

Alle ausführbaren Beispiele in diesem Playbook benötigen:

- **Java 21** (via [SDKMAN](https://sdkman.io/): `sdk install java 21-tem`)
- **Maven 3.9+** (als `./mvnw`-Wrapper in jedem Beispiel enthalten)
- **Docker** (für Testcontainers in Integrationstests)

## Ein Beispiel ausführen

Jedes Muster hat ein eigenständiges Maven-Projekt unter `examples/`:

```bash
git clone https://github.com/marvinrichter/jvm-modernisation-playbook
cd jvm-modernisation-playbook/examples/strangler-fig   # oder branch-by-abstraction / acl

./mvnw spring-boot:run
```

Tests ausführen:

```bash
./mvnw verify
```

## Wie die Beispiele aufgebaut sind

Jedes Beispiel enthält:

| Verzeichnis | Zweck |
|-------------|-------|
| `src/main/java/de/marvinrichter/<muster>/legacy/` | Der Legacy-Code, den wir ersetzen |
| `src/main/java/de/marvinrichter/<muster>/newservice/` | Die hexagonale Zielimplementierung |
| `src/main/resources/application.properties` | Feature-Flags und Konfiguration |
| `src/test/` | Integrationstests, die beide Pfade (alt und neu) abdecken |

## Welches Muster passt?

Du weißt nicht, welches Muster für deine Situation geeignet ist?

```
Ist der Legacy-Code per HTTP erreichbar?
  JA  → Strangler Fig
  NEIN → Ist es eine einzelne Klasse/Service mit klarer Schnittstelle?
           JA  → Branch-by-Abstraction
           NEIN → Verwendet das neue System ein anderes Domänenmodell?
                    JA  → Anti-Corruption Layer
                    NEIN → Branch-by-Abstraction + Anti-Corruption Layer (kombinieren)
```

## Zusammenhang mit `spring-hexagonal-archetype`

Der „Nachher"-Zustand in jedem Beispiel entspricht der Package-Struktur, die
[`spring-hexagonal-archetype`](https://github.com/marvinrichter/spring-hexagonal-archetype) generiert.
Wenn du neben dem Legacy-System einen neuen Service startest, nutze den Archetype —
und dann die Muster hier, um Traffic dorthin zu leiten.
