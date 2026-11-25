// TicTacTest Frontend – plain JavaScript, kein Framework, kein Build-Step.
// Die URLs kommen aus config.js (window.TICTACTEST_CONFIG), nie aus diesem Code.
(function () {
  "use strict";

  var cfg = window.TICTACTEST_CONFIG || {};
  var frame = document.getElementById("console");

  function startGame() {
    if (!cfg.consoleUrl) {
      frame.srcdoc = "<p style='color:#f87171;font-family:sans-serif'>config.js fehlt oder enthaelt keine consoleUrl.</p>";
      return;
    }
    // Neu laden = neue WebSocket-Verbindung = ttyd startet einen neuen Java-Prozess (neue Partie)
    frame.src = "about:blank";
    setTimeout(function () { frame.src = cfg.consoleUrl; }, 50);
  }

  function showText(id, url) {
    var el = document.getElementById(id);
    if (!url) { el.textContent = "unbekannt"; return; }
    fetch(url, { cache: "no-store" })
      .then(function (r) { if (!r.ok) { throw new Error(r.status); } return r.text(); })
      .then(function (t) { el.textContent = t.trim(); })
      .catch(function (e) { el.textContent = "nicht erreichbar (" + e.message + ")"; });
  }

  document.getElementById("restart").addEventListener("click", startGame);

  startGame();
  showText("backend-version", cfg.versionUrl);
  showText("frontend-version", "version.txt");
})();
