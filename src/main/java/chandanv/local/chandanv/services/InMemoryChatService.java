package chandanv.local.chandanv.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import chandanv.local.chandanv.models.dto.ChatMessage;

/** Lưu tạm tin nhắn chat theo bàn trong RAM (không ghi MongoDB), xoá khi bàn hoàn thành/reset. */
@Service
public class InMemoryChatService {

    private static final int MAX_MESSAGES_PER_TABLE = 100;

    private final Map<Integer, List<ChatMessage>> tableChatCache = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public InMemoryChatService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void addMessage(int idBan, ChatMessage message) {
        if (idBan <= 0 || message == null) return;
        List<ChatMessage> list = tableChatCache.computeIfAbsent(idBan, k -> new ArrayList<>());
        synchronized (list) {
            list.add(message);
            while (list.size() > MAX_MESSAGES_PER_TABLE) {
                list.remove(0);
            }
        }
    }

    public List<ChatMessage> getMessages(int idBan) {
        List<ChatMessage> list = tableChatCache.get(idBan);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    /** Tin hệ thống (vd. "Đơn #DH-5 đã bị huỷ") hiện trong khung chat của bàn cho cả khách và nhân viên. */
    public void thongBao(int idBan, String text) {
        if (idBan <= 0) return;
        ChatMessage msg = new ChatMessage(ChatMessage.MessageType.NOTIFICATION, idBan, "Hệ thống", "SYSTEM", text);
        addMessage(idBan, msg);
        messagingTemplate.convertAndSend("/topic/chat/table/" + idBan, msg);
        messagingTemplate.convertAndSend("/topic/chat/staff", msg);
    }

    public void clearTableChat(int idBan) {
        if (idBan <= 0) return;
        tableChatCache.remove(idBan);
        ChatMessage clearMsg = systemMessage(idBan, "Phiên trò chuyện của bàn " + idBan + " đã được đặt lại.");
        messagingTemplate.convertAndSend("/topic/chat/table/" + idBan, clearMsg);
        messagingTemplate.convertAndSend("/topic/chat/staff", clearMsg);
    }

    public void clearAll() {
        for (Integer idBan : new ArrayList<>(tableChatCache.keySet())) {
            messagingTemplate.convertAndSend("/topic/chat/table/" + idBan,
                    systemMessage(idBan, "Toàn bộ trạng thái bàn và phiên chat đã được đặt lại."));
        }
        tableChatCache.clear();
        messagingTemplate.convertAndSend("/topic/chat/staff", systemMessage(0, "Đã reset toàn bộ bàn."));
    }

    private static ChatMessage systemMessage(int idBan, String text) {
        return new ChatMessage(ChatMessage.MessageType.CLEAR_CHAT, idBan, "Hệ thống", "SYSTEM", text);
    }
}
