package chandanv.local.chandanv.websocket;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import chandanv.local.chandanv.models.entity.Admin;
import chandanv.local.chandanv.services.AuthService;

/**
 * Kiểm soát quyền cho STOMP chat (/ws-chat). STOMP tự định tuyến tin theo topic, nên nếu không chặn ở đây thì
 * bất kỳ ai cũng có thể SUBSCRIBE /topic/chat/table/5 để đọc trộm chat của bàn 5, hoặc gửi tin giả danh bàn khác.
 *  CONNECT:   STAFF phải gửi header adminToken còn hạn; CUSTOMER phải gửi idBan + tableToken
 *             (mã riêng của máy đã được duyệt ở bàn đó, không phải mã in trên QR).
 *  SUBSCRIBE: khách chỉ được nghe kênh của bàn mình; nhân viên nghe /topic/chat/staff và /topic/chat/table/{n}.
 */
@Component
public class ChatSecurityChannelInterceptor implements ChannelInterceptor {

    private static final Pattern TABLE_TOPIC = Pattern.compile("^/topic/chat/table/\\d{1,6}$");

    /** Kết nối chat của khách: sessionId -> (bàn, mã máy) – để chặn tin tới máy đã bị mời ra / hết phiên. */
    private record KhachChat(int idBan, String token) {}
    private final Map<String, KhachChat> khachTheoPhien = new ConcurrentHashMap<>();

    private final AuthService authService;

    public ChatSecurityChannelInterceptor(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Lọc tin GỬI ĐI (clientOutboundChannel): máy khách bị mời ra / phiên bàn đã kết thúc không còn nhận
     * tin chat của bàn qua kết nối cũ. Riêng tin CLEAR_CHAT (báo phiên chat đã đặt lại) vẫn cho qua.
     */
    public Message<?> locTinGuiDi(Message<?> message) {
        if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) return message;
        KhachChat khach = khachTheoPhien.get(SimpMessageHeaderAccessor.getSessionId(message.getHeaders()));
        if (khach == null || authService.tokenBanHopLe(khach.idBan(), khach.token())) return message;
        Object payload = message.getPayload();
        String body = payload instanceof byte[] b ? new String(b, StandardCharsets.UTF_8) : String.valueOf(payload);
        return body.contains("\"CLEAR_CHAT\"") ? message : null;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (StompCommand.DISCONNECT.equals(command)) {
            if (accessor.getSessionId() != null) khachTheoPhien.remove(accessor.getSessionId());
        } else if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            Map<String, Object> attrs = accessor.getSessionAttributes();
            if (attrs == null || attrs.get("role") == null) {
                throw new SecurityException("Chưa xác thực kết nối chat");
            }
        }
        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) {
            throw new IllegalArgumentException("Không tìm thấy phiên kết nối");
        }
        String role = accessor.getFirstNativeHeader("role");

        if ("STAFF".equalsIgnoreCase(role)) {
            Admin admin = authService.timNhanVien(accessor.getFirstNativeHeader("adminToken"))
                    .orElseThrow(() -> new IllegalArgumentException("Phiên đăng nhập nhân viên không hợp lệ"));
            attrs.put("role", "STAFF");
            attrs.put("adminId", admin.getId());
            attrs.put("sender", admin.getUsername());
            return;
        }

        String idBanStr = accessor.getFirstNativeHeader("idBan");
        int idBan;
        try {
            idBan = Integer.parseInt(idBanStr == null ? "" : idBanStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mã bàn không hợp lệ: " + idBanStr);
        }
        String tableToken = accessor.getFirstNativeHeader("tableToken");
        if (!authService.tokenBanHopLe(idBan, tableToken)) {
            throw new IllegalArgumentException("Máy chưa được vào bàn " + idBan + " hoặc phiên bàn đã kết thúc");
        }
        attrs.put("role", "CUSTOMER");
        attrs.put("idBan", idBan);
        attrs.put("tableToken", tableToken); // kiểm tra lại mỗi tin (máy bị mời ra thì không chat tiếp được)
        if (accessor.getSessionId() != null) khachTheoPhien.put(accessor.getSessionId(), new KhachChat(idBan, tableToken));
        attrs.put("sender", "Bàn " + idBan); // tên do server đặt, khách không tự đổi được
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (destination == null || attrs == null) {
            throw new SecurityException("Không tìm thấy phiên kết nối hợp lệ");
        }
        Object role = attrs.get("role");

        if ("CUSTOMER".equals(role)) {
            Object idBanObj = attrs.get("idBan");
            if (!(idBanObj instanceof Number n) || !destination.equals("/topic/chat/table/" + n.intValue())) {
                throw new SecurityException("Khách hàng chỉ được phép nhận tin nhắn từ bàn của mình");
            }
        } else if ("STAFF".equals(role)) {
            if (!destination.equals("/topic/chat/staff") && !TABLE_TOPIC.matcher(destination).matches()) {
                throw new SecurityException("Kênh nhận tin không hợp lệ cho nhân viên");
            }
        } else {
            throw new SecurityException("Vai trò không xác định");
        }
    }
}
