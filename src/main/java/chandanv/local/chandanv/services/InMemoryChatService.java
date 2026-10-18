package chandanv.local.chandanv.services;

import chandanv.local.chandanv.models.dto.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class InMemoryChatService {

    // In-memory cache: idBan -> List of ChatMessage
    // Strictly stored in RAM until table status is completed or reset. ZERO MongoDB persistence!
    private final Map<Integer, List<ChatMessage>> tableChatCache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public void addMessage(int idBan, ChatMessage message) {
        if (idBan <= 0 || message == null) return;

        List<ChatMessage> list = tableChatCache.computeIfAbsent(idBan, k -> new CopyOnWriteArrayList<>());
        list.add(message);

        // Keep maximum 100 messages per active meal session in RAM
        if (list.size() > 100) {
            list.remove(0);
        }
    }

    public List<ChatMessage> getMessages(int idBan) {
        List<ChatMessage> list = tableChatCache.get(idBan);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(list);
    }

    public void clearTableChat(int idBan) {
        if (idBan <= 0) return;

        tableChatCache.remove(idBan);

        // Notify both customer and staff that the session for this table was reset
        if (messagingTemplate != null) {
            ChatMessage clearMsg = new ChatMessage(
                    ChatMessage.MessageType.CLEAR_CHAT,
                    idBan,
                    "Hệ thống",
                    "SYSTEM",
                    "Phiên trò chuyện của bàn " + idBan + " đã được đặt lại."
            );
            messagingTemplate.convertAndSend("/topic/chat/table/" + idBan, clearMsg);
            messagingTemplate.convertAndSend("/topic/chat/staff", clearMsg);
        }
    }

    public void clearAll() {
        for (Integer idBan : tableChatCache.keySet()) {
            if (messagingTemplate != null) {
                ChatMessage clearMsg = new ChatMessage(
                        ChatMessage.MessageType.CLEAR_CHAT,
                        idBan,
                        "Hệ thống",
                        "SYSTEM",
                        "Toàn bộ trạng thái bàn và phiên chat đã được đặt lại."
                );
                messagingTemplate.convertAndSend("/topic/chat/table/" + idBan, clearMsg);
            }
        }

        tableChatCache.clear();

        if (messagingTemplate != null) {
            ChatMessage clearMsg = new ChatMessage(
                    ChatMessage.MessageType.CLEAR_CHAT,
                    0,
                    "Hệ thống",
                    "SYSTEM",
                    "Đã reset toàn bộ bàn."
            );
            messagingTemplate.convertAndSend("/topic/chat/staff", clearMsg);
        }
    }
}

