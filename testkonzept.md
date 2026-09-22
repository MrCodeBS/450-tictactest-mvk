# Testkonzept Tic-Tac-Toe

## 1. Ziel

Das Testkonzept stellt sicher, dass die wichtigsten Regeln des Spiels korrekt
funktionieren. Getestet werden vor allem Gewinnerkennung, Spielzüge und die
Behandlung ungültiger Eingaben.

## 2. Testumfang

Im Testumfang sind:

- `TicTacToeMain.isWin()`
- `TicTacToeMain.play()`
- `GreedyPlayer.play()`
- gültige und ungültige Spielzüge

Die Ein- und Ausgabe auf der Konsole wird nicht im Detail getestet. Sie ist für
die Spiellogik nicht entscheidend.

## 3. Teststufen

### Unit-Tests

Jede wichtige Methode wird isoliert getestet. Dafür werden feste Spielfelder
als Test-Fixtures verwendet.

### Integrationstest

Ein vollständiger Spieldurchlauf mit zwei unterschiedlichen Spielern prüft,
ob Spielzüge, Spielerwechsel und Gewinnergebnis zusammenspielen.

## 4. Testfälle

| ID | Bereich | Testfall | Erwartetes Ergebnis |
|---|---|---|---|
| TC-01 | Gewinnerkennung | X hat eine horizontale Reihe | `true` |
| TC-02 | Gewinnerkennung | O hat eine vertikale Reihe | `true` |
| TC-03 | Gewinnerkennung | Ein Spieler gewinnt diagonal | `true` |
| TC-04 | Gewinnerkennung | Spielfeld ist leer | `false` |
| TC-05 | Gewinnerkennung | Kein Spieler hat drei Zeichen in einer Reihe | `false` |
| TC-06 | Spieler | GreedyPlayer erhält ein teilweise belegtes Feld | Erstes freies Feld wird gewählt |
| TC-07 | Spielablauf | Beide Spielobjekte sind identisch | `IllegalArgumentException` |
| TC-08 | Spielablauf | Spieler liefert ein belegtes Feld | `IllegalStateException` |
| TC-09 | Spielablauf | Ein Spieler gewinnt | Gewinnerfarbe wird zurückgegeben |
| TC-10 | Spielablauf | Alle Felder sind belegt, ohne Gewinner | `null` wird zurückgegeben |

Die Fälle TC-01 bis TC-05 werden als parametrische Tests umgesetzt. Dadurch
kann dieselbe Testlogik mit mehreren Spielfeldern geprüft werden.

## 5. Testdaten

Die Spielfelder werden zentral in `TicTacToeTestFixtures` erstellt. Verwendet
werden:

- leere Spielfelder
- horizontale, vertikale und diagonale Gewinnsituationen
- teilweise belegte Spielfelder
- volle Spielfelder ohne Gewinner

## 6. Durchführen der Tests

Die Tests werden lokal mit folgendem Befehl gestartet:

```bash
./gradlew test --console=plain
```

Alle Tests müssen erfolgreich sein. Bei einem Fehler wird zuerst der betroffene
Testfall geprüft und danach die Spiellogik korrigiert oder der Testfall
angepasst, falls die Erwartung falsch war.

## 7. Abnahmekriterien

Das Projekt gilt als getestet, wenn:

- alle Testfälle erfolgreich durchlaufen
- Gewinnsituationen korrekt erkannt werden
- ungültige Spielzüge abgelehnt werden
- ein Sieg und ein Unentschieden korrekt zurückgegeben werden
- die Tests automatisch über Gradle ausführbar sind

## 8. Nicht im Umfang

Performance-, Last- und Sicherheitstests sind für dieses kleine Konsolenspiel
nicht notwendig.


# feedback von Mariia ist gut es fählt give when und rolen und owner sind nicht s