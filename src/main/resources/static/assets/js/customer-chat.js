/**
 * customer-chat.js – khung chat khách <-> nhân viên (STOMP over WebSocket /ws-chat).
 * Cần: app.js và thư viện stomp.js 2.3.3. Tin nhắn chỉ giữ trong RAM (không lưu localStorage).
 * API cho trang khác: CustomerChat.send("Xin thêm đá"), CustomerChat.open()
 */
(function (global) {
  "use strict";
  if (global.CustomerChat) return;

  const table = App.getTable();
  const tableId = table ? table.idBan : null;

  // Mã riêng của điện thoại này: nhiều máy cùng bàn -> phân biệt tin "Bạn" với tin "Khách cùng bàn"
  let clientId = App.store.get("chatClientId");
  if (!clientId) {
    clientId = (global.crypto && crypto.randomUUID ? crypto.randomUUID() : String(Math.random()).slice(2) + Date.now()).slice(0, 36);
    App.store.set("chatClientId", clientId);
  }

  let stompClient = null;
  let isConnected = false;
  let isOpen = false;
  let unread = 0;
  let retry = 0;
  let reconnectTimer = null;
  // Dự phòng khi WebSocket/STOMP bị chặn (vd. iPhone + chứng chỉ HTTPS tự ký): gửi qua HTTP, vài giây lấy tin mới 1 lần
  let pollTimer = null;
  let daTaiLichSu = false;
  const daHien = new Set(); // tin đã hiện (tránh trùng giữa STOMP, hỏi định kỳ và tin vừa gửi)
  const khoa = (m) => `${m.timestamp}|${m.role}|${m.content}`;

  const $ = (id) => document.getElementById(id);

  function createUI() {
    const root = document.createElement("div");
    root.id = "customer-chat-root";
    root.innerHTML = `
      <button class="customer-chat-fab" id="customer-chat-fab" title="Trò chuyện với nhân viên" aria-label="Mở chat">
        <i class="bi bi-chat-dots-fill chat-icon"></i>
        <span class="unread-badge d-none" id="customer-chat-badge">0</span>
      </button>
      <div class="customer-chat-popup collapsed" id="customer-chat-popup" role="dialog" aria-label="Chat với quán">
        <div class="chat-header">
          <div class="d-flex align-items-center gap-2">
            <i class="bi bi-headset fs-5 text-warning"></i>
            <div>
              <h6 class="chat-header-title">${tableId ? "Bàn " + tableId : "Khách hàng"} - Hỗ trợ</h6>
              <span class="chat-status-pill" id="customer-chat-status"></span>
            </div>
          </div>
          <button type="button" class="btn-close-chat" id="customer-chat-close" title="Đóng"><i class="bi bi-x-lg"></i></button>
        </div>
        <div class="chat-body" id="customer-chat-messages">
          <div class="chat-empty-state" id="customer-chat-empty">
            <i class="bi bi-chat-heart"></i>
            <p class="m-0 fw-bold">Xin chào quý khách!</p>
            <small>${tableId
              ? "Quý khách cần thêm nước, đường, đá hoặc gọi nhân viên? Hãy nhắn cho quán tại đây nhé."
              : "Vui lòng quét mã QR tại bàn để chat với nhân viên phục vụ."}</small>
          </div>
        </div>
        <form class="chat-footer" id="customer-chat-form">
          <input type="text" class="chat-input" id="customer-chat-input" autocomplete="off"
                 placeholder="${tableId ? "Nhập tin nhắn..." : "Vui lòng quét mã QR tại bàn"}"
                 maxlength="500" ${tableId ? "" : "disabled"} />
          <button class="chat-send-btn" type="submit" title="Gửi" ${tableId ? "" : "disabled"}><i class="bi bi-send-fill"></i></button>
        </form>
      </div>`;
    document.body.appendChild(root);

    $("customer-chat-fab").addEventListener("click", () => toggle());
    $("customer-chat-close").addEventListener("click", () => toggle(false));
    $("customer-chat-form").addEventListener("submit", (e) => {
      e.preventDefault();
      const input = $("customer-chat-input");
      if (send(input.value)) input.value = "";
    });
    setStatus(tableId ? "connecting" : "disconnected");
  }

  function toggle(force) {
    isOpen = typeof force === "boolean" ? force : !isOpen;
    $("customer-chat-popup").classList.toggle("collapsed", !isOpen);
    if (isOpen) {
      unread = 0;
      updateBadge();
      setTimeout(() => $("customer-chat-input").focus(), 150);
      scrollBottom();
    }
  }

  function setStatus(state) {
    const el = $("customer-chat-status");
    if (!el) return;
    const map = {
      connected: ["connected", "Đã kết nối"],
      connecting: ["connecting", "Đang kết nối..."],
      disconnected: ["disconnected", tableId ? "Mất kết nối – đang thử lại" : "Chưa xác định bàn"],
      polling: ["connecting", "Kết nối dự phòng – vài giây cập nhật 1 lần"],
    };
    const [cls, text] = map[state] || map.disconnected;
    el.innerHTML = `<span class="chat-status-dot ${cls}"></span> ${text}`;
  }

  function updateBadge() {
    const b = $("customer-chat-badge");
    b.textContent = unread;
    b.classList.toggle("d-none", unread === 0);
  }

  function scrollBottom() {
    const c = $("customer-chat-messages");
    if (c) c.scrollTop = c.scrollHeight;
  }

  function renderMessage(msg) {
    daHien.add(khoa(msg));
    const empty = $("customer-chat-empty");
    if (empty) empty.style.display = "none";
    // Tin của khách: của máy này nếu cùng clientId (tin cũ chưa có clientId coi như của mình)
    const mine = msg.role === "CUSTOMER" && (!msg.clientId || msg.clientId === clientId);
    const row = document.createElement("div");
    row.className = `chat-message-row ${mine ? "mine" : "theirs"}`;

    const name = document.createElement("span");
    name.className = "chat-sender-name";
    name.textContent = mine ? "Bạn" : msg.role === "CUSTOMER" ? "Khách cùng bàn" : msg.sender || "Nhân viên";
    const bubble = document.createElement("div");
    bubble.className = "chat-bubble";
    bubble.textContent = msg.content; // textContent -> không thể chèn HTML/JS
    const time = document.createElement("span");
    time.className = "chat-time";
    time.textContent = App.formatTime(msg.timestamp || Date.now());

    row.append(name, bubble, time);
    $("customer-chat-messages").appendChild(row);
    scrollBottom();
  }

  function resetMessages(text) {
    daHien.clear();
    const c = $("customer-chat-messages");
    c.innerHTML = `<div class="chat-empty-state" id="customer-chat-empty">
        <i class="bi bi-chat-heart"></i><p class="m-0 fw-bold">Xin chào quý khách!</p><small></small></div>`;
    c.querySelector("small").textContent = text;
  }

  function handleIncoming(msg) {
    if (!msg) return;
    if (msg.type === "CLEAR_CHAT") {
      resetMessages("Bàn đã hoàn thành / đặt lại. Phiên trò chuyện mới đã sẵn sàng.");
      return;
    }
    if (!msg.content || daHien.has(khoa(msg))) return;
    renderMessage(msg);
    // Tin nhân viên + tin hệ thống (vd. "Đơn #DH-5 đã được huỷ") -> âm báo, số chưa đọc, toast
    if (msg.role === "STAFF" || msg.type === "NOTIFICATION") {
      beep();
      if (!isOpen) {
        unread++;
        updateBadge();
        App.toast((msg.role === "STAFF" ? "Quán: " : "") + msg.content, "info", 4000);
      }
    }
  }

  async function loadHistory() {
    try {
      const history = await App.api(`/api/chat/history/${tableId}`);
      const c = $("customer-chat-messages");
      c.querySelectorAll(".chat-message-row").forEach((r) => r.remove());
      daHien.clear();
      (history || []).forEach(renderMessage);
      daTaiLichSu = true;
    } catch (e) { /* bỏ qua */ }
  }

  /** Dự phòng: hỏi lịch sử chat định kỳ, hiện tin mới (có âm báo / toast như khi nhận qua STOMP). */
  async function hoiTinMoi() {
    if (document.hidden) return;
    if (!daTaiLichSu) return loadHistory(); // lần đầu: hiện lịch sử, không kêu lại tin cũ
    try {
      const history = (await App.api(`/api/chat/history/${tableId}`)) || [];
      if (!history.length && daHien.size) {
        resetMessages("Bàn đã hoàn thành / đặt lại. Phiên trò chuyện mới đã sẵn sàng.");
        return;
      }
      history.forEach(handleIncoming);
    } catch (e) { /* lỗi mạng: lần sau hỏi lại */ }
  }

  function batDuPhong() {
    if (pollTimer || !tableId) return;
    setStatus("polling");
    hoiTinMoi();
    pollTimer = setInterval(hoiTinMoi, 4000);
  }

  function tatDuPhong() {
    clearInterval(pollTimer);
    pollTimer = null;
  }

  function connect() {
    if (!tableId) return;
    if (typeof Stomp === "undefined") {
      console.warn("Thiếu thư viện stomp.js – chat dùng chế độ dự phòng");
      batDuPhong();
      return;
    }
    setStatus("connecting");
    const ws = new WebSocket(App.WS_BASE + "/ws-chat");
    stompClient = Stomp.over(ws);
    stompClient.debug = null;
    stompClient.heartbeat.outgoing = 10000;
    stompClient.heartbeat.incoming = 10000;

    stompClient.connect(
      { role: "CUSTOMER", idBan: String(tableId), tableToken: table.token },
      () => {
        isConnected = true;
        retry = 0;
        tatDuPhong();
        setStatus("connected");
        stompClient.subscribe(`/topic/chat/table/${tableId}`, (frame) => {
          try { handleIncoming(JSON.parse(frame.body)); } catch (e) { /* ignore */ }
        });
        loadHistory();
      },
      () => {
        isConnected = false;
        batDuPhong(); // WebSocket bị chặn / mất kết nối -> vẫn chat được qua HTTP trong lúc thử kết nối lại
        clearTimeout(reconnectTimer);
        reconnectTimer = setTimeout(connect, Math.min(30000, 2000 * Math.pow(2, retry++)));
      }
    );
  }

  function publish(content) {
    stompClient.send("/app/chat.send", {}, JSON.stringify({ content, type: "CHAT", clientId }));
  }

  /** Gửi tin nhắn: qua STOMP nếu đang kết nối, không thì gửi qua HTTP (dự phòng). */
  function send(text) {
    const content = String(text || "").trim();
    if (!content || !tableId) return false;
    if (isConnected && stompClient) {
      publish(content);
    } else {
      App.api(`/api/chat/${tableId}`, { method: "POST", body: { content, clientId } })
        .then(handleIncoming)
        .catch((e) => App.toast(e.message, "error", 4000));
    }
    return true;
  }

  function beep() {
    try {
      const Ctx = global.AudioContext || global.webkitAudioContext;
      if (!Ctx) return;
      const ctx = new Ctx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.frequency.setValueAtTime(587.33, ctx.currentTime);
      osc.frequency.setValueAtTime(880, ctx.currentTime + 0.1);
      gain.gain.setValueAtTime(0.15, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.35);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.35);
    } catch (e) { /* trình duyệt chặn âm thanh trước khi người dùng tương tác */ }
  }

  function init() {
    createUI();
    connect();
  }

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", init);
  else init();

  global.CustomerChat = { send, open: () => toggle(true) };
})(window);
