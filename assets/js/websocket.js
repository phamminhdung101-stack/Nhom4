// websocket.js
let socket = null;
let reconnectInterval = 3000;
let isConnected = false;
let clientType = "CUSTOMER"; // Default, can be overridden by scripts

function connectWebSocket() {
    socket = new WebSocket("ws://localhost:8080/ws");

    socket.onopen = function () {
        console.log("🟢 WebSocket connected");
        isConnected = true;
        
        // Join room
        socket.send(JSON.stringify({ type: "JOIN_ROOM", room: clientType }));
        
        // Update UI if there's a status element
        const wsStatus = document.getElementById("ws-status");
        if (wsStatus) {
            wsStatus.innerHTML = "🟢 Connected";
            wsStatus.style.color = "green";
        }
    };

    socket.onmessage = function (event) {
        console.log("WebSocket message received:", event.data);
        if (event.data === "pong") return; // heartbeat
        
        try {
            const data = JSON.parse(event.data);
            handleMessage(data);
        } catch (e) {
            console.error("Error parsing message", e);
        }
    };

    socket.onclose = function () {
        console.log("🔴 WebSocket disconnected");
        isConnected = false;
        const wsStatus = document.getElementById("ws-status");
        if (wsStatus) {
            wsStatus.innerHTML = "🔴 Disconnected";
            wsStatus.style.color = "red";
        }
        
        // Auto-reconnect
        setTimeout(connectWebSocket, reconnectInterval);
    };

    socket.onerror = function (error) {
        console.error("WebSocket error", error);
        socket.close();
    };
}

function handleMessage(data) {
    if (data.type === "NEW_ORDER") {
        console.log("New order received:", data);
        if (typeof window.handleNewOrder === "function") {
            window.handleNewOrder(data);
        }
    } else if (data.type === "ORDER_STATUS_UPDATED") {
        console.log("Order status updated:", data);
        if (typeof window.handleOrderStatus === "function") {
            window.handleOrderStatus(data);
        }
    }
}

// Simple heartbeat to keep connection alive
setInterval(() => {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send("ping");
    }
}, 30000);

// Expose functions globally
window.connectWebSocket = connectWebSocket;
window.socket = socket;

