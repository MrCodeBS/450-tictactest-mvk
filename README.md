# TicTacTest

Tic-Tac-Toe als Konsolen-Applikation (Modul 450: Testing), als Web-App in der Cloud betrieben (Modul 324: DevOps).

## Web-App

**▶ Spielen: <http://tictactest-dev-buckets-frontend-auahrllgtm0d.s3-website-us-east-1.amazonaws.com>**

| | URL |
|---|---|
| Frontend (S3 Static Website) | <http://tictactest-dev-buckets-frontend-auahrllgtm0d.s3-website-us-east-1.amazonaws.com> |
| Web-Konsole (ttyd on EC2) | <http://ec2-100-51-150-108.compute-1.amazonaws.com/> |
| Backend-Version | <http://ec2-100-51-150-108.compute-1.amazonaws.com/version.txt> |

> Läuft im AWS Learner Lab: erreichbar, solange eine Lab-Session läuft.

## Dokumentation

| Thema | Datei |
|---|---|
| Auftrag 1 – Web-Konsole, Frontend, Release-Assets | [docs/webkonsole.md](docs/webkonsole.md) |
| Auftrag 2 + 3 – Infrastruktur, Deployment, Secrets, Reflexion, Promotion | [docs/deployment.md](docs/deployment.md) |
| IaC-Grundlagen (OpenTofu, EC2) | [IAC.md](IAC.md) |
| Release-Prozess | [RELEASE.md](RELEASE.md), [CHANGELOG.md](CHANGELOG.md) |
| Testkonzept (M450) | [testkonzept.md](testkonzept.md) |

## Aufbau

```
src/                 Spiel (Backend, wird zum Jar)
frontend/            statische Webseite (plain HTML/CSS/JS)
iac/                 OpenTofu: EC2 + EIP + S3 + systemd/nginx/ttyd setup
scripts/             detect-changes.sh (was hat sich seit dem letzten Release geändert?)
.github/workflows/   ci.yml (jeder Push), release.yml (tag v*), deploy.yml (Release / manuell)
```

## Schnellstart

```bash
java -jar tictactest-<version>.jar     # lokal im Terminal spielen
git tag v1.2.0 && git push origin v1.2.0   # Release + automatisches Deployment der geänderten Teile
```
