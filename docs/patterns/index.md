# Migrationsmuster

Die meiste JVM-Modernisierungsarbeit lässt sich auf drei strukturelle Probleme
reduzieren: Legacy-Code läuft hinter HTTP, oder er wird direkt als Klasse aufgerufen,
oder die Domänenmodelle stimmen nicht überein. Für jedes gibt es ein Muster — jeweils
mit einem ausführbaren Spring-Boot-Beispiel, das die konkrete Code-Transformation zeigt.

---

## Musterübersicht

| Muster | Situation | Risiko | Typische Dauer |
|--------|-----------|--------|----------------|
| [Strangler Fig](strangler-fig.md) | Per HTTP erreichbares Legacy; Route-für-Route-Ablösung | Gering — Legacy bedient anfangs noch den gesamten Traffic | Wochen bis Monate pro Route |
| [Branch-by-Abstraction](branch-by-abstraction.md) | In-Process-Klassen-/Service-Ablösung | Mittel — beide Implementierungen müssen parallel gepflegt werden | Tage bis Wochen pro Komponente |
| [Anti-Corruption Layer](anti-corruption-layer.md) | Unterschiedliche Domänenmodelle zwischen Legacy und Neu | Gering — Übersetzung ist explizit und testbar | 1–3 Tage Aufbau; laufend bei Modelldivergenz |

---

## Muster kombinieren

Diese Muster schließen sich nicht gegenseitig aus. Eine typische Modernisierung verwendet alle drei:

1. **Strangler Fig** — leitet Traffic zu einem neuen Spring-Boot-Service um.
2. **Branch-by-Abstraction** — ersetzt interne Services innerhalb dieses neuen Systems,
   während Legacy-Code schrittweise extrahiert wird.
3. **Anti-Corruption Layer** — sitzt an der Grenze zwischen neuem und altem Service,
   um Domänenmodell-Pollution zu verhindern.

Die Reihenfolge ist wichtig: der Strangler Fig steuert, was das Legacy-System überhaupt
noch erreicht. Branch-by-Abstraction restrukturiert die Interna des neuen Services.
Der ACL verhindert, dass die beiden Domänenmodelle ineinander bluten.

---

## Auf Code-Ebene

Die drei Muster oben bauen ein System um. Wie die einzelne Klasse danach
aussieht, steht in [Code-Muster: gut und schlecht](code-patterns.md): sechs
Paare aus schlechter und guter Fassung, jeweils mit der Zeile, die den
Unterschied trägt, und den ArchUnit-Regeln, die sie im Build erzwingen.
