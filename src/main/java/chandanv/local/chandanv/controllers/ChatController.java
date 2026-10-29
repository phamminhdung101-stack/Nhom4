package chandanv.local.chandanv.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import chandanv.local.chandanv.models.dto.ChatMessage;
import chandanv.local.chandanv.repositories.BanAnRepository;
import chandanv.local.chandanv.security.AuditLog;
import chandanv.local.chandanv.security.RateLimiter;
import chandanv.local.chandanv.services.AuthService;
import chandanv.local.chandanv.services.InMemoryChatService;
import chandanv.local.chandanv.util.TextUtil;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ChatController {

    static final int MAX_LENGTH = 500;
    /** Chống spam chat: tối đa 10 tin / 10 giây mỗi kết nối. */
    static final int MAX_MESSAGES = 10;
    static final long WINDOW_MS = 10_000;

    private final SimpMessagingTemplate messagingTemplate;
    private final InMemoryChatService chatService;
    private final BanAnRepository banAnRepository;
    private final AuthService auth;
    private final RateLimiter rateLimiter;

    public ChatController(SimpMessagingTemplate messagingTemplate, InMemoryChatService chatService,
                          BanAnRepository banAnRepository, AuthService auth, RateLimiter rateLimiter) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
        this.banAnRepository = banAnRepository;
        this.auth = auth;
        this.rateLimiter = rateLimiter;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> session = headerAccessor.getSessionAttributes();
        if (session == null || session.get("role") == null || message == null) return;

        String sessionId = headerAccessor.getSessionId();
        if (!rateLimiter.tryAcquire("chat:" + sessionId, MAX_MESSAGES, WINDOW_MS)) {
            AuditLog.warn("CHAT_FLOOD", "session=" + sessionId + " role=" + session.get("role") + " ban=" + session.get("idBan"));
            return; // bỏ qua tin vượt giới hạn
        }

        // Nội dung lưu dạng văn bản thuần; client hiển thị bằng textContent nên không cần (và không được) escape 2 lần.
        String content = TextUtil.lamSach(message.getContent(), MAX_LENGTH);
        if (content == null) return;

        String role = (String) session.get("role");
        int idBan;
        if ("CUSTOMER".equals(role)) {
            Object idBanObj = session.get("idBan");
            if (!(idBanObj instanceof Number n)) return;
            idBan = n.intValue(); // luôn dùng bàn đã xác thực, bỏ qua idBan client gửi
            // Máy bị mời ra / phiên bàn đã kết thúc sau khi kết nối -> không cho chat tiếp
            if (!auth.tokenBanHopLe(idBan, (String) session.get("tableToken"))) return;
        } else if ("STAFF".equals(role)) {
            idBan = message.getIdBan();
            if (idBan <= 0 || banAnRepository.findByIdBan(idBan).isEmpty()) return;
        } else {
            return;
        }

        ChatMessage out = new ChatMessage(ChatMessage.MessageType.CHAT, idBan,
                (String) session.get("sender"), role, content);
        if ("CUSTOMER".equals(role)) out.setClientId(TextUtil.lamSach(message.getClientId(), 40));

        chatService.addMessage(idBan, out);
        messagingTemplate.convertAndSend("/topic/chat/table/" + idBan, out);
        if ("CUSTOMER".equals(role)) {
            messagingTemplate.convertAndSend("/topic/chat/staff", out); // badge + toast trên sơ đồ bàn
        }
    }

    @GetMapping("/api/chat/history/{idBan}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable int idBan, HttpServletRequest req) {
        auth.yeuCauBanHoacNhanVien(req, idBan);
        return chatService.getMessages(idBan);
    }

    /**
     * Khách gửi tin qua HTTP thường – dự phòng khi WebSocket/STOMP bị chặn (vd. iPhone + chứng chỉ HTTPS tự ký).
     * body {"content": "...", "clientId": "..."}. Cùng kiểm tra như STOMP: máy đã được duyệt, chống spam, 500 ký tự.
     */
    @PostMapping("/api/chat/{idBan}")
    @ResponseBody
    public ChatMessage guiTinQuaHttp(@PathVariable int idBan, @RequestBody(required = false) Map<String, String> body,
                                     HttpServletRequest req) {
        String token = req.getHeader(AuthService.TABLE_HEADER);
        if (!auth.tokenBanHopLe(idBan, token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Máy của bạn chưa được vào bàn hoặc phiên bàn đã kết thúc, vui lòng quét lại mã QR trên bàn");
        }
        if (!rateLimiter.tryAcquire("chat:" + token, MAX_MESSAGES, WINDOW_MS)) {
            AuditLog.warn("CHAT_FLOOD", "http ban=" + idBan);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bạn nhắn quá nhanh, vui lòng chờ vài giây");
        }
        String content = TextUtil.lamSach(body == null ? null : body.get("content"), MAX_LENGTH);
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tin nhắn trống");
        }
        ChatMessage out = new ChatMessage(ChatMessage.MessageType.CHAT, idBan, "Bàn " + idBan, "CUSTOMER", content);
        out.setClientId(TextUtil.lamSach(body.get("clientId"), 40));
        chatService.addMessage(idBan, out);
        messagingTemplate.convertAndSend("/topic/chat/table/" + idBan, out); // máy khác cùng bàn (đang dùng STOMP)
        messagingTemplate.convertAndSend("/topic/chat/staff", out);          // nhân viên: badge + toast + âm báo
        return out;
    }
}
