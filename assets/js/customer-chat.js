/**
 * customer-chat.js
 * Real-time WebSocket Chat Widget for Customers
 * Strictly in-memory: NO localStorage / sessionStorage / MongoDB storage of chat messages.
 */

(function () {
  "use strict";

  // Prevent multiple initializations
  if (window.__customerChatInitialized) return;
  window.__customerChatInitialized = true;

  // In-memory chat messages array for active session
  const activeSessionMessages = [];
  let unreadCount = 0;
  let isWindowOpen = false;
  let stompClient = null;
  let isConnected = false;
  let reconnectTimer = null;

  // Resolve current table ID
  function getTableId() {
    const params = new URLSearchParams(window.location.search);
    let id = params.get("idBan") || localStorage.getItem("idBan") || localStorage.getItem("current_table_id");
    if (!id || id === "0" || isNaN(Number(id))) {
      return null;
    }
    return parseInt(id, 10);
  }

  const tableId = getTableId();

  // Gentle audio beep on staff reply using Web Audio API (no external file dependency)
  function playBeep() {
    try {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (!AudioContext) return;
      const ctx = new AudioContext();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = "sine";
      osc.frequency.setValueAtTime(587.33, ctx.currentTime); // D5
      osc.frequency.setValueAtTime(880, ctx.currentTime + 0.1); // A5
      gain.gain.setValueAtTime(0.15, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.35);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.35);
    } catch (e) {
      // Audio playback might be restricted by browser policy before user interaction
    }
  }

  // Create UI DOM
  function createChatUI() {
    const container = document.createElement("div");
    container.id = "customer-chat-root";

    const tableNameText = tableId ? `Bàn ${tableId}` : "Khách hàng";

    container.innerHTML = `
      <!-- Floating Button -->
      <button class="customer-chat-fab" id="customer-chat-fab" title="Trò chuyện với nhân viên" aria-label="Mở chat">
        <i class="bi bi-chat-dots-fill chat-icon"></i>
        <span class="unread-badge d-none" id="customer-chat-badge">0</span>
      </button>

      <!-- Chat Popup Window -->
      <div class="customer-chat-popup collapsed" id="customer-chat-popup">
        <div class="chat-header">
          <div class="d-flex align-items-center gap-2">
            <i class="bi bi-headset fs-5 text-warning"></i>
            <div>
              <h6 class="chat-header-title">${tableNameText} - Hỗ trợ</h6>
              <span class="chat-status-pill" id="customer-chat-status">
                <span class="chat-status-dot connecting"></span> Đang kết nối...
              </span>
            </div>
          </div>
          <button type="button" class="btn-close-chat" id="customer-chat-close" title="Đóng">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <div class="chat-body" id="customer-chat-messages">
          <div class="chat-empty-state" id="customer-chat-empty">
            <i class="bi bi-chat-heart"></i>
            <p class="m-0 fw-bold">Xin chào quý khách!</p>
            <small>Quý khách cần hỗ trợ thêm nước, đường, đá hoặc gọi nhân viên? Hãy nhắn cho quán tại đây nhé.</small>
          </div>
        </div>

        <div class="chat-footer">
          <input type="text" class="chat-input" id="customer-chat-input" 
                 placeholder="${tableId ? 'Nhập tin nhắn hỗ trợ...' : 'Vui lòng quét mã QR tại bàn'}" 
                 maxlength="500" ${!tableId ? "disabled" : ""} />
          <button class="chat-send-btn" id="customer-chat-send" title="Gửi" ${!tableId ? "disabled" : ""}>
            <i class="bi bi-send-fill"></i>
          </button>
        </div>
      </div>
    `;

    document.body.appendChild(container);

    // Event listeners
    const fab = document.getElementById("customer-chat-fab");
    const popup = document.getElementById("customer-chat-popup");
    const closeBtn = document.getElementById("customer-chat-close");
    const sendBtn = document.getElementById("customer-chat-send");
    const input = document.getElementById("customer-chat-input");

    fab.addEventListener("click", () => {
      toggleChatWindow();
    });

    closeBtn.addEventListener("click", () => {
      toggleChatWindow(false);
    });

    sendBtn.addEventListener("click", () => {
      sendCustomerMessage();
    });

    input.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        sendCustomerMessage();
      }
    });
  }

  function toggleChatWindow(forceState) {
    const popup = document.getElementById("customer-chat-popup");
    const badge = document.getElementById("customer-chat-badge");
    const input = document.getElementById("customer-chat-input");

    if (typeof forceState === "boolean") {
      isWindowOpen = forceState;
    } else {
      isWindowOpen = !isWindowOpen;
    }

    if (isWindowOpen) {
      popup.classList.remove("collapsed");
      unreadCount = 0;
      badge.classList.add("d-none");
      badge.innerText = "0";
      setTimeout(() => input && input.focus(), 150);
      scrollChatToBottom();
    } else {
      popup.classList.add("collapsed");
    }
  }

  function updateStatus(status) {
    const statusEl = document.getElementById("customer-chat-status");
    if (!statusEl) return;

    if (status === "connected") {
      statusEl.innerHTML = `<span class="chat-status-dot connected"></span> Đã kết nối`;
    } else if (status === "connecting") {
      statusEl.innerHTML = `<span class="chat-status-dot connecting"></span> Đang kết nối...`;
    } else {
      statusEl.innerHTML = `<span class="chat-status-dot disconnected"></span> Mất kết nối`;
    }
  }

  function appendMessageUI(msg) {
    const emptyState = document.getElementById("customer-chat-empty");
    if (emptyState) {
      emptyState.style.display = "none";
    }

    const messagesContainer = document.getElementById("customer-chat-messages");
    if (!messagesContainer) return;

    const isMine = msg.role === "CUSTOMER";
    const row = document.createElement("div");
    row.className = `chat-message-row ${isMine ? "mine" : "theirs"}`;

    const timeStr = msg.timestamp
      ? new Date(msg.timestamp).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })
      : new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });

    row.innerHTML = `
      <span class="chat-sender-name">${isMine ? "Bạn" : msg.sender || "Nhân viên"}</span>
      <div class="chat-bubble">${msg.content}</div>
      <span class="chat-time">${timeStr}</span>
    `;

    messagesContainer.appendChild(row);
    scrollChatToBottom();
  }

  function scrollChatToBottom() {
    const messagesContainer = document.getElementById("customer-chat-messages");
    if (messagesContainer) {
      messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
  }

  function showToast(text) {
    const toast = document.createElement("div");
    toast.className = "chat-toast";
    toast.innerHTML = `
      <div class="d-flex align-items-center gap-2">
        <i class="bi bi-chat-fill text-warning fs-5"></i>
        <div>
          <strong class="d-block small">Tin nhắn mới từ Quán</strong>
          <span class="small">${text}</span>
        </div>
      </div>
    `;
    document.body.appendChild(toast);
    setTimeout(() => {
      toast.style.transition = "opacity 0.4s";
      toast.style.opacity = "0";
      setTimeout(() => toast.remove(), 400);
    }, 4000);
  }

  // Pure WebSocket-based STOMP protocol client (works 100% offline & with SockJS/Stomp if available)
  function initWebSocketConnection() {
    if (!tableId) {
      updateStatus("disconnected");
      const emptyState = document.getElementById("customer-chat-empty");
      if (emptyState) {
        emptyState.innerHTML = `
          <i class="bi bi-qr-code text-danger"></i>
          <p class="m-0 fw-bold text-danger">Chưa xác định được bàn</p>
          <small>Vui lòng quét mã QR tại bàn để có thể chat với nhân viên phục vụ.</small>
        `;
      }
      return;
    }

    updateStatus("connecting");

    function getBackendHost() {
      if (window.location.port === "8080") {
        return window.location.host;
      }
      return "localhost:8080";
    }

    const host = getBackendHost();
    const wsProtocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const wsUrl = `${wsProtocol}//${host}/ws-chat`;

    // Check if external Stomp library is present
    if (typeof Stomp !== "undefined") {
      try {
        let socket;
        if (typeof SockJS !== "undefined") {
          socket = new SockJS(`http://${host}/ws-chat`);
        } else {
          socket = new WebSocket(wsUrl);
        }
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Disable debug logging for security & performance

        const headers = {
          role: "CUSTOMER",
          idBan: tableId,
          sender: "Bàn " + tableId,
        };

        stompClient.connect(
          headers,
          function () {
            isConnected = true;
            updateStatus("connected");

            // Subscribe to table topic
            stompClient.subscribe(`/topic/chat/table/${tableId}`, function (messageOutput) {
              handleIncomingMessage(JSON.parse(messageOutput.body));
            });

            // Load active session temporary chat history for this table
            loadChatHistory();
          },
          function () {
            isConnected = false;
            updateStatus("disconnected");
            scheduleReconnect();
          }
        );
        return;
      } catch (e) {
        console.warn("Stomp init failed, falling back to native STOMP WebSocket:", e);
      }
    }

    // Native fallback STOMP implementation over browser WebSocket
    connectNativeStomp(wsUrl);
  }

  function connectNativeStomp(wsUrl) {
    try {
      const socket = new WebSocket(wsUrl);

      socket.onopen = function () {
        const connectFrame =
          "CONNECT\n" +
          "accept-version:1.2,1.1,1.0\n" +
          "heart-beat:10000,10000\n" +
          "role:CUSTOMER\n" +
          `idBan:${tableId}\n` +
          `sender:Bàn ${tableId}\n` +
          "\n\0";
        socket.send(connectFrame);
      };

      socket.onmessage = function (event) {
        const data = event.data;
        if (data.startsWith("CONNECTED")) {
          isConnected = true;
          updateStatus("connected");

          // Subscribe frame
          const subFrame =
            "SUBSCRIBE\n" +
            "id:sub-table-" + tableId + "\n" +
            `destination:/topic/chat/table/${tableId}\n` +
            "\n\0";
          socket.send(subFrame);
        } else if (data.startsWith("MESSAGE")) {
          const bodyIndex = data.indexOf("\n\n");
          if (bodyIndex !== -1) {
            const rawBody = data.substring(bodyIndex + 2).replace(/\0$/, "").trim();
            try {
              const parsed = JSON.parse(rawBody);
              handleIncomingMessage(parsed);
            } catch (err) {
              // ignore parse errors
            }
          }
        }
      };

      socket.onclose = function () {
        isConnected = false;
        updateStatus("disconnected");
        scheduleReconnect();
      };

      socket.onerror = function () {
        isConnected = false;
        updateStatus("disconnected");
      };

      stompClient = {
        send: function (destination, headers, body) {
          if (socket.readyState === WebSocket.OPEN) {
            const frame =
              "SEND\n" +
              `destination:${destination}\n` +
              "content-type:application/json\n" +
              "\n" +
              body +
              "\0";
            socket.send(frame);
          }
        },
      };
    } catch (e) {
      updateStatus("disconnected");
      scheduleReconnect();
    }
  }

  function scheduleReconnect() {
    if (reconnectTimer) clearTimeout(reconnectTimer);
    reconnectTimer = setTimeout(() => {
      if (!isConnected) {
        initWebSocketConnection();
      }
    }, 4000);
  }

  function loadChatHistory() {
    if (!tableId) return;
    function getBackendHost() {
      if (window.location.port === "8080") {
        return window.location.host;
      }
      return "localhost:8080";
    }

    const host = getBackendHost();
    fetch(`http://${host}/api/chat/history/${tableId}`)
      .then((res) => {
        if (!res.ok) return [];
        return res.json();
      })
      .then((history) => {
        if (history && history.length > 0) {
          const messagesContainer = document.getElementById("customer-chat-messages");
          const emptyState = document.getElementById("customer-chat-empty");
          if (emptyState) emptyState.style.display = "none";
          if (messagesContainer) {
            activeSessionMessages.length = 0;
            const rows = messagesContainer.querySelectorAll(".chat-message-row");
            rows.forEach((r) => r.remove());

            history.forEach((msg) => {
              activeSessionMessages.push(msg);
              appendMessageUI(msg);
            });
          }
        }
      })
      .catch(() => {});
  }

  function handleIncomingMessage(msg) {
    if (!msg) return;

    // Handle clear chat event when table is completed / reset
    if (msg.type === "CLEAR_CHAT") {
      activeSessionMessages.length = 0;
      const messagesContainer = document.getElementById("customer-chat-messages");
      if (messagesContainer) {
        messagesContainer.innerHTML = `
          <div class="chat-empty-state" id="customer-chat-empty">
            <i class="bi bi-chat-heart"></i>
            <p class="m-0 fw-bold">Xin chào quý khách!</p>
            <small>Bàn đã hoàn thành / đặt lại. Phiên trò chuyện mới đã sẵn sàng.</small>
          </div>
        `;
      }
      return;
    }

    if (!msg.content) return;

    // In-memory active session tracking (NEVER stored to localStorage/MongoDB)
    activeSessionMessages.push(msg);

    appendMessageUI(msg);

    // If message is from staff and window is closed
    if (msg.role === "STAFF") {
      playBeep();
      if (!isWindowOpen) {
        unreadCount++;
        const badge = document.getElementById("customer-chat-badge");
        if (badge) {
          badge.innerText = unreadCount;
          badge.classList.remove("d-none");
        }
        showToast(msg.content);
      }
    }
  }

  function sendCustomerMessage() {
    const input = document.getElementById("customer-chat-input");
    if (!input || !tableId) return;

    const content = input.value.trim();
    if (!content) return;

    if (!isConnected || !stompClient) {
      alert("Kết nối đang gián đoạn. Đang kết nối lại, vui lòng thử lại sau giây lát.");
      return;
    }

    const chatMsg = {
      idBan: tableId,
      sender: "Bàn " + tableId,
      role: "CUSTOMER",
      content: content,
      type: "CHAT",
    };

    try {
      stompClient.send("/app/chat.send", {}, JSON.stringify(chatMsg));
      input.value = "";
      input.focus();
    } catch (e) {
      console.error("Gửi tin nhắn thất bại:", e);
    }
  }

  // Initialize on DOM ready
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
      createChatUI();
      initWebSocketConnection();
    });
  } else {
    createChatUI();
    initWebSocketConnection();
  }
})();

