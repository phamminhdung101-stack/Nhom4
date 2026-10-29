/**
 * websocket.js – kết nối Raw WebSocket /ws để nhận thông báo đơn hàng thời gian thực.
 *
 *   OrderSocket.connect({
 *     join: { type: "JOIN_ROOM", room: "KITCHEN", token: adminToken },   // hoặc { type: "JOIN_TABLE", idBan, token: <mã riêng của máy> }
 *     onEvent: (data) => { ... },        // NEW_ORDER | ORDER_STATUS_UPDATED | TABLE_STATUS_UPDATED | CART_UPDATED
 *                                        // | DEVICE_PENDING | LEFT_TABLE | RECONNECTED (do file này tự phát)
 *     onStatus: (state) => { ... },      // "connecting" | "connected" | "disconnected" | "error"
 *   });
 *
 * - Tự kết nối lại với backoff tăng dần (1s, 2s, 4s ... tối đa 30s).
 * - Heartbeat: server gửi Ping frame (opcode 0x9) mỗi 25s, trình duyệt tự trả Pong (0xA).
 * - Khi kết nối lại thành công -> gọi onEvent({type:"RECONNECTED"}) để trang tải lại dữ liệu bị lỡ.
 * - Mỗi sự kiện còn được phát thành window event "order-socket" (admin.js dùng để phát âm báo chung).
 * - Không kết nối được (WebSocket bị chặn) -> dự phòng: cứ pollMs (5s) gọi onEvent({type:"RECONNECTED", polling:true})
 *   để trang tự tải lại dữ liệu; kết nối được lại thì thôi.
 */
(function (global) {
  "use strict";

  let soKetNoi = 0;

  function connect(options) {
    soKetNoi++;
    const opts = Object.assign({ onEvent() {}, onStatus() {}, pollMs: 5000 }, options);
    let socket = null;
    let retry = 0;
    let timer = null;
    let pollTimer = null;
    let stopped = false;
    let everConnected = false;

    function setStatus(state, detail) {
      try { opts.onStatus(state, detail); } catch (e) { console.error(e); }
      const el = document.getElementById("ws-status");
      if (el) {
        el.dataset.state = state;
        el.title = state === "connected" ? "Đang nhận cập nhật trực tiếp"
          : pollTimer ? `Không có kết nối trực tiếp – tự cập nhật mỗi ${opts.pollMs / 1000} giây`
          : "Mất kết nối, đang thử lại...";
      }
    }

    // Dự phòng: WebSocket bị chặn (vd. iPhone + chứng chỉ HTTPS tự ký, mạng chặn WebSocket...) ->
    // cứ vài giây báo trang tải lại dữ liệu (cùng sự kiện "RECONNECTED" mà các trang đã xử lý sẵn).
    function startPolling() {
      if (pollTimer || stopped) return;
      pollTimer = setInterval(() => {
        if (document.hidden) return;
        try { opts.onEvent({ type: "RECONNECTED", polling: true }); } catch (e) { console.error(e); }
      }, opts.pollMs);
    }

    function stopPolling() {
      clearInterval(pollTimer);
      pollTimer = null;
    }

    function open() {
      if (stopped) return;
      clearTimeout(timer);
      if (socket && socket.readyState <= 1) return; // đang mở / đang kết nối -> không tạo kết nối trùng
      setStatus("connecting");
      try {
        socket = new WebSocket(App.WS_BASE + "/ws");
      } catch (e) {
        schedule();
        return;
      }

      socket.onopen = () => {
        if (opts.join) socket.send(JSON.stringify(opts.join));
        else markConnected();
      };

      socket.onmessage = (ev) => {
        if (ev.data === "pong") return;
        let data;
        try { data = JSON.parse(ev.data); } catch (e) { return; }
        if (data.type === "JOINED") return markConnected();
        if (data.type === "ERROR") {
          startPolling(); // không vào được phòng -> vẫn cập nhật bằng cách tải lại định kỳ
          setStatus("error", data.message);
          console.warn("[OrderSocket]", data.message);
          return;
        }
        try { global.dispatchEvent(new CustomEvent("order-socket", { detail: data })); } catch (e) { console.error(e); }
        try { opts.onEvent(data); } catch (e) { console.error(e); }
      };

      socket.onclose = () => {
        startPolling();
        setStatus("disconnected");
        schedule();
      };

      socket.onerror = () => { /* onclose sẽ được gọi tiếp ngay sau */ };
    }

    function markConnected() {
      const wasReconnect = everConnected || !!pollTimer;
      everConnected = true;
      retry = 0;
      stopPolling();
      setStatus("connected");
      if (wasReconnect) {
        try { opts.onEvent({ type: "RECONNECTED" }); } catch (e) { console.error(e); }
      }
    }

    function schedule() {
      if (stopped) return;
      clearTimeout(timer);
      const delay = Math.min(30000, 1000 * Math.pow(2, retry++));
      timer = setTimeout(open, delay);
    }

    // Quay lại tab/ có mạng lại -> thử kết nối ngay
    global.addEventListener("online", () => { retry = 0; if (!socket || socket.readyState > 1) open(); });
    document.addEventListener("visibilitychange", () => {
      if (!document.hidden && (!socket || socket.readyState > 1)) { retry = 0; open(); }
    });

    open();

    return {
      close() {
        stopped = true;
        clearTimeout(timer);
        stopPolling();
        if (socket) socket.close();
      },
    };
  }

  global.OrderSocket = { connect, count: () => soKetNoi };
})(window);
