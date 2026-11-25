# Web-App-Setup in ein eigenes Repo übernehmen

Mit [`scripts/setup-webapp.sh`](../scripts/setup-webapp.sh) lässt sich die komplette Web-App
(Frontend, OpenTofu-Infrastruktur, Release-/Deploy-Workflows) in ein anderes TicTacTest-Repo übernehmen.

## Voraussetzungen

| Was | Prüfen mit |
|---|---|
| Git Bash / Linux / WSL / Devcontainer | `bash --version` |
| OpenTofu ≥ 1.6 | `tofu version` |
| AWS CLI v2 | `aws --version` |
| optional: GitHub CLI, eingeloggt | `gh auth status` |
| Learner Lab gestartet (grüner Punkt) | – |
| Credentials aus *AWS Details → AWS CLI: Show* in `~/.aws/credentials` | `aws sts get-caller-identity` |
| Euer Build erzeugt `build/libs/tictactest-<version>.jar` | `build.gradle`: `archiveBaseName = 'tictactest'`, `version = project.findProperty('projVersion') ?: '1.0.0'` |

## Ablauf

```bash
# 1. Dieses Repo und euer Repo nebeneinander klonen
git clone https://github.com/MrCodeBS/450-tictactest-mvk.git
git clone https://github.com/<euer-user>/<euer-repo>.git

# 2. Alles in einem Durchgang
450-tictactest-mvk/scripts/setup-webapp.sh all <euer-repo>

#    ... oder Schritt für Schritt:
450-tictactest-mvk/scripts/setup-webapp.sh copy   <euer-repo>   # Dateien kopieren (bestehende werden gesichert)
450-tictactest-mvk/scripts/setup-webapp.sh infra  <euer-repo>   # tofu apply (mit "yes" bestätigen)
450-tictactest-mvk/scripts/setup-webapp.sh github <euer-repo>   # GitHub Variables + Secrets setzen

# 3. Committen, pushen, ersten Release taggen -> wird automatisch deployt
cd <euer-repo>
git add -A && git commit -m "feat: Web-App" && git push
git tag v1.0.0 && git push origin v1.0.0
```

## Was das Skript macht

| Schritt | Aktion |
|---|---|
| `copy` | Kopiert `frontend/`, `iac/` (ohne State/tfvars), `release.yml`, `deploy.yml`, `scripts/detect-changes.sh` und die Doku-Vorlagen. Vorhandene Dateien landen in `.webapp-backup-<datum>/`. Ergänzt `.gitignore`. |
| `infra` | Prüft die AWS-Credentials, `tofu init` + `tofu apply`, wartet bis `/version.txt` antwortet, zeigt die Outputs. |
| `github` | Mit `gh`: setzt die 6 Variables aus `tofu output` und die 3 AWS-Secrets direkt aus `~/.aws/credentials` (Werte werden nie angezeigt). Ohne `gh`: gibt genau aus, was von Hand einzutragen ist. |

## Wichtig

- **Secrets laufen ab**: Bei jeder neuen Learner-Lab-Session `~/.aws/credentials` aktualisieren und
  `scripts/setup-webapp.sh github <euer-repo>` erneut ausführen (oder die 3 Secrets von Hand ersetzen).
- **Secrets nur als Wert** eintragen, ohne `aws_secret_access_key=` davor – sonst schlägt der Deploy mit
  `SignatureDoesNotMatch` fehl.
- `terraform.tfstate` bleibt lokal bei der Person, die `infra` ausgeführt hat – nicht committen, nicht löschen.
- `docs/deployment.md` und `docs/webkonsole.md` sind **Vorlagen**: Testergebnisse, Run-Links und URLs
  durch eure eigenen ersetzen – sonst dokumentiert ihr unsere Läufe.
- Läuft bei euch noch der `GreedyPlayer` in `main()`, ist alles kompatibel; der Smoke-Test spielt einfach eine Partie.
