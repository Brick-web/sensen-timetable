(function () {
  "use strict";

  if (!window.matchMedia("(pointer: fine)").matches) return;
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

  var MAG = 1.2;

  function mount() {
    var page = document.querySelector(".page-root");
    if (!page) return;

    var drop = document.createElement("div");
    drop.className = "cursor-drop";
    drop.setAttribute("aria-hidden", "true");
    drop.inert = true;
    var sample = document.createElement("div");
    sample.className = "cursor-drop__sample";
    drop.appendChild(sample);
    document.body.appendChild(drop);

    var scene;
    var tx = 0;
    var ty = 0;
    var x = 0;
    var y = 0;
    var running = false;
    var seen = false;
    var rebuildPending = false;

    function rebuildScene() {
      rebuildPending = false;
      // 复制页面本身而非截图；文字、背景与玻璃卡片均在透镜内真正放大。
      // 镜像不可交互，不重复执行脚本，也不复制页面 ID。
      var next = page.cloneNode(true);
      next.classList.add("cursor-drop__scene");
      next.removeAttribute("id");
      next.setAttribute("aria-hidden", "true");
      next.inert = true;
      next.querySelectorAll("script, iframe").forEach(function (node) { node.remove(); });
      next.querySelectorAll("[id]").forEach(function (node) { node.removeAttribute("id"); });
      var rect = page.getBoundingClientRect();
      next.style.width = rect.width + "px";
      next.style.height = rect.height + "px";
      if (scene) sample.replaceChild(next, scene);
      else sample.appendChild(next);
      scene = next;
      if (seen) kick();
    }

    function scheduleRebuild() {
      if (rebuildPending) return;
      rebuildPending = true;
      requestAnimationFrame(rebuildScene);
    }

    function frame() {
      var px = x;
      var py = y;
      x += (tx - x) * 0.2;
      y += (ty - y) * 0.2;
      var vx = x - px;
      var vy = y - py;
      var speed = Math.min(Math.hypot(vx, vy), 18);
      var stretch = 1 + speed * 0.012;
      var squeeze = 2 - stretch;
      var angle = speed > 0.4 ? Math.atan2(vy, vx) : 0;
      var rect = page.getBoundingClientRect();
      var centerX = x - rect.left;
      var centerY = y - rect.top;

      // 外框保留随速度拉伸、旋转的水滴形状，所有放大内容都在圆角内裁切。
      drop.style.transform =
        "translate3d(" + x.toFixed(2) + "px," + y.toFixed(2) + "px,0) rotate(" +
        angle.toFixed(4) + "rad) scale(" + stretch.toFixed(4) + "," + squeeze.toFixed(4) + ")";
      // 反向抵消外框变形，避免文字随水滴旋转；只对页面内容施加真实缩放。
      sample.style.transform =
        "scale(" + (1 / stretch).toFixed(6) + "," + (1 / squeeze).toFixed(6) + ") rotate(" +
        (-angle).toFixed(4) + "rad) scale(" + MAG + ") translate3d(" +
        (-centerX).toFixed(2) + "px," + (-centerY).toFixed(2) + "px,0)";

      if (Math.abs(tx - x) < 0.15 && Math.abs(ty - y) < 0.15 && speed < 0.15) {
        running = false;
        return;
      }
      requestAnimationFrame(frame);
    }

    function kick() {
      if (!running) {
        running = true;
        requestAnimationFrame(frame);
      }
    }

    rebuildScene();
    // 版本号、下载信息更新后同步镜像；页面滚动时只更新取样位置。
    new MutationObserver(scheduleRebuild).observe(page, {
      subtree: true, childList: true, characterData: true, attributes: true,
    });
    window.addEventListener("resize", scheduleRebuild, { passive: true });
    window.addEventListener("scroll", function () { if (seen) kick(); }, { passive: true });
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(scheduleRebuild);

    function hide() {
      drop.classList.remove("is-on");
      seen = false;
    }

    window.addEventListener("pointermove", function (e) {
      if (e.pointerType && e.pointerType !== "mouse") return;
      tx = e.clientX;
      ty = e.clientY;
      if (!seen) {
        seen = true;
        x = tx;
        y = ty;
        drop.classList.add("is-on");
      }
      kick();
    }, { passive: true });

    window.addEventListener("pointerout", function (e) {
      if (!e.relatedTarget) hide();
    });
    window.addEventListener("blur", hide);
    document.addEventListener("visibilitychange", function () {
      if (document.hidden) hide();
    });
  }

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", mount);
  else mount();
})();
