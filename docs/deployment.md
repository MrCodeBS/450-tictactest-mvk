# Deployment – TicTacTest Web-App (Aufträge 2 und 3)

> Web-App: siehe Link im [README](../README.md#web-app) · Web-Konsole und Frontend: [webkonsole.md](webkonsole.md)

## Inhalt

1. [Architektur](#1-architektur)
2. [Infrastruktur (Auftrag 2)](#2-infrastruktur-auftrag-2)
3. [Manuelles Deployment (Auftrag 2, Teil 3)](#3-manuelles-deployment-auftrag-2-teil-3)
4. [Robustheit (Auftrag 2, Teil 4)](#4-robustheit-auftrag-2-teil-4)
5. [Secrets-Konzept (Auftrag 3, Teil 1)](#5-secrets-konzept-auftrag-3-teil-1)
6. [Automatisches Deployment (Auftrag 3, Teile 2–5)](#6-automatisches-deployment-auftrag-3-teile-25)
7. [Nachweis der Trennung (Auftrag 3, Teil 6)](#7-nachweis-der-trennung-auftrag-3-teil-6)
8. [Reflexion und Promotion-Strategie (Auftrag 3, Teil 7)](#8-reflexion-und-promotion-strategie-auftrag-3-teil-7)

---

## 1. Architektur

```
                        ┌──────────────────────── AWS Learner Lab (us-east-1) ───────────────────────┐
                        │                                                                            │
Browser ──HTTP──────────┼─► S3 Static Website (Frontend-Bucket, öffentlich)                          │
   │                    │     index.html, app.js, style.css, version.txt   ← Deployment (Release-Zip) │
   │                    │     config.js                                     ← OpenTofu (Infrastruktur)│
   │ iframe + fetch     │                                                                            │
   └──HTTP──────────────┼─► Elastic IP ─► EC2 t2.micro (Amazon Linux 2023, LabInstanceProfile)       │
                        │                   nginx :80 ─┬─ /version.txt                               │
                        │                              └─ /  ─► ttyd :7681 (lo) ─► java -jar (pro Tab)│
                        │                   systemd tictactest.service (User tictactest)             │
                        │                        ▲ s3 cp (Instance Profile, keine Keys)              │
                        │                   S3 Artefakt-Bucket (privat): backend/<v>/tictactest-<v>.jar│
                        └────────────────────────▲───────────────────────────────▲───────────────────┘
                                                 │ aws s3 cp / sync              │ aws ssm send-command
                                       GitHub Actions (Release → Deploy) ────────┘
                                                 ▲ gh release download (Assets, nicht neu gebaut)
                                       GitHub Release vX.Y.Z: Jar, Frontend-Zip, checksums.txt
```

**Trennung Infrastruktur ↔ Applikation**

| Verantwortlich | Was | Wann |
|---|---|---|
| OpenTofu (`iac/`) | EC2, EIP, Security Group, Buckets, `config.js`, Java, ttyd, nginx, systemd-Unit, Deploy-Skript | nur lokal `tofu apply`, wenn sich die Infrastruktur ändert |
| Deployment (`deploy.yml`) | *welche* Version läuft: Jar in S3 + `app.env` auf der Instanz, Frontend-Dateien im Bucket | bei jedem Release bzw. manuell |

---

## 2. Infrastruktur (Auftrag 2)

Dateien: [`iac/main.tf`](../iac/main.tf), [`iac/s3.tf`](../iac/s3.tf), [`iac/outputs.tf`](../iac/outputs.tf),
[`iac/variables.tf`](../iac/variables.tf), [`iac/user_data.yaml.tftpl`](../iac/user_data.yaml.tftpl), [`iac/files/`](../iac/files).

### Anforderungen und Umsetzung

| Anforderung | Umsetzung |
|---|---|
| Feste öffentliche Adresse | `aws_eip` – überlebt Stop/Start der Instanz bei neuer Learner-Lab-Session |
| Zugriff ohne Access Keys | `data "aws_iam_instance_profile" "lab"` → `LabInstanceProfile` (LabRole). Eigene IAM-Rollen sind im Lab verboten. Die Instanz bekommt temporäre Credentials über IMDSv2. |
| Laufzeitumgebung | cloud-init (`user_data`): `java-25-amazon-corretto-headless` + `nginx` aus den signierten AL2023-Repos; ttyd **1.7.7** von GitHub mit **SHA-256-Prüfung** (`sha256sum -c`), bricht bei Abweichung ab |
| Dienst | `tictactest.service`: `WantedBy=multi-user.target` (Autostart), `User=tictactest` (nicht root, `/sbin/nologin`), `Restart=always` |
| Gleiche Version nach Neustart | Version steht persistent in `/etc/tictactest/app.env` (EBS). `ExecStartPre` holt das Jar bei Bedarf aus S3 und schreibt `version.txt` |
| Versionsabfrage | `http://<EIP-DNS>/version.txt` (nginx, CORS `*`) |
| Frontend | S3 Static Website, `bucket_prefix` (global eindeutige Namen), Public-Read-Policy nur `s3:GetObject` |
| **Learner-Lab-Besonderheit S3** | Eine SCP verbietet `s3:GetBucketObjectLockConfiguration`. `resource "aws_s3_bucket"` liest das bei jedem Refresh und scheitert (getestet mit AWS-Provider 5.100 und 6.74). Lösung: Nur die zwei Buckets werden über einen kleinen `aws_cloudformation_stack` erzeugt (CloudFormation generiert eindeutige Namen wie `bucket_prefix`), Website-Konfiguration, Policy, Public-Access-Block und `config.js` sind normale OpenTofu-Ressourcen. |
| Konsolen-URL nicht im Code | OpenTofu schreibt `config.js` (`aws_s3_object`) mit der EIP-Adresse; Frontend-Deployment schliesst `config.js` aus |

### Security Group

| Regel | Begründung |
|---|---|
| Ingress TCP 80 von `0.0.0.0/0` | nginx: Web-Konsole + Versionsabfrage. Muss öffentlich sein, sonst kann niemand spielen. |
| Ingress TCP 22 (nur wenn `ssh_public_key` gesetzt) von `allowed_ssh_cidr` | optional fürs Debugging. Standard: **kein SSH**, Zugriff über SSM. |
| Egress alles | dnf-Repos, GitHub (ttyd-Download), S3- und SSM-Endpunkte (HTTPS). |
| **Nicht** offen: 7681 | ttyd hört nur auf `lo`, ist nur über nginx erreichbar. |

### Wie kommt das Jar auf die Instanz? → privater S3-Bucket + SSM

Gewählt: Pipeline lädt das Jar aus dem GitHub-Release, legt es in den **privaten Artefakt-Bucket**
(`backend/<version>/tictactest-<version>.jar`), und die Instanz holt es über das **Instance Profile**.

- **Privates Repo**: funktioniert trotzdem – die Instanz braucht keinen GitHub-Token (Download von GitHub-Release-Assets privater Repos bräuchte einen Token auf der Instanz).
- **Nach Neustart**: `app.env` sagt welche Version, das Jar liegt lokal in `/opt/tictactest/releases/<v>/`, sonst holt `ExecStartPre` es aus S3 (z. B. nach Neuerstellung der Instanz).
- **Version wechseln** ohne neue Instanz: `aws ssm send-command … tictactest-deploy <v>`.
- Verworfen: Download in `user_data` – läuft nur beim ersten Boot, eine neue Version bräuchte eine neue Instanz bzw. `user_data_replace_on_change = true` würde die Instanz bei jeder Versionsänderung **ersetzen** (neue Instanz-ID, Downtime, verlorener lokaler Zustand). Genau das soll vermieden werden.

**SSM vs. SSH aus GitHub Actions**

| | SSM `send-command` ✅ | SSH |
|---|---|---|
| Port | keiner offen nötig | Port 22 offen (GitHub-Runner-IPs ändern sich → praktisch `0.0.0.0/0`) |
| Secrets | nur die AWS-Credentials (sowieso nötig) | zusätzlich privater SSH-Key als Secret |
| Audit | Befehl + Output in SSM-History/CloudTrail | nur Logs auf der Instanz |
| Nachteil | SSM-Agent + passende Rolle nötig; asynchron (Polling) | einfacher zu debuggen, direkte Ausgabe |

### Outputs

| Output | Zweck | Vertraulich? |
|---|---|---|
| `frontend_url` | URL zum Spielen | nein (öffentlich) |
| `console_url`, `version_url` | Web-Konsole, Versionsabfrage | nein |
| `instance_id`, `public_ip` | Ziel fürs Deployment | nein (ohne Credentials nutzlos), aber nicht unnötig streuen |
| `artifacts_bucket`, `frontend_bucket` | Ziel fürs Deployment | nein |
| `github_variables` | fertiger Block für GitHub Variables | nein |

Vertraulich sind ausschliesslich die **AWS-Credentials** – sie sind kein Output, sondern kommen aus dem Learner Lab.
Ebenfalls vertraulich: die **State-Datei** (`terraform.tfstate`), sie bleibt lokal und ist in `.gitignore`.

**Was nie in den öffentlichen Frontend-Bucket darf:** Credentials, Tokens, `tfstate`, Jars/interne Artefakte,
Logs, Konfiguration mit internen Adressen. Alles im Bucket ist für jeden lesbar.

---

## 3. Manuelles Deployment (Auftrag 2, Teil 3)

Diese Befehle sind die Vorlage für die Pipeline. Voraussetzung: Learner Lab gestartet, Credentials aus
*AWS Details → AWS CLI* in `~/.aws/credentials` (oder Datei via `AWS_SHARED_CREDENTIALS_FILE`).

### 3.1 Infrastruktur

```bash
cd iac
tofu init
tofu apply                 # einmalig bzw. nur bei Infrastruktur-Änderungen
tofu output                # URLs, instance_id, Bucket-Namen
tofu output -raw github_variables
```

```bash
# Variablen für die folgenden Schritte
export AWS_REGION=$(tofu output -raw aws_region)
export INSTANCE_ID=$(tofu output -raw instance_id)
export ARTIFACTS_BUCKET=$(tofu output -raw artifacts_bucket)
export FRONTEND_BUCKET=$(tofu output -raw frontend_bucket)
export VERSION_URL=$(tofu output -raw version_url)
export FRONTEND_URL=$(tofu output -raw frontend_url)
```

### 3.2 Release A deployen (z. B. `v1.2.0`)

```bash
TAG=v1.2.0; V=${TAG#v}

# Assets aus dem GitHub-Release holen (nicht lokal bauen) und prüfen
gh release download $TAG -p "tictactest-$V.jar" -p "tictactest-frontend-$V.zip" -p checksums.txt
sha256sum -c checksums.txt

# Backend: Jar in privaten Bucket, dann auf der Instanz aktivieren
aws s3 cp tictactest-$V.jar s3://$ARTIFACTS_BUCKET/backend/$V/tictactest-$V.jar
CMD_ID=$(aws ssm send-command --instance-ids $INSTANCE_ID --document-name AWS-RunShellScript \
  --parameters "commands=[\"/usr/local/bin/tictactest-deploy $V\"]" --query Command.CommandId --output text)
aws ssm wait command-executed --command-id $CMD_ID --instance-id $INSTANCE_ID
aws ssm get-command-invocation --command-id $CMD_ID --instance-id $INSTANCE_ID --query StandardOutputContent --output text
curl $VERSION_URL                                   # -> 1.2.0

# Frontend: Archiv entpacken, Bucket synchronisieren, config.js NICHT anfassen
mkdir site && unzip tictactest-frontend-$V.zip -d site && rm site/config.js
aws s3 sync site/ s3://$FRONTEND_BUCKET/ --delete --exclude config.js --cache-control no-cache
curl $FRONTEND_URL/version.txt                      # -> 1.2.0
```

Was `tictactest-deploy` auf der Instanz tut ([Quelle](../iac/files/tictactest-deploy.sh)):

1. Jar aus `s3://<artifacts>/backend/<v>/` in ein Temp-Verzeichnis laden (Instance Profile)
2. **Smoke-Test**: eine ganze Partie mit Eingaben `0…8` spielen, Ausgabe muss `winner` oder `draw` enthalten
3. erst dann: Jar nach `/opt/tictactest/releases/<v>/`, `TICTACTEST_VERSION=<v>` nach `/etc/tictactest/app.env`
4. `systemctl restart tictactest` → `ExecStartPre` verlinkt `current` und schreibt `version.txt`

### 3.3 Release B deployen – ohne `tofu apply`

Gleiche Befehle mit `TAG=v1.3.0`. Danach:

```bash
cd iac && tofu plan        # -> "No changes. Your infrastructure matches the configuration."
```

OpenTofu sieht keine Änderung, weil die Version nirgends in der Infrastruktur steht (nur in `app.env` auf der
Instanz und in den Bucket-Objekten, die OpenTofu nicht verwaltet – ausser `config.js`, die unverändert bleibt).

### Durchgeführte Deployments (25.11.2026)

| Schritt | Ergebnis |
|---|---|
| `tofu apply` | 13 Ressourcen. Outputs: EIP `100.51.150.108`, Instanz, Buckets, URLs. `/version.txt` → `not-deployed` (keine Version in der Infrastruktur). SSM-Agent sofort `Online`. |
| Release A manuell: Backend **v1.1.0** (Jar aus GitHub-Release, `sha256sum -c` ok) | 1. Versuch: **Smoke-Test schlug fehl** (`Unable to access jarfile`): `mktemp -d` erzeugt ein Verzeichnis mit Rechten 0700 (root), der Smoke-Test läuft aber als User `tictactest`. Alte Version blieb aktiv (= Schutzmechanismus funktioniert). Fix: `chmod 0755` im Deploy-Skript. |
| Setup-Änderung (Fix im Deploy-Skript = `user_data` geändert) | `tofu plan`: `aws_instance … will be updated in-place` – cloud-init würde aber nicht erneut laufen. Daher bewusst `tofu apply -replace=aws_instance.tictactest_server`: neue Instanz-ID, **EIP bleibt** (nur Re-Association), `config.js` unverändert. |
| Release A erneut | SSM `Success`, Smoke-Test ok, `/version.txt` → `1.1.0`. Frontend manuell (`s3 sync --delete --exclude config.js`) → `version.txt` `1.1.0`, `config.js` unverändert. Im Browser gespielt: Züge, ungültige Eingabe (`x` → *Invalid input*), beide Versionen angezeigt. |
| Release B | über die Pipeline (Abschnitt 7) – ohne `tofu apply`; `tofu plan` danach: *No changes*. |

---

## 4. Robustheit (Auftrag 2, Teil 4)

| Experiment | Ergebnis |
|---|---|
| **Learner-Lab-Session beenden und neu starten** (getestet mit `aws ec2 stop-instances` + `start-instances`) | **Ergebnis:** gleiche IP `100.51.150.108`, `/version.txt` → `1.1.0` ca. 30 s nach dem Start, ohne Eingriff. Instanz wird gestoppt/gestartet. Die **Elastic IP bleibt** (URL gleich, `config.js` stimmt weiter). systemd startet nginx + `tictactest` automatisch, `app.env` liegt auf dem EBS-Volume → **dieselbe Version** läuft, ohne Eingriff. |
| **Setup ändern** (z. B. neue Variable in `infra.env`, `max_clients` 8→6) | `tofu plan` zeigt `~ update in-place` von `user_data` auf `aws_instance`. Beim Apply wird die Instanz **gestoppt und gestartet** (gleiche Instanz-ID, EIP bleibt, Version bleibt), aber **cloud-init läuft nicht erneut** → die Änderung kommt nicht an! Mit `user_data_replace_on_change = true` würde die Instanz ersetzt. Bewusster Weg: `tofu apply -replace=aws_instance.tictactest_server` und danach die aktuelle Version per `Deploy`-Workflow erneut deployen. |
| **`tofu destroy` + `tofu apply`** | Neu: Instanz-ID, Elastic IP/DNS, beide Bucket-Namen (`bucket_prefix`), damit Frontend-URL, Konsolen-URL, `config.js`. Die Instanz startet **ohne Version** (`version.txt` = `not-deployed`), die Buckets sind leer. Hinweis: Die CloudFormation-Buckets lassen sich nur leer löschen → vor `tofu destroy` `aws s3 rm s3://<bucket> --recursive` für beide Buckets. Nötig: GitHub Variables (`INSTANCE_ID`, `ARTIFACTS_BUCKET`, `FRONTEND_BUCKET`, `VERSION_URL`, `FRONTEND_URL`) aus `tofu output -raw github_variables` aktualisieren, README-Link anpassen, dann **Deploy**-Workflow mit dem aktuellen Tag (Backend + Frontend) starten. |

---

## 5. Secrets-Konzept (Auftrag 3, Teil 1)

### Welche Zugangsdaten braucht die Pipeline?

| Wert | Art | Ablage | Herkunft / Gültigkeit |
|---|---|---|---|
| `AWS_ACCESS_KEY_ID` | **Secret** | GitHub *Secrets* (Repository) | Learner Lab → AWS Details → AWS CLI; gilt nur für die laufende Session (max. 4 h) |
| `AWS_SECRET_ACCESS_KEY` | **Secret** | GitHub *Secrets* | dito |
| `AWS_SESSION_TOKEN` | **Secret** | GitHub *Secrets* | dito – ohne Token sind die temporären Keys ungültig |
| `GITHUB_TOKEN` | Secret (automatisch) | von GitHub pro Lauf erzeugt | nur während des Laufs gültig, `permissions: contents: read` für `gh release download` |
| `AWS_REGION`, `INSTANCE_ID`, `ARTIFACTS_BUCKET`, `FRONTEND_BUCKET`, `FRONTEND_URL`, `VERSION_URL` | **Variable** | GitHub *Variables* | aus `tofu output`; nicht geheim, ändern sich nur nach `destroy/apply` |

**Secrets vs. Variables:** Secrets werden verschlüsselt gespeichert, sind nach dem Speichern nicht mehr lesbar
und werden in Logs automatisch als `***` maskiert. Variables sind Klartext-Konfiguration, im UI und im Log
sichtbar. Faustregel: alles, womit man *handeln* kann (Credentials, Tokens) → Secret; alles, was nur *beschreibt*
wohin (IDs, URLs, Namen) → Variable. So bleiben Logs lesbar und echte Geheimnisse geschützt.

Die Instanz selbst braucht **keine** Secrets: sie nutzt das Instance Profile (temporäre, automatisch rotierte Credentials).

### Abgelaufene Learner-Lab-Credentials

- Die Lab-Credentials laufen mit jeder Session ab. **Vor jedem Release** (bzw. zu Beginn jeder Lab-Session):
  Lab starten → *AWS Details → AWS CLI: Show* → die drei Werte unter *Settings → Secrets and variables → Actions*
  ersetzen. Die Instanz muss laufen (Lab gestartet), sonst kann SSM nichts ausführen.
- **Release mit abgelaufenen Credentials**: Der Release (Jar/Zip) wird trotzdem erstellt. Der Job
  *Preflight* prüft mit `aws sts get-caller-identity` und **schlägt sofort fehl**, mit Hinweis in der Job-Summary.
  Es wird nichts halb deployt.
- **Erkennen**: roter Workflow *Release → Deploy / Preflight*, Fehlermeldung `ExpiredToken`/`InvalidClientTokenId`, GitHub-Mail.
- **Nachdeployen**: Secrets aktualisieren → *Actions → Deploy → Run workflow* mit dem Tag (Backend und/oder Frontend
  anhaken, entsprechend der Summary des fehlgeschlagenen Laufs) – oder den fehlgeschlagenen Lauf *Re-run failed jobs*
  (Re-runs lesen die Secrets neu).
- Getestet: Release mit Credentials einer beendeten Session → Preflight rot, danach Secrets erneuert und per Deploy-Workflow nachdeployt.

### Wer sieht/benutzt die Secrets?

- **Sehen**: niemand – auch Admins können Secrets nur überschreiben, nicht auslesen.
- **Benutzen**: jeder Workflow-Lauf im Repository, also jeder mit **Schreibrechten** (kann einen Workflow ändern und Secrets z. B. an einen externen Server schicken). Darum: wenige Collaborators, Branch Protection.
- **Logs**: GitHub maskiert bekannte Secret-Werte. Trotzdem: nie `set -x` mit Secrets, nie `env` ausgeben, nie Secrets in Dateien/Artefakte schreiben. `configure-aws-credentials` setzt sie als Umgebungsvariablen und maskiert sie. Nach jedem Lauf Logs kontrolliert: keine Credentials sichtbar.
- **Forks / Pull Requests aus Forks**: Workflows bekommen **keine** Secrets (ausser `GITHUB_TOKEN` read-only). Deshalb unser Deployment nur bei Tags/manuell, nie bei `pull_request_target`.
- `deploy.yml` übernimmt Eingaben (`tag`) nur über Umgebungsvariablen + Regex-Prüfung → keine Script-Injection über `${{ }}`.

### Schaden bei Leak und Reaktion

| Secret | Schaden | Reaktion |
|---|---|---|
| AWS-Lab-Credentials | Wer sie hat, kann im Lab-Konto alles tun, was LabRole darf: Instanzen starten (Budget von 50 $ verbrauchen, Mining), Buckets lesen/löschen, unsere App manipulieren. Begrenzt durch Ablauf nach Session-Ende. | Sofort **End Lab** (Credentials werden ungültig) bzw. *Reset*; Secret in GitHub ersetzen; Commit/Log bereinigen (Log löschen, bei Commit History umschreiben – geleakt ist geleakt); prüfen, was in der Zeit passiert ist (CloudTrail, laufende Ressourcen). |
| `GITHUB_TOKEN` | nur während des Laufs gültig, read-only | – |
| In echten Firmen: langlebige Access Keys | unbegrenzter Zugriff bis Rotation | Key deaktivieren/rotieren, CloudTrail auswerten, Incident-Prozess |

### Wie würde man das in einem Unternehmen lösen? → OIDC

GitHub Actions kann pro Lauf ein **OIDC-ID-Token** ausstellen (`permissions: id-token: write`). In AWS wird
GitHub als *Identity Provider* eingetragen und eine IAM-Rolle mit Trust-Policy angelegt, die nur Tokens mit
z. B. `sub: repo:<org>/<repo>:environment:production` akzeptiert. `aws-actions/configure-aws-credentials` mit
`role-to-assume` tauscht das Token gegen temporäre Credentials (`sts:AssumeRoleWithWebIdentity`, ~1 h gültig).

Vorteile: **keine gespeicherten Secrets** (nichts kann leaken oder ablaufen), Rechte minimal pro Rolle,
an Repo/Branch/Environment gebunden, vollständig in CloudTrail nachvollziehbar.
Im Learner Lab nicht möglich, weil wir weder IAM-Rollen noch Identity Provider anlegen dürfen.

---

## 6. Automatisches Deployment (Auftrag 3, Teile 2–5)

Workflows:

- [`release.yml`](../.github/workflows/release.yml): Tag `v*` → Tests, Jar, Frontend-Zip, `checksums.txt`, GitHub-Release → **changes** → **deploy**
- [`deploy.yml`](../.github/workflows/deploy.yml): wiederverwendbar (`workflow_call`) **und** manuell (`workflow_dispatch`)
- [`scripts/detect-changes.sh`](../scripts/detect-changes.sh): Änderungserkennung
- `ci.yml` läuft bei jedem Push, **deployt aber nie**.

```
git tag v1.3.0 && git push origin v1.3.0
        │
        ▼
 release.yml ── release (Tests, Build, GitHub-Release mit Assets)
        │
        ├── changes:  git diff --name-only <vorheriger v*-Tag>..v1.3.0
        │             backend=true|false   frontend=true|false
        ▼
 deploy.yml (concurrency: deploy-production)
   preflight:  Tag gültig? Release existiert? AWS-Credentials gültig?
   backend  (if backend):   gh release download jar → sha256 → s3 cp → ssm send-command tictactest-deploy
                            → curl /version.txt == 1.3.0 (max. 2 min, sonst rot)
   frontend (if frontend):  gh release download zip → sha256 → s3 sync --delete --exclude config.js
                            → curl <frontend>/version.txt == 1.3.0
```

### Teil 2 – Änderungen erkennen

| Pfad | gehört zu | Begründung |
|---|---|---|
| `src/main/**`, `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/**`, `gradlew*` | **Backend** | beeinflusst den Inhalt des Jars |
| `frontend/**` | **Frontend** | ist der Inhalt des Zips |
| `src/test/**`, `*.md`, `docs/**` | nichts | ändert kein ausgeliefertes Artefakt |
| `iac/**` | nichts (Infrastruktur) | wird mit `tofu apply` ausgerollt, nicht vom Deployment |
| `.github/workflows/**`, `scripts/**` | nichts | Pipeline selbst; Änderungen wirken ab dem nächsten Lauf. Will man nach einer Pipeline-Änderung bewusst neu ausrollen → manueller Deploy. |

- **Vergleich mit dem vorherigen Tag, nicht dem letzten Commit**: zwischen zwei Releases liegen meist viele Commits. Der letzte Commit vor dem Tag kann z. B. nur Doku sein, obwohl drei Commits vorher das Frontend geändert wurde – diese Änderung wäre nie deployt worden. Massgeblich ist, was *seit dem zuletzt ausgelieferten Stand* anders ist.
- Vorheriger Tag: `git describe --tags --abbrev=0 --match 'v*' "<tag>^"` (Tags erreichbar vom Vorgänger-Commit), braucht `fetch-depth: 0`.
- **Erster Release** (kein vorheriger Tag): alles deployen.
- **Warum nicht `on.push.paths`?** Der `paths`-Filter vergleicht nur die Commits *dieses Pushes* und wird bei Tag-Pushes **ignoriert** (gilt nur für Branches). Er kann auch nicht beide Teile unterscheiden, sondern nur den ganzen Workflow starten oder nicht.

### Teil 3 – Backend

- Jar aus dem Release → privater Bucket → `tictactest-deploy` per SSM → Dienst-Neustart. Instanz bleibt bestehen.
- **Prüfung**: Smoke-Test auf der Instanz (kaputtes Jar wird gar nicht aktiviert) **und** danach Polling von `/version.txt` bis exakt die neue Version gemeldet wird; Timeout 2 min → Workflow rot.
- **Concurrency**: `concurrency: group: deploy-production, cancel-in-progress: false` – ein zweites Deployment wartet.
- **Laufende Partien beim Neustart**: `systemctl restart` beendet ttyd und alle Java-Kindprozesse → die Partie bricht ab, der Browser zeigt *Reconnect*. Vermeiden könnte man das mit *Connection Draining*: neue Version parallel auf zweitem Port starten, nginx per `reload` auf neue Version umschalten (bestehende WebSockets laufen weiter), alte Instanz erst beenden, wenn keine Verbindung mehr offen ist (oder nach Timeout). Oder Blue/Green mit zwei Instanzen hinter einem Load Balancer.
- **Version nach Neustart**: steht in `/etc/tictactest/app.env` auf dem EBS-Volume; `ExecStartPre` holt das passende Jar.

### Teil 4 – Frontend

- `aws s3 sync site/ s3://<bucket>/ --delete --exclude config.js`: ersetzt alle Dateien, **entfernt nicht mehr vorhandene**, lässt `config.js` in Ruhe (`--exclude` gilt auch für `--delete`). Zusätzlich wird `config.js` aus dem entpackten Archiv gelöscht.
- Prüfung: `<frontend>/version.txt` muss die neue Version liefern; zusätzlich muss `config.js` noch im Bucket existieren.
- **Wenn `config.js` gelöscht würde**: Das Frontend kennt die Adresse der Web-Konsole nicht mehr → leeres Terminal mit Fehlermeldung, Backend-Version „unbekannt“. Würde stattdessen die lokale `config.js` hochgeladen, zeigt die Seite auf `localhost` → bei jedem Besucher kaputt. Reparatur: `tofu apply` (schreibt `config.js` neu). Schutz: `--exclude`, `rm site/config.js` und die Prüfung im Workflow.

### Teil 5 – gezielt eine Version deployen

*Actions → Deploy → Run workflow*: `tag` (z. B. `v1.2.0`), Backend und Frontend angehakt.
Da nur Release-Assets verwendet werden, ist jede veröffentlichte Version jederzeit wieder deploybar.

| Test | Ergebnis |
|---|---|
| **Rollback** v1.3.x → v1.2.0 | Deploy-Workflow mit `v1.2.0` → Backend und Frontend zeigen `1.2.0` |
| **Kaputter Release** (Jar startet nicht) | Smoke-Test auf der Instanz schlägt fehl → `tictactest-deploy` bricht ab, **alte Version läuft weiter**, SSM-Status `Failed`, Workflow rot. Ausfallzeit: 0. Behebung: Fix-Release oder Deploy einer funktionierenden Version (~1–2 min). |
| **Nach `destroy` + `apply`** | GitHub Variables aus `tofu output -raw github_variables` aktualisieren, Deploy-Workflow mit aktuellem Tag → Backend + Frontend wieder da (Jar wird erneut aus dem Release in den neuen Bucket kopiert). |

---

## 7. Nachweis der Trennung (Auftrag 3, Teil 6)

Workflow-Läufe: <https://github.com/MrCodeBS/450-tictactest-mvk/actions>

| # | Fall | Release / Commit | Backend-Job | Frontend-Job | Ergebnis |
|---|---|---|---|---|---|
| 1 | Erster Release der Web-App (Baseline) | `v1.2.0` | deployt | deployt | Backend 1.2.0, Frontend 1.2.0 |
| 2 | nur Frontend (Farbe) | `v1.3.0` | übersprungen | deployt | Backend 1.2.0, Frontend 1.3.0 |
| 3 | nur Backend (PerfectPlayer in `main()`) | `v1.4.0` | deployt | übersprungen | Backend 1.4.0, Frontend 1.3.0 |
| 4 | beides | `v1.5.0` | deployt | deployt | Backend 1.5.0, Frontend 1.5.0 |
| 5 | Push auf `main` ohne Tag | – | – | – | nur CI läuft, kein Release-/Deploy-Workflow |
| 6 | Rollback (manuell) | Deploy `v1.2.0` | deployt | deployt | Backend 1.2.0, Frontend 1.2.0 |

Links zu den einzelnen Läufen: siehe [Anhang](#anhang-workflow-läufe).

---

## 8. Reflexion und Promotion-Strategie (Auftrag 3, Teil 7)

### Weg einer Änderung

**Backend-Änderung** (z. B. Player in `TicTacToeMain.main()`):

```
Commit src/main/... ─► Push main ─► CI (Tests, Coverage)            [automatisch, deployt nichts]
     ─► git tag v1.4.0 + push                                         [manuell]
     ─► release.yml: Tests ─► Jar + Zip + checksums ─► GitHub-Release [automatisch]
     ─► changes: backend=true, frontend=false                         [automatisch]
     ─► deploy: preflight ─► Jar ─► S3 (privat) ─► SSM tictactest-deploy (Smoke-Test, restart)
     ─► /version.txt == 1.4.0 ✔                                       [automatisch]
```

**Frontend-Änderung** (z. B. Farbe in `frontend/style.css`):

```
Commit frontend/... ─► Push main ─► CI                               [automatisch]
     ─► git tag v1.3.0 + push                                         [manuell]
     ─► release.yml ─► GitHub-Release                                 [automatisch]
     ─► changes: backend=false, frontend=true
     ─► deploy: preflight ─► Zip ─► s3 sync --delete --exclude config.js
     ─► <frontend>/version.txt == 1.3.0 ✔                             [automatisch]
```

**Automatisiert**: Tests, Build, Release, Änderungserkennung, Deployment, Verifikation.
**Noch manuell**: Tag setzen (bewusste Release-Entscheidung), `tofu apply` (lokal, State lokal),
Learner Lab starten und **Secrets pro Session erneuern**, GitHub Variables nach `destroy/apply` nachtragen.

### Mehrere Umgebungen (dev, staging, prod)

Änderungen an der OpenTofu-Konfiguration:

- `var.environment` gibt es bereits und fliesst in alle Namen/Tags ein (`tictactest-dev-…`).
- **Getrennter State pro Umgebung**: Remote Backend (S3 + Locking) mit eigenem Key pro Umgebung, oder `tofu workspace` – oder pro Umgebung ein Verzeichnis `envs/dev|staging|prod` das ein gemeinsames **Modul** (`modules/tictactest`) aufruft.
- Pro Umgebung eine `*.tfvars`-Datei.

| gleich | unterschiedlich |
|---|---|
| Module/Code, AMI-Typ, Java-/ttyd-Version, Dienst-Setup, Ports | Name/Tags, Instanzgrösse (dev t2.micro, prod t3.small), `max_clients`, SSH (dev ja, prod nein), Domain/TLS, ggf. Account/Region, Bucket-Namen, EIP |

In GitHub: pro Umgebung ein **GitHub Environment** (`dev`, `staging`, `production`) mit eigenen Variables
(`INSTANCE_ID`, Buckets, URLs) und eigenen Credentials (bzw. eigener OIDC-Rolle). `deploy.yml` bekommt die
Umgebung als Input (`environment: ${{ inputs.environment }}`) und `concurrency: deploy-${{ inputs.environment }}`.

### Promotion-Strategie

```
 Push main ──► CI ──► automatisch nach dev  (Snapshot-Build, schnelles Feedback)
 Tag v1.4.0 ──► Release (Artefakte, unveränderlich)
            ──► automatisch nach staging ──► E2E-/Smoke-Tests gegen staging
                                         ──► manuelle Freigabe (Required Reviewers)
                                         ──► production (gleiches Artefakt v1.4.0)
```

- **Build once, deploy many**: Es wird **einmal** gebaut (Release), und **dasselbe Artefakt** (gleiche Prüfsumme) wird von Umgebung zu Umgebung weitergereicht. Würde pro Umgebung neu gebaut, könnten sich Abhängigkeiten, Compiler oder Code unterscheiden – in prod liefe etwas, das nie in staging getestet wurde.
- **Auslöser**: Push → dev; Tag/Release → staging; manueller Knopf bzw. Approval im GitHub Environment `production` → prod. Ein erfolgreicher Test in staging gibt den Knopf frei.
- **Prüfung vor der nächsten Umgebung**: Versions-Check und Smoke-Test (haben wir), zusätzlich **End-to-End-Tests** gegen die laufende Umgebung (z. B. Playwright: Seite laden, im Terminal eine Partie spielen, Ergebnis prüfen) und Monitoring ohne Fehler über eine gewisse Zeit.
- **Wer darf nach prod?** GitHub Environment `production` mit **Required Reviewers** (z. B. Lehrperson/Teamlead), optional *Wait timer* und *Deployment branches/tags* nur `v*`. Die prod-Credentials sind nur im Environment hinterlegt und damit nur nach Freigabe nutzbar.
- **Änderungserkennung bei mehreren Umgebungen**: „seit dem letzten Release“ reicht dann nicht mehr, weil Umgebungen unterschiedlich weit hinterherhinken (staging hat v1.5, prod noch v1.3; v1.4 war nur Frontend, v1.5 nur Backend → prod braucht **beides**). Der Vergleich muss gegen die **in der Zielumgebung laufende Version** erfolgen: Versionen über `/version.txt` bzw. `version.txt` im Bucket abfragen (oder in einem Deployment-Record/GitHub Deployments API) und `git diff <deployed-tag>..<neuer-tag>` pro Teil berechnen. Einfacher und robuster ist bei Promotion oft: immer **beide** Teile deployen (idempotent, gleiche Artefakte), und die Änderungserkennung nur als Optimierung nutzen.

---

## Anhang: Workflow-Läufe

_Wird nach den Releases ergänzt._
