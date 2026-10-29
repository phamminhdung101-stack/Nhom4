package chandanv.local.chandanv.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import chandanv.local.chandanv.security.AuditLog;
import chandanv.local.chandanv.security.RateLimiter;
import chandanv.local.chandanv.services.AuthService;

/**
 * Raw WebSocket /ws: đẩy sự kiện đơn hàng / bàn / giỏ hàng tới trình duyệt ngay lập tức (không phải tải lại trang).
 *
 * Mỗi kết nối vào đúng 1 PHÒNG, và chỉ nhận sự kiện của phòng đó:
 *   KITCHEN, CASHIER   – trang quản trị, phải gửi token đăng nhập nhân viên.
 *   TABLE_{idBan}      – điện thoại của khách, phải gửi mã riêng của máy (X-Table-Token) đã được duyệt ở bàn đó.
 *                        Gửi mã in trên QR sẽ bị từ chối: mã QR chỉ dùng để xin vào bàn.
 *
 * Giao thức (JSON text frame):
 *   Client -> {"type":"JOIN_ROOM","room":"KITCHEN"|"CASHIER","token":"<token nhân viên>"}
 *   Client -> {"type":"JOIN_TABLE","idBan":1,"token":"<mã riêng của máy>"}
 *   Server -> {"type":"JOINED","room":"..."} | {"type":"ERROR","message":"..."}
 *   Server -> sự kiện, xem OrderEventPublisher: NEW_ORDER, ORDER_STATUS_UPDATED, TABLE_STATUS_UPDATED,
 *             CART_UPDATED, DEVICE_PENDING, LEFT_TABLE (máy bị mời ra / phiên bàn kết thúc).
 *
 * Phát hiện kết nối "chết" (điện thoại tắt màn hình, mất Wi-Fi mà không đóng kết nối): mỗi 25 giây server gửi
 * Ping frame, trình duyệt tự trả Pong. Kết nối không phản hồi gì quá 60 giây bị đóng và xoá khỏi phòng,
 * để server không gửi mãi vào kết nối đã mất. Client tự viết cũng có thể gửi text "ping" và nhận "pong".
 */
