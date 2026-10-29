/**
 * app.js – tiện ích dùng chung cho mọi trang (khách & nhân viên).
 *  - Xác định địa chỉ backend (không hardcode localhost)
 *  - Gọi API có xử lý lỗi + tự gắn token
 *  - Phiên bàn (mã QR) và phiên nhân viên
 *  - escapeHtml, formatMoney, toast, debounce...
 */
(function (global) {
  "use strict";

  // ---------- Địa chỉ backend ----------
  // Trang do Spring Boot phục vụ -> dùng chính origin hiện tại (chạy được cả qua IP LAN trên điện thoại).
  // Mở bằng Live Server (cổng 5500/5501) hoặc file:// -> gọi sang server HTTPS cổng 8443 cùng máy.
  // Có thể ghi đè: localStorage.setItem("apiBase", "https://192.168.1.10:8443")
  // (chạy HTTP với APP_HTTPS=false thì ghi đè thành "http://localhost:8081")
  function detectApiBase() {
    try {
      const override = localStorage.getItem("apiBase");
      if (override) return override.replace(/\/+$/, "");
    } catch (e) { /* ignore */ }
    const { protocol, hostname, port, origin } = global.location;
    if (protocol === "file:") return "https://localhost:8443";
    if (port === "5500" || port === "5501" || port === "3000") return `https://${hostname}:8443`;
    return origin;
  }

  const API_BASE = detectApiBase();
  const WS_BASE = API_BASE.replace(/^http/, "ws");

  const DEFAULT_IMG =
    "data:image/svg+xml;charset=utf-8," +
    encodeURIComponent(
      '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><rect width="200" height="200" fill="#f3ece7"/>' +
        '<text x="100" y="118" font-size="64" text-anchor="middle">🍵</text></svg>'
    );

  const TRANG_THAI_DON = {
    0: { text: "Chờ xác nhận", cls: "pending", icon: "⏳" },
    1: { text: "Đang pha chế", cls: "cooking", icon: "🔥" },
    2: { text: "Đang mang ra bàn", cls: "delivering", icon: "🛵" },
    3: { text: "Hoàn thành", cls: "completed", icon: "✅" },
    4: { text: "Đã huỷ", cls: "cancelled", icon: "❌" },
  };

  // ---------- localStorage không bao giờ ném lỗi ----------
  // Safari chế độ riêng tư / trình duyệt trong Zalo có thể chặn localStorage và NÉM LỖI khi đọc/ghi;
  // dữ liệu bị sửa tay cũng có thể không phải JSON. Bọc try/catch để trang không chết vì lý do đó.
  const store = {
    get(key) {
      try { return JSON.parse(localStorage.getItem(key)); } catch (e) { return null; }
    },
    set(key, value) {
      try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* ignore */ }
    },
    remove(key) {
      try { localStorage.removeItem(key); } catch (e) { /* ignore */ }
    },
  };

  // ---------- Phiên bàn (khách quét QR) ----------
  // Lưu: { idBan, qr (mã in trong QR – chỉ để xin vào bàn), token (mã riêng của máy này), trangThai, ten, laChu }
  const TABLE_KEY = "tableSession";

  /** Đọc ?idBan=..&t=.. trên URL (khi vừa quét QR) và lưu lại. Trả về bàn nếu máy này ĐÃ được vào bàn. */
  function captureTableFromUrl() {
    const p = new URLSearchParams(global.location.search);
    const idBan = parseInt(p.get("idBan"), 10);
    const qr = p.get("t");
    if (idBan > 0 && qr) {
      const cu = getTableScan();
      // Quét lại đúng mã của bàn cũ -> giữ máy (token) đã có; quét bàn khác -> phải xin vào lại
      const giu = cu && cu.idBan === idBan && cu.qr === qr;
      store.set(TABLE_KEY, giu ? Object.assign({}, cu, { at: Date.now() }) : { idBan, qr, at: Date.now() });
      // Xoá token khỏi thanh địa chỉ để không lộ khi chụp màn hình / chia sẻ link
      try {
        p.delete("t");
        const qs = p.toString();
        global.history.replaceState(null, "", global.location.pathname + (qs ? "?" + qs : ""));
      } catch (e) { /* ignore */ }
    }
    return getTable();
  }

  /** Bàn mà máy này ĐÃ ĐƯỢC VÀO (chủ bàn hoặc được nhân viên duyệt) – mới đặt món / chat được. */
  function getTable() {
    const t = store.get(TABLE_KEY);
    return t && t.idBan > 0 && t.token && t.trangThai === "DA_DUYET" ? t : null;
  }

  /** Bàn đã quét QR (kể cả chưa vào / đang chờ nhân viên duyệt) – dùng ở trang chào. */
  function getTableScan() {
    const t = store.get(TABLE_KEY);
    if (!t || !(t.idBan > 0)) return null;
    if (!t.qr && t.token && !t.trangThai) return { idBan: t.idBan, qr: t.token }; // dữ liệu bản cũ: token chính là mã QR
    return t.qr ? t : null;
  }

  /** Lưu kết quả xin vào bàn (info từ /vao-ban hoặc /thiet-bi/toi); null = máy không còn trong phiên. */
  function setTableDevice(info) {
    const t = getTableScan();
    if (!t) return;
    store.set(TABLE_KEY, {
      idBan: t.idBan,
      qr: t.qr,
      token: info ? info.token || t.token : null,
      trangThai: info ? info.trangThai : null,
      ten: info ? info.ten : null,
      laChu: info ? !!info.laChu : false,
      at: Date.now(),
    });
  }

  function clearTable() {
    store.remove(TABLE_KEY);
  }

  // ---------- Phiên nhân viên ----------
  const ADMIN_KEY = "adminSession";

  function getAdmin() {
    const a = store.get(ADMIN_KEY);
    if (!a || !a.token) return null;
    if (a.hetHan && Date.parse(a.hetHan) < Date.now()) {
      store.remove(ADMIN_KEY);
      return null;
    }
    return a;
  }

  function setAdmin(data) {
    store.set(ADMIN_KEY, { token: data.token, username: data.username, hetHan: data.hetHan });
  }

  function clearAdmin() {
    store.remove(ADMIN_KEY);
  }

  // ---------- Gọi API ----------
  class ApiError extends Error {
    constructor(status, message) {
      super(message);
      this.status = status;
    }
  }

  /**
   * api("/api/mon-an")
   * api("/api/gio-hang/1/items", { method: "POST", body: {...} })
   * Trả về JSON, hoặc null nếu 204. Ném ApiError (có message tiếng Việt từ server) nếu lỗi.
   */
  async function api(path, options = {}) {
    const headers = Object.assign({}, options.headers);
    const table = store.get(TABLE_KEY); // gửi cả token máy đang chờ duyệt (để hỏi trạng thái)
    const admin = getAdmin();
    if (admin) headers["X-Admin-Token"] = admin.token;
    if (table && table.token) headers["X-Table-Token"] = table.token;

    let body = options.body;
    if (body !== undefined && !(body instanceof FormData) && !(body instanceof URLSearchParams) && typeof body !== "string") {
      headers["Content-Type"] = "application/json";
      body = JSON.stringify(body);
    }

    let res;
    try {
      res = await fetch(API_BASE + path, { method: options.method || "GET", headers, body });
    } catch (e) {
      throw new ApiError(0, "Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.");
    }

    if (res.status === 204) return null;

    const isJson = (res.headers.get("Content-Type") || "").includes("application/json");
    const data = isJson ? await res.json().catch(() => null) : null;

    if (!res.ok) {
      const msg = (data && data.message) || `Lỗi ${res.status}`;
      if (res.status === 401 && admin && options.redirectOn401 !== false) {
        clearAdmin();
        const back = encodeURIComponent(global.location.pathname.split("/").pop());
        global.location.href = "login.html?next=" + back;
      }
      // Khách: máy bị mời ra / bàn đã trả (phiên kết thúc) -> về trang chào để xin vào bàn lại bằng mã QR đã quét
      if (res.status === 403 && !admin && table && table.token && options.redirectOn403 !== false) {
        setTableDevice(null);
        if (!/welcome\.html$/.test(global.location.pathname)) {
          global.location.href = (global.location.pathname.includes("/pages/") ? "../" : "") + "welcome.html";
        }
      }
      throw new ApiError(res.status, msg);
    }
    return data;
  }

  // ---------- Tiện ích hiển thị ----------
  /**
   * Chống XSS: BẮT BUỘC bọc mọi dữ liệu lấy từ server / người dùng (tên món, ghi chú, lý do huỷ, tin chat...)
   * bằng escapeHtml trước khi ghép vào innerHTML. Nếu không, khách gõ ghi chú "<img src=x onerror=...>" thì
   * đoạn mã đó chạy trên máy nhân viên và có thể lấy token đăng nhập. Gán bằng textContent thì không cần escape.
   */
  function escapeHtml(value) {
    return String(value ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function formatMoney(n) {
    return Number(n || 0).toLocaleString("vi-VN") + "đ";
  }

  function formatTime(iso) {
    if (!iso) return "--:--";
    const d = new Date(iso);
    return isNaN(d) ? "--:--" : d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
  }

  function formatDateTime(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    return isNaN(d) ? "" : d.toLocaleString("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit" });
  }

  /** Ảnh từ backend: đường dẫn tương đối /uploads/.. -> URL đầy đủ. */
  function imageUrl(path) {
    if (!path) return DEFAULT_IMG;
    if (/^(https?:|data:)/.test(path)) return path;
    return API_BASE + (path.startsWith("/") ? path : "/" + path);
  }

  /** Dùng trong onerror: chỉ thay ảnh 1 lần, tránh vòng lặp request vô hạn. */
  function imgFallback(img) {
    img.onerror = null;
    img.src = DEFAULT_IMG;
  }

  function debounce(fn, ms = 250) {
    let t;
    return function (...args) {
      clearTimeout(t);
      t = setTimeout(() => fn.apply(this, args), ms);
    };
  }

  /**
   * Thông báo nhỏ góc màn hình thay cho alert() (không chặn giao diện). type: success|error|info
   * Trả về phần tử toast (để gắn sự kiện click nếu cần).
   */
  function toast(message, type = "info", ms = 3000) {
    let box = document.getElementById("app-toast-box");
    if (!box) {
      box = document.createElement("div");
      box.id = "app-toast-box";
      box.className = "app-toast-box";
      box.setAttribute("aria-live", "polite");
      document.body.appendChild(box);
    }
    const el = document.createElement("div");
    el.className = `app-toast app-toast-${type}`;
    const icon = { success: "bi-check-circle-fill", error: "bi-exclamation-triangle-fill", info: "bi-info-circle-fill" }[type] || "bi-info-circle-fill";
    el.innerHTML = `<i class="bi ${icon}"></i><span></span>`;
    el.querySelector("span").textContent = message;
    box.appendChild(el);
    requestAnimationFrame(() => el.classList.add("show"));
    setTimeout(() => {
      el.classList.remove("show");
      setTimeout(() => el.remove(), 300);
    }, ms);
    return el;
  }

  /** Khoá nút trong lúc chờ request (chống bấm 2 lần). */
  async function withBusy(button, fn) {
    if (!button) return fn();
    if (button.dataset.busy === "1") return;
    button.dataset.busy = "1";
    button.disabled = true;
    button.classList.add("is-busy");
    try {
      return await fn();
    } finally {
      button.dataset.busy = "";
      button.disabled = false;
      button.classList.remove("is-busy");
    }
  }

  global.App = {
    API_BASE,
    WS_BASE,
    DEFAULT_IMG,
    TRANG_THAI_DON,
    api,
    ApiError,
    store,
    captureTableFromUrl,
    getTable,
    getTableScan,
    setTableDevice,
    clearTable,
    getAdmin,
    setAdmin,
    clearAdmin,
    escapeHtml,
    formatMoney,
    formatTime,
    formatDateTime,
    imageUrl,
    imgFallback,
    debounce,
    toast,
    withBusy,
  };
})(window);
