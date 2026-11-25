#!/usr/bin/env bash
# Ermittelt, welche Teile sich seit dem vorherigen Release-Tag geaendert haben.
#   scripts/detect-changes.sh v1.3.0
# Ausgabe (GitHub-Output-Format):
#   backend=true|false
#   frontend=true|false
# Verglichen wird mit dem vorherigen Tag v*, nicht mit dem letzten Commit:
# zwischen zwei Releases koennen beliebig viele Commits liegen.
set -euo pipefail

TAG="${1:?Usage: detect-changes.sh <tag>}"

# Pfade, die das Jar beeinflussen
BACKEND_PATHS=(src/main build.gradle settings.gradle gradle.properties gradle gradlew gradlew.bat)
# Pfade, die das Frontend-Archiv beeinflussen
FRONTEND_PATHS=(frontend)

PREV="$(git describe --tags --abbrev=0 --match 'v*' "${TAG}^" 2>/dev/null || true)"

if [ -z "$PREV" ]; then
  echo "Kein vorheriger Release-Tag -> erster Release, alles deployen." >&2
  echo "backend=true"
  echo "frontend=true"
  exit 0
fi

echo "Vergleiche $PREV..$TAG" >&2
git diff --name-only "$PREV" "$TAG" >&2 || true

changed() {
  if git diff --quiet "$PREV" "$TAG" -- "$@"; then echo false; else echo true; fi
}

echo "backend=$(changed "${BACKEND_PATHS[@]}")"
echo "frontend=$(changed "${FRONTEND_PATHS[@]}")"
