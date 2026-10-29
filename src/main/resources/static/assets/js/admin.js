/**
 * admin.js – dùng chung cho trang quản trị: kiểm tra đăng nhập, dựng sidebar, đăng xuất,
 *            thông báo khi khách nhắn tin (toast + âm báo + số tin chưa đọc) ở mọi trang.
 * Trang cần: <aside class="sidebar" id="adminSidebar" data-page="orders"></aside>
 *            <div class="admin-topbar" data-title="..."></div> (tuỳ chọn, cho mobile)
 */
(function (global) {
  "use strict";

  const PAGES = [
    ["dashboard", "dashboard.html", "bi-graph-up-arrow", "Doanh thu"],
    ["orders", "orders.html", "bi-receipt", "Đơn hàng (Bếp)"],
    ["pos", "pos.html", "bi-cash-coin", "Thu ngân (POS)"],
    ["tables", "tables.html", "bi-map", "Sơ đồ bàn & Chat"],
    ["qr", "qr_maker.html", "bi-qr-code", "Mã QR bàn"],
  ];

  const session = App.getAdmin();
  if (!session) {
    const next = encodeURIComponent(global.location.pathname.split("/").pop() || "dashboard.html");
    global.location.replace("login.html?next=" + next);
  }

  function toggleSidebar(force) {
    const sb = document.getElementById("adminSidebar");
    const ov = document.querySelector(".sidebar-overlay");
    const show = typeof force === "boolean" ? force : !sb.classList.contains("show");
    sb.classList.toggle("show", show);
    if (ov) ov.classList.toggle("show", show);
  }

  async function logout() {
    try { await App.api("/api/admin/logout", { method: "POST", redirectOn401: false }); } catch (e) { /* vẫn đăng xuất ở máy */ }
    App.clearAdmin();
    global.location.href = "login.html";
  }

  function render() {
    const sb = document.getElementById("adminSidebar");
    if (!sb || !session) return;
    const active = sb.dataset.page;
    sb.innerHTML = `
      <div class="d-flex justify-content-between align-items-center mb-4 px-2">
        <h4 class="fw-bold text-uppercase m-0"><i class="bi bi-cup-hot-fill me-2"></i>Tiệm Trà</h4>
        <button class="btn btn-link text-white d-md-none p-0" data-close aria-label="Đóng menu"><i class="bi bi-x-lg fs-4"></i></button>
      </div>
      <nav>
        ${PAGES.map(([key, href, icon, label]) =>
          `<a href="${href}" class="nav-link ${key === active ? "active" : ""}" data-key="${key}"><i class="bi ${icon}"></i> ${label}</a>`).join("")}
        <hr class="border-secondary opacity-50" />
        <a href="../welcome.html" class="nav-link text-warning" target="_blank" rel="noopener"><i class="bi bi-box-arrow-up-right"></i> Xem website</a>
      </nav>
      <div class="user-box px-2">
        <button class="btn btn-sm btn-outline-light rounded-pill w-100 mb-2" data-sound title="Âm báo đơn mới, tin nhắn, huỷ đơn"></button>
        <div class="mb-2"><i class="bi bi-person-badge me-1"></i> <span data-username></span></div>
        <button class="btn btn-sm btn-outline-light rounded-pill w-100" data-logout><i class="bi bi-box-arrow-right me-1"></i> Đăng xuất</button>
      </div>`;
    sb.querySelector("[data-username]").textContent = session.username || "";
    sb.querySelector("[data-logout]").addEventListener("click", logout);
    sb.querySelector("[data-close]").addEventListener("click", () => toggleSidebar(false));
    sb.querySelector("[data-sound]").addEventListener("click", () => setSoundOn(!soundOn()));

    if (!document.querySelector(".sidebar-overlay")) {
      const ov = document.createElement("div");
      ov.className = "sidebar-overlay";
      ov.addEventListener("click", () => toggleSidebar(false));
      document.body.prepend(ov);
    }

    const top = document.querySelector(".admin-topbar");
    if (top) {
      top.innerHTML = `<button class="btn btn-light" aria-label="Mở menu"><i class="bi bi-list fs-3"></i></button>
                       <h5 class="m-0 fw-bold"></h5>`;
      top.querySelector("h5").textContent = top.dataset.title || document.title;
      top.querySelector("button").addEventListener("click", () => toggleSidebar(true));
    }

    // Kiểm tra token còn hạn (nếu không App.api sẽ tự chuyển về trang đăng nhập)
    App.api("/api/admin/me").catch(() => {});

    renderChatBadge();
    activePage = active;
    initSound();
    // Trang Sơ đồ bàn tự kết nối chat và tự báo tin mới; các trang khác dùng kết nối thông báo chung
    if (active !== "tables") loadScript(STOMP_SRC, () => typeof Stomp !== "undefined", connectChatNotifier);
    // Trang không tự mở WebSocket đơn hàng (vd. Mã QR) -> mở 1 kết nối chỉ để nhận âm báo đơn mới
    loadScript("../assets/js/websocket.js?v=20261029a", () => typeof OrderSocket !== "undefined", () => {
      if (!(OrderSocket.count && OrderSocket.count())) OrderSocket.connect({ join: { type: "JOIN_ROOM", room: "CASHIER", token: session.token } });
    });
  }

  // ---------- Âm báo dùng chung (đơn mới / tin nhắn / huỷ đơn) ----------
  // Mỗi loại 1 giai điệu riêng để nghe là biết ngay, không cần nhìn màn hình.
  const SOUNDS = {
    // Đơn mới: chuông 3 nốt đi lên (Đô–Mi–Sol), vang 2 lần, to và rõ
    order: { wave: "triangle", vol: 0.35, notes: [[1046.5, 0, 0.13], [1318.5, 0.13, 0.13], [1568, 0.26, 0.3], [1046.5, 0.7, 0.13], [1318.5, 0.83, 0.13], [1568, 0.96, 0.4]] },
    // Tin nhắn: 2 nốt nhẹ, ngắn
    chat: { wave: "sine", vol: 0.18, notes: [[784, 0, 0.1], [1046.5, 0.1, 0.16]] },
    // Huỷ đơn: 2 nốt trầm đi xuống
    cancel: { wave: "square", vol: 0.12, notes: [[523.25, 0, 0.18], [349.2, 0.2, 0.38]] },
    // Máy mới xin vào bàn: 3 tiếng "tít" cùng cao độ
    device: { wave: "sine", vol: 0.22, notes: [[1175, 0, 0.09], [1175, 0.16, 0.09], [1175, 0.32, 0.09]] },
  };
  const SOUND_KEY = "soundOn";
  let audioCtx = null;
  let activePage = "";

  function soundOn() {
    return App.store.get(SOUND_KEY) !== false;
  }

  function setSoundOn(on) {
    App.store.set(SOUND_KEY, !!on);
    if (on) {
      unlockAudio();
      sound("order"); // nghe thử
    }
    renderSoundUI();
  }

  function audio() {
    if (!audioCtx) {
      const Ctx = global.AudioContext || global.webkitAudioContext;
      if (!Ctx) return null;
      audioCtx = new Ctx();
      audioCtx.onstatechange = renderSoundUI;
    }
    return audioCtx;
  }

  /** Trình duyệt chỉ cho phát âm thanh sau khi người dùng bấm/chạm vào trang ít nhất 1 lần. */
  function unlockAudio() {
    const ctx = audio();
    if (ctx && ctx.state === "suspended") ctx.resume().catch(() => {});
  }

  function sound(kind) {
    if (!soundOn()) return;
    const ctx = audio();
    if (!ctx) return;
    if (ctx.state !== "running") {
      // Bị trình duyệt chặn: không xếp hàng âm cũ (sẽ kêu muộn), chỉ nhắc người dùng bấm vào trang
      ctx.resume().catch(() => {});
      renderSoundUI(true);
      return;
    }
    const s = SOUNDS[kind] || SOUNDS.chat;
    const t0 = ctx.currentTime + 0.02;
    s.notes.forEach(([freq, at, dur]) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = s.wave;
      osc.frequency.value = freq;
      gain.gain.setValueAtTime(0.0001, t0 + at);
      gain.gain.exponentialRampToValueAtTime(s.vol, t0 + at + 0.015);
      gain.gain.exponentialRampToValueAtTime(0.0001, t0 + at + dur);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(t0 + at);
      osc.stop(t0 + at + dur + 0.03);
    });
  }

  function renderSoundUI(nhacBat) {
    const on = soundOn();
    const btn = document.querySelector("#adminSidebar [data-sound]");
    if (btn) btn.innerHTML = on ? '<i class="bi bi-bell-fill me-1"></i> Âm báo: Bật' : '<i class="bi bi-bell-slash me-1"></i> Âm báo: Tắt';
    const toggle = document.getElementById("sound-toggle"); // công tắc trên màn hình bếp
    if (toggle) toggle.checked = on;

    let hint = document.getElementById("audio-hint");
    const blocked = on && audioCtx && audioCtx.state !== "running";
    if (blocked && !hint) {
      hint = document.createElement("button");
      hint.id = "audio-hint";
      hint.className = "audio-hint";
      hint.innerHTML = '<i class="bi bi-volume-mute-fill"></i> Trình duyệt đang chặn âm thanh – bấm vào đây để bật âm báo';
      hint.addEventListener("click", () => {
        unlockAudio();
        setTimeout(() => sound("chat"), 150);
      });
      document.body.appendChild(hint);
    }
    if (hint) {
      hint.hidden = !blocked;
      if (blocked && nhacBat) {
        hint.classList.remove("shake");
        void hint.offsetWidth;
        hint.classList.add("shake");
      }
    }
  }

  function initSound() {
    audio(); // tạo sẵn để biết trình duyệt có đang chặn không
    // Lần bấm / chạm / gõ phím đầu tiên ở bất kỳ đâu trên trang -> mở khoá âm thanh
    ["pointerdown", "keydown", "touchstart"].forEach((ev) =>
      document.addEventListener(ev, unlockAudio, { capture: true, passive: true })
    );
    const toggle = document.getElementById("sound-toggle");
    if (toggle) toggle.addEventListener("change", () => setSoundOn(toggle.checked));
    renderSoundUI();
  }

  // Sự kiện đơn hàng từ WebSocket /ws (websocket.js phát lại thành sự kiện "order-socket" trên window)
  global.addEventListener("order-socket", (e) => {
    const d = e.detail || {};
    if (d.type === "NEW_ORDER") {
      if (activePage === "pos" && !(d.tableId > 0)) return; // đơn vừa tự tạo ở quầy này
      sound("order");
      // Bếp và POS tự hiện thông báo riêng; các trang khác hiện ở đây
      if (activePage !== "orders" && activePage !== "pos") {
        const el = App.toast(`Đơn mới #DH-${d.idDonHang} – ${d.tableId > 0 ? "Bàn " + d.tableId : "Tại quầy"} (${d.soMon} món, ${App.formatMoney(d.tongTien)})`, "success", 6000);
        el.style.cursor = "pointer";
        el.title = "Bấm để mở màn hình bếp";
        el.addEventListener("click", () => (global.location.href = "orders.html"));
      }
    } else if (d.type === "DEVICE_PENDING") {
      // Điện thoại mới xin vào bàn đang có khách -> nhân viên ra xác nhận người đó đang ngồi ở bàn
      sound("device");
      const el = App.toast(`${d.tenBan || "Bàn " + d.idBan}: ${d.may} xin vào bàn – bấm để cho phép / từ chối`, "info", 10000);
      el.style.cursor = "pointer";
      el.addEventListener("click", () => {
        if (activePage === "tables" && global.TablesPage) global.TablesPage.show(d.idBan);
        else global.location.href = "tables.html?ban=" + d.idBan;
      });
    } else if (d.type === "ORDER_STATUS_UPDATED" && d.status === 4 && d.nguoiHuy !== session.username) {
      sound("cancel");
      if (activePage !== "orders") {
        App.toast(`Đơn #DH-${d.idDonHang} (${d.tableId > 0 ? "Bàn " + d.tableId : "Tại quầy"}) đã bị huỷ bởi ${d.nguoiHuy || "?"}`, "error", 6000);
      }
    }
  });

  // ---------- Thông báo khi khách nhắn tin ----------
  const UNREAD_KEY = "chatUnread"; // { idBan: số tin chưa đọc } – dùng chung giữa các trang/tab
  const STOMP_SRC = "https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js";
  const baseTitle = document.title;
  let chatRetry = 0;

  function getUnread() {
    const u = App.store.get(UNREAD_KEY);
    return u && typeof u === "object" ? u : {};
  }

  function setUnread(map) {
    App.store.set(UNREAD_KEY, map);
    renderChatBadge();
  }

  function renderChatBadge() {
    const total = Object.values(getUnread()).reduce((s, n) => s + (Number(n) || 0), 0);
    document.title = total ? `(${total}) ${baseTitle}` : baseTitle;
    [document.querySelector('#adminSidebar [data-key="tables"]'), document.querySelector(".admin-topbar .btn")].forEach((host) => {
      if (!host) return;
      let badge = host.querySelector(".chat-badge");
      if (!badge) {
        badge = document.createElement("span");
        badge.className = "chat-badge";
        host.appendChild(badge);
      }
      badge.textContent = total > 99 ? "99+" : String(total);
      badge.hidden = !total;
    });
  }

  function onStaffChat(msg) {
    if (!msg) return;
    const u = getUnread();
    if (msg.type === "CLEAR_CHAT") {
      if (msg.idBan) delete u[msg.idBan];
      else Object.keys(u).forEach((k) => delete u[k]);
      setUnread(u);
      return;
    }
    if (msg.role !== "CUSTOMER" || !msg.idBan) return;
    u[msg.idBan] = (u[msg.idBan] || 0) + 1;
    setUnread(u);
    sound("chat");
    const el = App.toast(`Bàn ${msg.idBan}: ${msg.content}`, "info", 6000);
    el.style.cursor = "pointer";
    el.title = "Bấm để mở chat với bàn này";
    el.addEventListener("click", () => (global.location.href = "tables.html?ban=" + msg.idBan));
  }

  /** Nạp script nếu trang chưa có (daCo() kiểm tra), xong gọi cb. */
  function loadScript(src, daCo, cb) {
    if (daCo()) return cb();
    const s = document.createElement("script");
    s.src = src;
    s.onload = cb;
    document.head.appendChild(s);
  }

  function connectChatNotifier() {
    if (!session || typeof Stomp === "undefined") return;
    const stomp = Stomp.over(new WebSocket(App.WS_BASE + "/ws-chat"));
    stomp.debug = null;
    stomp.heartbeat.outgoing = 10000;
    stomp.heartbeat.incoming = 10000;
    stomp.connect(
      { role: "STAFF", adminToken: session.token },
      () => {
        chatRetry = 0;
        stomp.subscribe("/topic/chat/staff", (f) => {
          try { onStaffChat(JSON.parse(f.body)); } catch (e) { /* ignore */ }
        });
      },
      () => setTimeout(connectChatNotifier, Math.min(30000, 2000 * Math.pow(2, chatRetry++)))
    );
  }

  // Tab khác đọc tin -> cập nhật số chưa đọc ở tab này
  global.addEventListener("storage", (e) => {
    if (e.key === UNREAD_KEY) renderChatBadge();
  });

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", render);
  else render();

  global.Admin = { session, logout, toggleSidebar, getUnread, setUnread, sound, soundOn, setSoundOn };
})(window);
