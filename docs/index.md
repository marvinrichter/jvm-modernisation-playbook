# JVM Modernisierungs-Playbook

> **Muster für die Migration von Legacy-JVM-Monolithen zu moderner Spring-Boot-Architektur —
> mit echtem Code, nicht nur mit Diagrammen.**

---

Die meisten Modernisierungsanleitungen hören beim Diagramm auf. Ein sauberes Strangler-Fig-Bild,
ein drei­schichtiges Hexagon, eine „Vorher/Nachher"-Folie. Und dann stehst du allein da,
wenn der Legacy-`OrderService` 2.000 Zeilen hat, keine Tests besitzt und sechs Teams von ihm abhängen.

Dieses Playbook zeigt die drei Muster, die ich bei jedem JVM-Modernisierungsprojekt einsetze —
nicht als Theorie, sondern als ausführbare Vorher/Nachher-Code-Beispiele, die du klonen,
anpassen und heute noch ausführen kannst.

---

## Was dich erwartet

=== "Strangler Fig"

    Leite Traffic über ein Spring-Boot-Gateway. Das Legacy-System bedient zunächst alles.
    Neue Funktionalität wird Route für Route eingebunden, bis das Legacy-System ausgehungert
    und gelöscht ist.

    **Wann verwenden:** Das Legacy-System ist per HTTP erreichbar und kann hinter einen Proxy gestellt werden.

    [Zum Muster →](patterns/strangler-fig.md)

=== "Branch-by-Abstraction"

    Extrahiere die zu ersetzende Komponente hinter eine Port-Schnittstelle. Lege alte und neue
    Implementierung dahinter. Schalte per `@ConditionalOnProperty` um. Lösche die Legacy-Implementierung,
    sobald das Vertrauen groß genug ist.

    **Wann verwenden:** Die Komponente wird direkt im Prozess aufgerufen — nicht per HTTP.

    [Zum Muster →](patterns/branch-by-abstraction.md)

=== "Anti-Corruption Layer"

    Übersetze zwischen dem Legacy-Domänenmodell und dem neuen an der Grenze.
    Verhindert, dass Legacy-Benennung, -Typen und -Annahmen frischen Code infizieren.

    **Wann verwenden:** Legacy- und neues System haben unterschiedliche Domänenkonzepte,
    die während der Migration koexistieren müssen.

    [Zum Muster →](patterns/anti-corruption-layer.md)

---

## Der Zielzustand

Der [`spring-hexagonal-archetype`](https://github.com/marvinrichter/spring-hexagonal-archetype)
ist der Zielzustand, auf den jedes Beispiel in diesem Playbook hinausläuft.
Wer die Struktur sehen will, bevor es in die Beispiele geht — fang dort an.

---

## Für wen ist das gedacht?

- **Engineering Manager und CTOs**, die Migrationsansätze evaluieren, bevor sie sich festlegen.
- **Tech Leads**, die wissen, *welches* Muster passt, aber die Spring-Boot-Spezifika brauchen,
  um es dem Team überzeugend vorzuschlagen.
- **Senior-Entwickler**, die neben der Theorie eine Referenzimplementierung suchen.

---

## Wer hat das geschrieben?

Ich bin [Marvin Richter](https://marvin-richter.de), freiberuflicher Architekt mit Schwerpunkt
JVM-Modernisierung. Ich habe jedes dieser Muster in echten Kundenprojekten eingesetzt —
keine Konstrukte, die für einen Vortrag zusammengestellt wurden.

Wenn du eine Modernisierung planst und eine zweite Meinung brauchst, welche Muster
passen und in welcher Reihenfolge:
**[Lass uns reden →](https://marvin-richter.de)**