@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {

    public static final String ROOM_KITCHEN = "KITCHEN";
    public static final String ROOM_CASHIER = "CASHIER";

    private static final Logger log = LoggerFactory.getLogger(OrderWebSocketHandler.class);
    private static final long TIMEOUT_MS = 60_000;
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_LIMIT_BYTES = 256 * 1024;
    /** Chống spam: tối đa 30 tin nhắn / 10 giây cho mỗi kết nối, vượt -> đóng kết nối. */
    private static final int MAX_MESSAGES = 30;
    private static final long MESSAGE_WINDOW_MS = 10_000;

    /**
     * sessionId -> session đã bọc ConcurrentWebSocketSessionDecorator.
     * Bắt buộc bọc: WebSocketSession gốc KHÔNG cho 2 luồng gửi cùng lúc (sẽ ném IllegalStateException), mà ở đây
     * nhiều request (bếp đổi trạng thái, khách đặt món...) có thể cùng gửi tới 1 kết nối. Bản bọc xếp hàng
     * các tin, và tự ngắt kết nối nếu gửi quá 5 giây hoặc tồn quá 256 KB (máy khách mạng quá chậm).
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    /** sessionId -> room hiện tại (mỗi session chỉ ở 1 room). */
    private final Map<String, String> sessionRoom = new ConcurrentHashMap<>();
    /** room -> tập sessionId. */
    private final Map<String, Set<String>> rooms = new ConcurrentHashMap<>();
    /** sessionId -> thời điểm nhận dữ liệu/pong gần nhất. */
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();
    /** sessionId -> mã máy đã dùng để vào phòng bàn (kiểm tra lại khi máy bị mời ra / phiên kết thúc). */
    private final Map<String, String> sessionTableToken = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final RateLimiter rateLimiter;

    public OrderWebSocketHandler(ObjectMapper objectMapper, AuthService authService, RateLimiter rateLimiter) {
        this.objectMapper = objectMapper;
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    public static String tableRoom(int idBan) {
        return "TABLE_" + idBan;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(),
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_LIMIT_BYTES));
        lastSeen.put(session.getId(), System.currentTimeMillis());
        log.debug("WS connected {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        lastSeen.put(session.getId(), System.currentTimeMillis());
        if (!rateLimiter.tryAcquire("ws:" + session.getId(), MAX_MESSAGES, MESSAGE_WINDOW_MS)) {
            AuditLog.warn("WS_FLOOD", "session=" + session.getId() + " ip=" + remoteIp(session));
            closeQuietly(session, CloseStatus.POLICY_VIOLATION.withReason("Gửi quá nhiều tin nhắn"));
            return;
        }
        String payload = message.getPayload();

        if ("ping".equalsIgnoreCase(payload.trim())) {
            send(session.getId(), "pong");
            return;
        }

        JsonNode json;
        try {
            json = objectMapper.readTree(payload);
        } catch (Exception e) {
            sendJson(session.getId(), Map.of("type", "ERROR", "message", "Tin nhắn phải là JSON"));
            return;
        }

        String type = json.path("type").asText("");
        String token = json.path("token").asText("");
        switch (type) {
            case "JOIN_ROOM" -> {
                String room = json.path("room").asText("");
                if (!ROOM_KITCHEN.equals(room) && !ROOM_CASHIER.equals(room)) {
                    sendJson(session.getId(), Map.of("type", "ERROR", "message", "Room không hợp lệ"));
                } else if (authService.timNhanVien(token).isEmpty()) {
                    AuditLog.warn("WS_JOIN_DENIED", "room=" + room + " ip=" + remoteIp(session));
                    sendJson(session.getId(), Map.of("type", "ERROR", "message", "Cần đăng nhập nhân viên"));
                } else {
                    sessionTableToken.remove(session.getId());
                    join(session.getId(), room);
                }
            }
            case "JOIN_TABLE" -> {
                int idBan = json.path("idBan").asInt(-1);
                if (idBan > 0 && authService.tokenBanHopLe(idBan, token)) {
                    sessionTableToken.put(session.getId(), token);
                    join(session.getId(), tableRoom(idBan));
                } else {
                    AuditLog.warn("WS_JOIN_DENIED", "ban=" + idBan + " ip=" + remoteIp(session));
                    sendJson(session.getId(), Map.of("type", "ERROR", "message", "Mã QR của bàn không hợp lệ"));
                }
            }
            default -> sendJson(session.getId(), Map.of("type", "ERROR", "message", "Loại tin nhắn không hỗ trợ"));
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        lastSeen.put(session.getId(), System.currentTimeMillis());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("WS transport error {}: {}", session.getId(), exception.getMessage());
        remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        remove(session.getId());
        log.debug("WS closed {} {}", session.getId(), status);
    }

    /** Gửi Ping frame định kỳ và đóng các kết nối "chết". */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, WebSocketSession> e : sessions.entrySet()) {
            String id = e.getKey();
            WebSocketSession s = e.getValue();
            long seen = lastSeen.getOrDefault(id, 0L);
            try {
                if (!s.isOpen()) {
                    remove(id);
                } else if (now - seen > TIMEOUT_MS) {
                    log.info("WS {} không phản hồi Pong > {}s -> đóng", id, TIMEOUT_MS / 1000);
                    s.close(CloseStatus.SESSION_NOT_RELIABLE);
                    remove(id);
                } else {
                    s.sendMessage(new PingMessage(ByteBuffer.wrap("hb".getBytes(StandardCharsets.UTF_8))));
                }
            } catch (Exception ex) {
                remove(id);
            }
        }
    }

    /** Gửi một sự kiện (đối tượng sẽ được chuyển thành JSON) tới các room. */
    public void sendToRooms(Object event, String... targetRooms) {
        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Không serialize được sự kiện WebSocket", e);
            return;
        }
        for (String room : targetRooms) {
            Set<String> ids = rooms.get(room);
            if (ids == null) continue;
            for (String id : ids) {
                send(id, json);
            }
        }
    }

    public int soKetNoi() {
        return sessions.size();
    }

    /**
     * Gọi khi bàn vừa mời 1 máy ra hoặc kết thúc phiên. Kiểm tra lại mã của mọi kết nối trong phòng bàn,
     * kết nối nào có mã không còn hợp lệ thì đưa ra khỏi phòng và báo LEFT_TABLE.
     * Cần làm vậy vì quyền chỉ được kiểm tra lúc JOIN_TABLE: nếu không, kết nối đã mở từ trước vẫn tiếp tục
     * nhận giỏ hàng / đơn của khách mới ngồi vào bàn.
     */
    public void loaiMayHetQuyen(int idBan) {
        String room = tableRoom(idBan);
        Set<String> ids = rooms.get(room);
        if (ids == null) return;
        for (String id : ids) {
            String token = sessionTableToken.get(id);
            if (token == null || !authService.tokenBanHopLe(idBan, token)) {
                ids.remove(id);
                sessionRoom.remove(id, room);
                sessionTableToken.remove(id);
                sendJson(id, Map.of("type", "LEFT_TABLE", "idBan", idBan));
            }
        }
    }

    private static String remoteIp(WebSocketSession session) {
        return session.getRemoteAddress() == null ? "unknown" : session.getRemoteAddress().getAddress().getHostAddress();
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {
            // đã đóng
        }
        remove(session.getId());
    }

    private void join(String sessionId, String room) {
        String old = sessionRoom.put(sessionId, room);
        if (old != null && !old.equals(room)) {
            Set<String> oldSet = rooms.get(old);
            if (oldSet != null) oldSet.remove(sessionId);
        }
        rooms.computeIfAbsent(room, r -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sendJson(sessionId, Map.of("type", "JOINED", "room", room));
    }

    private void remove(String sessionId) {
        rateLimiter.reset("ws:" + sessionId);
        sessions.remove(sessionId);
        lastSeen.remove(sessionId);
        sessionTableToken.remove(sessionId);
        String room = sessionRoom.remove(sessionId);
        if (room != null) {
            Set<String> set = rooms.get(room);
            if (set != null) set.remove(sessionId);
        }
    }

    private void sendJson(String sessionId, Object obj) {
        try {
            send(sessionId, objectMapper.writeValueAsString(obj));
        } catch (Exception e) {
            log.warn("Không gửi được JSON tới {}", sessionId);
        }
    }

    private void send(String sessionId, String text) {
        WebSocketSession s = sessions.get(sessionId);
        if (s == null || !s.isOpen()) {
            remove(sessionId);
            return;
        }
        try {
            s.sendMessage(new TextMessage(text));
        } catch (Exception e) { // IOException, IllegalStateException, SessionLimitExceededException...
            log.debug("Gửi WS thất bại tới {}: {}", sessionId, e.getMessage());
            remove(sessionId);
        }
    }
}
