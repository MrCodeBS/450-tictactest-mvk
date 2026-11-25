#!/usr/bin/env bash
# =============================================================================
# setup-webapp.sh - TicTacTest als Web-App in ein anderes Repo uebernehmen
#
# Kopiert Frontend, OpenTofu-Infrastruktur, Workflows und Hilfsskripte in ein
# Ziel-Repo, erstellt die Infrastruktur im AWS Learner Lab und gibt die
# GitHub-Secrets/-Variables aus (oder setzt sie per gh CLI).
#
# Usage:
#   scripts/setup-webapp.sh copy  <ziel-repo>     # Dateien ins Ziel-Repo kopieren
#   scripts/setup-webapp.sh infra <ziel-repo>     # tofu init + apply im Ziel-Repo
#   scripts/setup-webapp.sh github <ziel-repo>    # GitHub Variables/Secrets setzen (gh CLI)
#   scripts/setup-webapp.sh all   <ziel-repo>     # alles nacheinander
#
# Voraussetzungen:
#   - git, tofu (OpenTofu >= 1.6), aws CLI v2, optional gh CLI (eingeloggt)
#   - Learner Lab gestartet, Credentials aus "AWS Details -> AWS CLI: Show"
#     in ~/.aws/credentials (oder AWS_SHARED_CREDENTIALS_FILE gesetzt)
#   - Ziel-Repo baut ein Jar build/libs/tictactest-<version>.jar
#     (build.gradle: archiveBaseName = 'tictactest', Version via -PprojVersion)
# =============================================================================
set -euo pipefail

SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CMD="${1:-}"
DEST="${2:-}"

die()  { echo "FEHLER: $*" >&2; exit 1; }
info() { echo -e "\n==> $*"; }

