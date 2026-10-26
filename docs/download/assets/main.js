(function () {
  "use strict";

  var EDITION = document.documentElement.getAttribute("data-edition") || "open";

  var CONFIG = {
    official: {
      appName: "森森课表",
      version: "1.0.1",
      apkUrl: "https://saas.hieu.ren/sensen-timetable/download/sensen_timetable_V1.0.1.apk",
      minAndroid: "Android 7.0+",
    },
    open: {
      appName: "森森课表开源版",
      version: "1.0.0",
      githubRepo: "https://github.com/Brick-web/sensen-timetable",
      releasesUrl: "https://github.com/Brick-web/sensen-timetable/releases/latest",
      minAndroid: "Android 7.0+",
    },
  };

  var cfg = CONFIG[EDITION] || CONFIG.open;

  function qs(sel) {
    return document.querySelector(sel);
  }

  function setText(sel, text) {
    var el = qs(sel);
    if (el && text != null) el.textContent = text;
  }

  function wirePrimaryDownload(url, label) {
    var btn = qs("[data-download-primary]");
    if (!btn || !url) return;
    btn.href = url;
    btn.removeAttribute("download");
    if (label) {
      var span = btn.querySelector("[data-btn-label]");
      if (span) span.textContent = label;
    }
  }

  function fetchLatestOpenRelease() {
    var api =
      "https://api.github.com/repos/Brick-web/sensen-timetable/releases/latest";
    return fetch(api, { headers: { Accept: "application/vnd.github+json" } })
      .then(function (res) {
        if (!res.ok) throw new Error("release");
        return res.json();
      })
      .then(function (data) {
        var tag = (data.tag_name || "").replace(/^v/, "") || cfg.version;
        var apk =
          (data.assets || []).find(function (a) {
            return /\.apk$/i.test(a.name || "");
          }) || null;
        return {
          version: tag,
          apkUrl: apk ? apk.browser_download_url : cfg.releasesUrl,
          apkName: apk ? apk.name : null,
        };
      });
  }

  function initOfficial() {
    setText("[data-version]", "v" + cfg.version);
    wirePrimaryDownload(cfg.apkUrl);
    var meta = qs("[data-download-meta]");
    if (meta) {
      meta.innerHTML =
        "<strong>v" + cfg.version + "</strong> · Android 7.0 及以上 · 免费";
    }
  }

  function initOpen() {
    setText("[data-version]", "v" + cfg.version);
    wirePrimaryDownload(cfg.releasesUrl);

    fetchLatestOpenRelease()
      .then(function (info) {
        setText("[data-version]", "v" + info.version);
        if (info.apkUrl && info.apkName) {
          wirePrimaryDownload(info.apkUrl, "下载 v" + info.version);
        }
        var meta = qs("[data-download-meta]");
        if (meta) {
          meta.innerHTML =
            "<strong>v" + info.version + "</strong> · Android 7.0 及以上 · MIT 协议";
        }
      })
      .catch(function () {});
  }

  document.addEventListener("DOMContentLoaded", function () {
    if (EDITION === "official") initOfficial();
    else initOpen();
  });
})();
