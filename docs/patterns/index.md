# Migrationsmuster

Drei Muster decken den Großteil aller JVM-Modernisierungsszenarien ab.
Jedes ist mit einem ausführbaren Spring-Boot-Beispiel gekoppelt, damit du die
genaue Code-Transformation siehst — nicht nur das Konzept.

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

Die Reihenfolge ist wichtig: der Strangler Fig schafft die Separation of Concerns auf HTTP-Ebene;
Branch-by-Abstraction arbeitet innerhalb des neuen Services;
der ACL verwaltet die Kommunikation zwischen beiden.
