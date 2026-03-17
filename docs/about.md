# Über dieses Playbook

## Warum es dieses Playbook gibt

Modernisierungsprojekte scheitern nicht, weil die Muster unbekannt sind —
sie scheitern, weil der Sprung von „ich verstehe Strangler Fig" zu
„ich weiß, wie ich das in unsere Spring-Boot-Codebasis einbaue" enorm ist
und kaum dokumentiert wird.

Ich habe dieses Playbook geschrieben, um diese Lücke zu schließen. Jedes Muster
stammt aus echten Kundenprojekten. Die Beispiele sind minimal, aber vollständig —
kein Handwedeln, kein „die Implementierung bleibt dem Leser überlassen".

## Der begleitende Archetype

Der „Nachher"-Zustand dieses Playbooks ist der
[`spring-hexagonal-archetype`](https://github.com/marvinrichter/spring-hexagonal-archetype).
Ausführen mit:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=de.marvinrichter \
  -DarchetypeArtifactId=spring-hexagonal-archetype \
  -DarchetypeVersion=LATEST
```

...generiert ein Spring-Boot-Projekt mit der hexagonalen Struktur, auf die die
Migrationsmuster in diesem Playbook abzielen.

## Über den Autor

Ich bin **Marvin Richter**, freiberuflicher Architekt mit Sitz in Deutschland.
Die meisten meiner Kunden kommen zu mir, wenn ein Legacy-Spring-MVC-Monolith so weit
gewachsen ist, dass ein Feature, das einen Tag dauern sollte, zwei Wochen braucht.
Ich helfe dabei herauszufinden, ob das ein Rewrite, eine Migration oder eine strukturelle
Änderung ist — und setze es dann gemeinsam mit dem Team um.

Wenn du eine Modernisierung planst — oder noch nicht sicher bist, ob du eine brauchst —
**[meld dich →](https://marvin-richter.de)**

## Mithelfen

Fehler gefunden? Hast du ein Muster beizusteuern?
[Öffne ein Issue](https://github.com/marvinrichter/jvm-modernisation-playbook/issues)
oder schicke einen PR — das `docs/`-Verzeichnis ist der richtige Einstiegspunkt.
