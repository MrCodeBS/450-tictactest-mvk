// Lokale Default-Konfiguration (Entwicklung).
// In der Cloud wird diese Datei NICHT aus dem Release ausgeliefert:
// OpenTofu schreibt eine eigene config.js in den Frontend-Bucket, und das
// Frontend-Deployment schliesst config.js beim Sync aus (--exclude config.js).
window.TICTACTEST_CONFIG = {
  consoleUrl: "http://localhost:8080/",
  versionUrl: "http://localhost:8080/version.txt"
};
