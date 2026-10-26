(function () {
  "use strict";

  var root = document.documentElement;
  if (!root.classList.contains("intro-active")) return;

  var TEXT = "一张课表，就刚刚好";
  var START_DELAY = 500;
  var CHAR_INTERVAL = 150;
  var HOLD_AFTER_TYPED = 750;
  var LOCK_HOLD = 2200;
  var LOCK_GAP = 500;
  var OPEN_DURATION = 2200;

  var openAt = Date.parse(root.getAttribute("data-open-at") || "");
  var timers = [];
  var finished = false;
  var reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  function locked() {
    return !isNaN(openAt) && Date.now() < openAt;
  }

  function later(fn, ms) {
    timers.push(setTimeout(fn, ms));
  }

  function finish() {
    if (finished) return;
    finished = true;
    timers.forEach(clearTimeout);
    var page = document.querySelector(".page-root");
    if (page) page.inert = false;
    root.classList.remove("intro-active", "intro-opening", "intro-locked", "intro-clear");
    var intro = document.querySelector(".intro");
    if (intro) intro.remove();
    window.removeEventListener("keydown", skip);
    window.removeEventListener("pointerdown", skip);
    window.removeEventListener("wheel", skip);
  }

  function open() {
    if (finished || locked()) return;
    window.scrollTo(0, 0);
    root.classList.remove("intro-clear", "intro-locked");
    var page = document.querySelector(".page-root");
    if (page) page.inert = false;
    if (reduce) return finish();
    root.classList.add("intro-opening");
    later(finish, OPEN_DURATION);
  }

  function skip() {
    if (locked()) return;
    if (!root.classList.contains("intro-opening")) open();
  }

  function type(el, done) {
    var i = 0;
    (function next() {
      if (finished) return;
      if (i < TEXT.length) {
        el.textContent = TEXT.slice(0, ++i);
        later(next, CHAR_INTERVAL);
      } else {
        root.classList.add("intro-typed");
        done();
      }
    })();
  }

  function formatOpen(date) {
    var parts = new Intl.DateTimeFormat("en-US", {
      timeZone: "Asia/Shanghai",
      month: "numeric",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    }).formatToParts(date);
    var get = function (type) {
      var part = parts.find(function (item) { return item.type === type; });
      return part ? part.value : "";
    };
    return get("month") + "月" + get("day") + "日 " + get("hour") + ":" + get("minute");
  }

  function watchUnlock() {
    later(function tick() {
      if (finished) return;
      if (!locked()) return open();
      later(tick, 1000);
    }, 1000);
  }

  function loop(el) {
    if (finished) return;
    if (!locked()) return open();
    root.classList.remove("intro-typed", "intro-clear");
    el.textContent = "";
    type(el, function () {
      if (finished) return;
      if (!locked()) return open();
      later(function () {
        if (finished || !locked()) return open();
        root.classList.add("intro-clear");
        later(function () { loop(el); }, 700 + LOCK_GAP);
      }, LOCK_HOLD);
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    var el = document.querySelector("[data-intro-text]");
    var page = document.querySelector(".page-root");
    if (!el) return finish();

    if (locked()) {
      if (page) page.inert = true;
      var intro = document.querySelector(".intro");
      if (intro) intro.removeAttribute("aria-hidden");
      var label = document.querySelector("[data-open-label]");
      if (label) {
        label.dateTime = root.getAttribute("data-open-at");
        label.textContent = formatOpen(new Date(openAt));
      }
      watchUnlock();
      if (reduce) {
        el.textContent = TEXT;
        root.classList.add("intro-typed");
        return;
      }
      later(function () { loop(el); }, START_DELAY);
      return;
    }

    window.addEventListener("keydown", skip);
    window.addEventListener("pointerdown", skip);
    window.addEventListener("wheel", skip, { passive: true });
    later(function () {
      type(el, function () { later(open, HOLD_AFTER_TYPED); });
    }, START_DELAY);
  });
})();
