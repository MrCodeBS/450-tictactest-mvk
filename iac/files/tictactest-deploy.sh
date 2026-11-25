#!/bin/bash
# Deployt eine Backend-Version auf dieser Instanz (wird per SSM aus der Pipeline aufgerufen).
#   tictactest-deploy <version>       z. B. tictactest-deploy 1.2.0
# Ablauf: Jar aus privatem S3-Bucket holen -> Smoke-Test -> app.env setzen -> Dienst neu starten.
# Ein kaputtes Jar besteht den Smoke-Test nicht und geht nie live (alte Version laeuft weiter).
set -euo pipefail

VERSION="${1:?Usage: tictactest-deploy <version>}"
if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][A-Za-z0-9.-]+)?$ ]]; then
  echo "Ungueltige Version: $VERSION" >&2
  exit 2
fi

# shellcheck disable=SC1091
source /etc/tictactest/infra.env

DIR="/opt/tictactest/releases/$VERSION"
TMP="$(mktemp -d)"
chmod 0755 "$TMP"   # Smoke-Test laeuft als User tictactest
trap 'rm -rf "$TMP"' EXIT

echo "==> Lade Jar $VERSION aus s3://$ARTIFACTS_BUCKET"
aws s3 cp --region "$AWS_REGION" --only-show-errors \
  "s3://$ARTIFACTS_BUCKET/backend/$VERSION/tictactest-$VERSION.jar" "$TMP/tictactest.jar"
chmod 0644 "$TMP/tictactest.jar"

echo "==> Smoke-Test: komplette Partie mit Eingaben 0..8 spielen"
OUT="$(printf '0\n1\n2\n3\n4\n5\n6\n7\n8\n' | timeout 30 runuser -u tictactest -- \
  /usr/bin/java -Xmx32m -jar "$TMP/tictactest.jar" 2>&1 || true)"
if ! grep -qiE 'winner|draw' <<<"$OUT"; then
  echo "Smoke-Test FEHLGESCHLAGEN - Version $VERSION wird nicht aktiviert." >&2
  echo "$OUT" | tail -n 20 >&2
  exit 1
fi
echo "Smoke-Test ok."

mkdir -p "$DIR"
install -m 0644 "$TMP/tictactest.jar" "$DIR/tictactest.jar"
echo "TICTACTEST_VERSION=$VERSION" > /etc/tictactest/app.env

echo "==> Starte Dienst neu"
systemctl restart tictactest
sleep 2
systemctl is-active --quiet tictactest
echo "Version $VERSION aktiv: $(cat /var/www/tictactest/version.txt)"

# Alte Releases aufraeumen (die letzten 5 behalten)
ls -1dt /opt/tictactest/releases/*/ 2>/dev/null | tail -n +6 | xargs -r rm -rf