usage() { sed -n '2,22p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 1; }

[ -n "$CMD" ] && [ -n "$DEST" ] || usage
[ -d "$DEST/.git" ] || die "$DEST ist kein Git-Repository"
DEST="$(cd "$DEST" && pwd)"

need() { command -v "$1" >/dev/null 2>&1 || die "'$1' nicht gefunden - bitte installieren"; }

# -----------------------------------------------------------------------------
copy_files() {
  info "Kopiere Web-App-Dateien nach $DEST"

  # Bestehende Dateien sichern statt ueberschreiben
  local backup="$DEST/.webapp-backup-$(date +%Y%m%d-%H%M%S)"
  for f in .github/workflows/release.yml .github/workflows/deploy.yml iac frontend scripts/detect-changes.sh; do
    if [ -e "$DEST/$f" ]; then
      mkdir -p "$backup/$(dirname "$f")"
      cp -r "$DEST/$f" "$backup/$f"
      echo "  gesichert: $f -> ${backup#$DEST/}/$f"
    fi
  done

  mkdir -p "$DEST/.github/workflows" "$DEST/scripts" "$DEST/docs" "$DEST/iac"
  cp -r "$SRC/frontend" "$DEST/"
  # iac ohne lokalen State, Provider-Cache oder tfvars
  (cd "$SRC/iac" && find . -type f \
      ! -path './.terraform/*' ! -name '*.tfstate*' ! -name '*.tfvars' ! -name 'tfplan' \
      -exec cp --parents {} "$DEST/iac/" \;)
  cp "$SRC/.github/workflows/release.yml" "$SRC/.github/workflows/deploy.yml" "$DEST/.github/workflows/"
  cp "$SRC/scripts/detect-changes.sh" "$DEST/scripts/"
  chmod +x "$DEST/scripts/detect-changes.sh"

  # Doku als Vorlage (Freund passt Texte, Links und Ergebnisse selbst an)
  for d in webkonsole.md deployment.md; do
    [ -e "$DEST/docs/$d" ] || cp "$SRC/docs/$d" "$DEST/docs/$d"
  done

  # .gitignore: State, Plans und tfvars nie committen
  local gi="$DEST/.gitignore"
  touch "$gi"
  for p in '.webapp-backup-*/' '**/.terraform/*' '*.tfstate' '*.tfstate.*' '*.tfvars' '!*.tfvars.example' 'tfplan' '*.pem'; do
    grep -qxF "$p" "$gi" || echo "$p" >> "$gi"
  done

  # Repo-Namen in der Doku ersetzen
  local remote
  remote="$(git -C "$DEST" remote get-url origin 2>/dev/null | sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$##')"
  if [ -n "$remote" ]; then
    sed -i "s#MrCodeBS/450-tictactest-mvk#$remote#g" "$DEST/docs/deployment.md" "$DEST/docs/webkonsole.md" 2>/dev/null || true
  fi

  echo "  fertig. Pruefen mit: git -C \"$DEST\" status"
  echo "  WICHTIG: Abschnitt 3/4/7 in docs/deployment.md beschreiben UNSERE Testlaeufe -"
  echo "           mit euren eigenen Ergebnissen und Links ersetzen."
}

# -----------------------------------------------------------------------------
create_infra() {
  need tofu; need aws
  info "Pruefe AWS-Credentials"
  aws sts get-caller-identity --query Arn --output text \
    || die "AWS-Credentials ungueltig/abgelaufen - Learner Lab starten und ~/.aws/credentials aktualisieren"

  info "tofu init + apply in $DEST/iac"
  cd "$DEST/iac"
  tofu init -input=false
  tofu apply -input=false   # fragt nach Bestaetigung ("yes")

  info "Warte, bis die Instanz eingerichtet ist (cloud-init, ca. 2-3 min)"
  local url
  url="$(tofu output -raw version_url)"
  for _ in $(seq 1 40); do
    if curl -fs -m 5 "$url" >/dev/null; then
      echo "  Instanz bereit: $(curl -fs "$url")"
      break
    fi
    sleep 10
  done

  info "Outputs"
  tofu output
}

# -----------------------------------------------------------------------------
setup_github() {
  cd "$DEST/iac"
  local vars
  vars="$(tofu output -raw github_variables)" || die "Keine Outputs - zuerst 'infra' ausfuehren"

  if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
    info "Setze GitHub Variables per gh CLI"
    while IFS='=' read -r k v; do
      [ -n "$k" ] || continue
      (cd "$DEST" && gh variable set "$k" --body "$v")
      echo "  $k gesetzt"
    done <<<"$vars"

    info "Setze GitHub Secrets aus der AWS-Credentials-Datei (Werte werden nicht angezeigt)"
    local credfile="${AWS_SHARED_CREDENTIALS_FILE:-$HOME/.aws/credentials}"
    for k in aws_access_key_id aws_secret_access_key aws_session_token; do
      local val
      val="$(sed -n "/^\[default\]/,/^\[/{s/^$k[[:space:]]*=[[:space:]]*//p}" "$credfile" | head -n1 | tr -d '\r')"
      [ -n "$val" ] || die "$k nicht in $credfile gefunden"
      (cd "$DEST" && printf '%s' "$val" | gh secret set "$(echo "$k" | tr '[:lower:]' '[:upper:]')")
      echo "  $(echo "$k" | tr '[:lower:]' '[:upper:]') gesetzt"
    done
    echo
    echo "  Hinweis: Die Secrets laufen mit jeder Learner-Lab-Session ab."
    echo "  Neu setzen mit: $0 github \"$DEST\""
  else
    info "gh CLI nicht vorhanden/eingeloggt - bitte von Hand eintragen:"
    echo "  GitHub -> Repo -> Settings -> Secrets and variables -> Actions"
    echo
    echo "  Tab 'Variables' (New repository variable):"
    echo "$vars" | sed 's/^/    /'
    echo
    echo "  Tab 'Secrets' (New repository secret) - Werte aus Learner Lab 'AWS Details',"
    echo "  jeweils NUR der Wert nach dem '=':"
    echo "    AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN"
  fi

  info "Naechste Schritte"
  cat <<EOF
  1. Aenderungen committen und pushen:
       git -C "$DEST" add -A && git -C "$DEST" commit -m "feat: Web-App" && git -C "$DEST" push
  2. Release erstellen (deployt automatisch):
       git -C "$DEST" tag v1.0.0 && git -C "$DEST" push origin v1.0.0
  3. Spielen: $(tofu output -raw frontend_url)
EOF
}

# -----------------------------------------------------------------------------
case "$CMD" in
  copy)   copy_files ;;
  infra)  create_infra ;;
  github) setup_github ;;
  all)    copy_files; create_infra; setup_github ;;
  *)      usage ;;
esac
