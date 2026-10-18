package chandanv.local.chandanv.controllers;

import chandanv.local.chandanv.models.dto.ChatMessage;
import chandanv.local.chandanv.services.InMemoryChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private InMemoryChatService inMemoryChatService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return;
        }

        String sessionRole = (String) sessionAttributes.get("role");
        if (sessionRole == null) {
            return;
        }

        // Validate content
        String rawContent = message.getContent();
        if (rawContent == null || rawContent.trim().isEmpty()) {
            return;
        }

        // Enforce maximum message length (500 chars)
        String trimmed = rawContent.trim();
        if (trimmed.length() > 500) {
            trimmed = trimmed.substring(0, 500);
        }

        // Escape HTML special characters to prevent XSS attacks while keeping Vietnamese diacritics
        String safeContent = sanitizeHtml(trimmed);
        message.setContent(safeContent);
        message.setTimestamp(System.currentTimeMillis());
        message.setType(ChatMessage.MessageType.CHAT);

        int targetTableId;

        if ("CUSTOMER".equals(sessionRole)) {
            Object idBanObj = sessionAttributes.get("idBan");
            if (idBanObj == null) {
                return;
            }
            targetTableId = (int) idBanObj;

            // Security: Always bind to the customer's authenticated table ID
            message.setIdBan(targetTableId);
            message.setRole("CUSTOMER");
            String sender = (String) sessionAttributes.get("sender");
            message.setSender(sender != null && !sender.isBlank() ? sender : "Bàn " + targetTableId);

            // Store in temporary in-memory RAM cache until table reset
            inMemoryChatService.addMessage(targetTableId, message);

            // Forward to table channel (customer & staff viewing this table will receive)
            messagingTemplate.convertAndSend("/topic/chat/table/" + targetTableId, message);

            // Forward to staff notification channel (so table diagram can show unread badge & toast)
            messagingTemplate.convertAndSend("/topic/chat/staff", message);

        } else if ("STAFF".equals(sessionRole)) {
            targetTableId = message.getIdBan();
            if (targetTableId <= 0) {
                return;
            }

            message.setRole("STAFF");
            String sender = (String) sessionAttributes.get("sender");
            message.setSender(sender != null && !sender.isBlank() ? sender : "Nhân viên");

            // Store in temporary in-memory RAM cache until table reset
            inMemoryChatService.addMessage(targetTableId, message);

            // Forward to table channel (customer at table & staff will receive)
            messagingTemplate.convertAndSend("/topic/chat/table/" + targetTableId, message);
        }
    }

    @GetMapping("/api/chat/history/{idBan}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable int idBan) {
        return inMemoryChatService.getMessages(idBan);
    }

    private String sanitizeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
