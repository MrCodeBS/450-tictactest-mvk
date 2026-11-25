#!/bin/bash
# ExecStartPre (als root): stellt sicher, dass das Jar der in app.env eingetragenen
# Version lokal vorhanden ist, und schreibt die Versionsabfrage (version.txt).
# Damit laeuft nach jedem Neustart der Instanz automatisch dieselbe Version wie vorher.
set -euo pipefail

VERSION="${TICTACTEST_VERSION:-}"
WWW=/var/www/tictactest

if [ -z "$VERSION" ]; then
  echo "not-deployed" > "$WWW/version.txt"
  echo "Noch keine Version deployt."
  exit 0
fi

DIR="/opt/tictactest/releases/$VERSION"
if [ ! -f "$DIR/tictactest.jar" ]; then
  echo "Jar $VERSION fehlt lokal, lade aus s3://$ARTIFACTS_BUCKET ..."
  mkdir -p "$DIR"
  aws s3 cp --region "$AWS_REGION" --only-show-errors \
    "s3://$ARTIFACTS_BUCKET/backend/$VERSION/tictactest-$VERSION.jar" "$DIR/tictactest.jar"
fi
ln -sfn "$DIR" /opt/tictactest/current
echo "$VERSION" > "$WWW/version.txt"
