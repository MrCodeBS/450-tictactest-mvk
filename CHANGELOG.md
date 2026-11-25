# Changelog

Alle nennenswerten Änderungen an diesem Projekt werden in dieser Datei dokumentiert.
Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/) und dieses Projekt folgt [Semantic Versioning](https://semver.org/lang/de/).

## [1.4.0] - 2026-11-25

### Changed
- **Backend:** Tippfehler im Eingabe-Prompt behoben (`where to put the next …`).
- **Frontend:** Neue Akzentfarbe (violett).

## [1.3.0] - 2026-11-25

### Changed
- **Backend:** `main()` spielt fest gegen den `PerfectPlayer` (Minimax) statt `GreedyPlayer`.

## [1.2.0] - 2026-11-25

### Added
- **Web-App (M324):** Frontend (`frontend/`), ttyd-Web-Konsole auf EC2, OpenTofu-Infrastruktur, Release mit Frontend-Zip, automatisches Deployment nur der geänderten Teile (`deploy.yml`). Siehe `docs/deployment.md`.

## [1.1.0] - 2026-11-18

### Added
- **PerfectPlayer (Minimax):** Neuer unschlagbarer Computer-Spieler basierend auf dem Minimax-Algorithmus (`ch.bbw.m450.tictactoe.players.PerfectPlayer`).
- **JUnit Pioneer End-to-End-Tests:** Vollständige E2E-Konsolentests (`TicTacToeConsoleE2ETest`) mit `@StdIo`, `@StdIn`, `@StdOut` und `@Timeout` gegen Denial-of-Service.
- **Property-based Testing (jqwik):** Umfassende Invarianten- und Eigenschaftstests (`TicTacToePropertiesTest`) für Spielfelder, Züge und Stein-Opponenten.
- **PITest Mutation Testing:** Integration des PIT-Plugins in Gradle mit 89 % Mutation Coverage und 100 % Line Coverage auf mutierten Klassen.
- **90 % Branch-Coverage-Gate:** Erweiterung des CI-Quality-Gates (`coverage_gate.py`) mit strikter Prüfung auf mindestens 90 % Branch Coverage (aktuell 100 % erreicht).
- **Automatisierter Release-Workflow:** GitHub Actions Workflow `.github/workflows/release.yml` für Releases bei Git-Tags (`v*`).

### Fixed
- **Scanner-Bug in `HumanPlayer` behoben:** `System.in` wird nicht mehr durch ein vorzeitiges `try-with-resources` geschlossen, sodass Folgerunden stabil spielbar sind.
- **Input-Validierung:** Ungültige Eingaben (Buchstaben, Zahlen ausserhalb 0-8, belegte Felder) werden abgefangen und neu abgefragt, anstatt das Programm mit einer Exception abstürzen zu lassen.

---

## [1.0.0] - 2026-09-22

### Added
- Erste spielbare Version von TicTacTest für die Konsole.
- Zwei Spieler können abwechselnd Züge durchführen.
- `GreedyPlayer`: Einfache naive KI, die stets das erste freie Feld wählt.
- `HumanPlayer`: Menschlicher Spieler mit Eingabe über die Konsole.
- Ausführbares JAR-Artefakt `tictactest-1.0.0.jar` via Gradle `jar`-Task.
- Basis-Testsuite mit parametrisierten Tests für Gewinnerkennung (`isWin`).
- Alpine-basierter DevContainer mit Java, Gradle und Non-Root-User `devuser`.
- Basis-CI-Pipeline mit JaCoCo-Code-Coverage und GitHub Pages Deployment.

### Fixed
- Fehler bei der horizontalen, vertikalen und diagonalen Gewinnerkennung behoben (#12).
- Behandlung ungültiger Spielzüge und Ablehnung identischer Spieler-Instanzen.
