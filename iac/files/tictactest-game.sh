#!/bin/bash
# Wird von ttyd pro Browser-Verbindung gestartet: genau ein Java-Prozess pro Partie.
# Kein interaktiver Shell-Zugang: exec ersetzt dieses Skript sofort durch die JVM.
# Kleine Heap-/JIT-Einstellungen, damit mehrere Partien auf 1 GB RAM Platz haben.
exec /usr/bin/java -Xmx32m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 \
  -jar /opt/tictactest/current/tictactest.jar
