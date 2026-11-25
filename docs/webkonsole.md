# Auftrag 1 – TicTacTest im Browser (Web-Konsole)

## Architektur

```
Browser ──► Frontend (statische Seite: index.html, app.js, config.js, version.txt)
   │            bettet per <iframe> die Web-Konsole ein, zeigt beide Versionen
   ▼
nginx :80 ──► /version.txt  (Textdatei mit der laufenden Backend-Version)
          └─► /            ──► ttyd :7681 (nur localhost) ──► java -jar tictactest-<version>.jar
                                 (ein Prozess pro Browser-Verbindung, I/O über WebSocket + PTY)
```

Das Spiel selbst wird **nicht verändert**. Betrieben wird das unveränderte Jar aus dem GitHub-Release.

## Werkzeugwahl: ttyd

| Werkzeug | Bewertung |
|---|---|
| **ttyd** ✅ | Statisches Binary (keine Abhängigkeiten), echtes Pseudo-Terminal → ANSI-Farben des Spiels funktionieren, xterm.js im Browser enthalten, aktiv gepflegt, Optionen für Schreibzugriff (`-W`), Origin-Check (`-O`) und Client-Limit (`-m`). |
| GoTTY | Funktional ähnlich, aber seit Jahren kaum gepflegt → Sicherheitsrisiko. |
| websocketd | Nur zeilenweiser Text, kein PTY. Eigene HTML/JS-Seite und ANSI-Behandlung nötig. |

Version **1.7.7**, auf dem Server mit fester SHA-256-Prüfsumme installiert (siehe `iac/variables.tf`).

## Funktionsweise

- **WebSocket**: Der Browser lädt von ttyd eine Seite mit xterm.js, die eine WebSocket-Verbindung öffnet. Tastatureingaben gehen über den WebSocket zu ttyd, die Ausgabe des Programms kommt zurück.
- **Pseudo-Terminal (PTY)**: ttyd startet das Programm in einem PTY. Für Java sieht es aus wie ein echtes Terminal (stdin/stdout, ANSI-Codes).
- **Prozess pro Verbindung**: Jeder Tab/jede Verbindung startet eine eigene JVM = eine eigene Partie.

## Lokal starten (nur aus Release-Assets)

Voraussetzung: Linux/Devcontainer/WSL mit Java ≥ 21 und ttyd (`curl -L -o ttyd https://github.com/tsl0922/ttyd/releases/download/1.7.7/ttyd.x86_64 && chmod +x ttyd`).

```bash
V=1.2.0
gh release download v$V -p "tictactest-*" -p checksums.txt   # Jar NICHT lokal bauen
sha256sum -c checksums.txt

# 1) Web-Konsole auf Port 8080 (Version liefert ttyd nicht -> siehe unten)
./ttyd -p 8080 -W java -jar tictactest-$V.jar

# 2) Frontend auf Port 8000 (zweiter, getrennter Server)
mkdir site && unzip tictactest-frontend-$V.zip -d site
cd site && python3 -m http.server 8000
```

Browser: <http://localhost:8000>. Das Frontend liest `config.js` (lokal: `localhost:8080`).
Für die Versionsabfrage lokal entweder nginx wie auf dem Server verwenden oder `echo $V > version.txt` und einen
kleinen Server mit CORS-Header davorstellen – ohne CORS meldet die Browser-Konsole:
*„blocked by CORS policy: No 'Access-Control-Allow-Origin' header“* (Frontend :8000 und Backend :8080 sind verschiedene Origins).

## Beobachtungen

| Frage | Ergebnis |
|---|---|
| Optionen fürs Tippen | ttyd ist seit 1.7 standardmässig **read-only**. Erst `-W` (`--writable`) erlaubt Eingaben. |
| Spielende | Das Jar beendet sich (`...and the winner is` / `it's a draw!`). ttyd zeigt die Verbindung als geschlossen und bietet *„Press ⏎ to Reconnect“* an. |
| Neues Spiel | Reconnect (Enter) oder Button *Neues Spiel* im Frontend (lädt das iframe neu) → neue Verbindung → neue JVM. |
| Ungültige Eingabe | `HumanPlayer` meldet *Invalid input…* bzw. *already occupied* und fragt erneut, das Spiel läuft weiter. |
| Zwei Tabs | Zwei Verbindungen = **zwei Java-Prozesse** (`pgrep -c java` → 2). |
| 1 GB RAM | Jede JVM braucht ~50–80 MB RSS (mit `-Xmx32m -XX:TieredStopAtLevel=1 -XX:+UseSerialGC`). Ohne Limit würde jeder Besucher eine JVM starten → OOM-Killer. Deshalb: `-m 8` (max. 8 Partien), kleine JVM-Optionen, 1 GB Swap als Reserve. |

## Sicherheit

Eine Web-Konsole ist für **jeden** erreichbar, der die URL kennt. Läuft dort eine Shell (`ttyd -W bash`), hat jeder
Besucher eine Remote-Shell auf dem Server: Daten lesen, Instance-Profile-Credentials über die Metadaten (IMDS)
abgreifen und damit das AWS-Konto missbrauchen, Krypto-Miner installieren, andere Systeme angreifen.

Massnahmen:

1. ttyd startet **nur** `/usr/local/bin/tictactest-game`, das per `exec` direkt `java -jar` startet – keine Shell, keine Argumente aus dem Browser (`--url-arg` ist aus).
2. Dienst läuft als unprivilegierter System-User `tictactest` mit `/sbin/nologin`, `NoNewPrivileges`, `ProtectSystem=strict`, `ProtectHome`, `PrivateTmp`.
3. ttyd hört nur auf `lo`; von aussen ist nur nginx auf Port 80 erreichbar.
4. `-O` prüft die Origin des WebSockets (Schutz vor Cross-Site-WebSocket-Hijacking), `-m 8` begrenzt die Ressourcen.
5. IMDSv2 ist erzwungen (`http_tokens = "required"`).

## Versionsabfrage (Teil 2)

`GET http://<server>/version.txt` → z. B. `1.2.0`.

Die Version stammt **automatisch aus dem Release-Tag**: Das Deployment ruft `tictactest-deploy 1.2.0` auf
(Version = Tag ohne `v`), das Skript schreibt `TICTACTEST_VERSION=1.2.0` nach `/etc/tictactest/app.env`.
Beim Start des Dienstes schreibt `ExecStartPre` (`tictactest-prepare`) diese Version nach
`/var/www/tictactest/version.txt`, nginx liefert sie aus (mit `Access-Control-Allow-Origin: *` für das Frontend).
Das Jar trägt dieselbe Version zusätzlich im Manifest (`Implementation-Version`, gesetzt via `-PprojVersion`).

## Frontend (Teil 3)

Ordner [`frontend/`](../frontend): plain HTML/CSS/JS, kein Framework, kein Build-Step.

- Titel, Spielregeln, eingebettetes Terminal (`<iframe>`), Backend- und Frontend-Version.
- Die URL der Web-Konsole steht **nicht im Code**: `config.js` setzt `window.TICTACTEST_CONFIG` und wird vor `app.js` geladen.
  - lokal: `frontend/config.js` → `localhost:8080`
  - Cloud: OpenTofu schreibt `config.js` mit der Elastic-IP-Adresse in den S3-Bucket; das Frontend-Deployment schliesst `config.js` aus.
- Frontend-Version: `version.txt`, beim Release aus dem Tag geschrieben.

## Release erweitern (Teil 4)

Ein Tag `v*` erzeugt jetzt (siehe [`release.yml`](../.github/workflows/release.yml)):

- `tictactest-<version>.jar`
- `tictactest-frontend-<version>.zip`
- `checksums.txt` (SHA-256 beider Assets)

**Start-Skript, systemd-Unit, nginx-Konfiguration** gehören zur **Infrastruktur** (`iac/files/`, per cloud-init installiert):
Sie beschreiben *wie* und *wo* die App läuft, sind für jede Version gleich und ändern sich mit der Umgebung, nicht mit dem Spiel.
Würden sie im Release liegen, müsste jedes Deployment auch die Server-Konfiguration ändern.

**Warum das Frontend als Release-Asset?** Ein Release ist eine unveränderliche, versionierte Einheit. Mit dem Archiv
lässt sich genau das Frontend von Version X jederzeit wieder ausliefern (Rollback), ohne aus Git neu zu bauen;
Jar und Frontend gehören nachvollziehbar zum selben Tag, die Prüfsumme garantiert, dass ausgeliefert wird, was getestet wurde.
