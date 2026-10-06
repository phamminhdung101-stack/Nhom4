package chandanv.local.chandanv.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {

    // Store sessions by room (CUSTOMER, KITCHEN, CASHIER)
    private final Map<String, CopyOnWriteArrayList<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderWebSocketHandler() {
        rooms.put("CUSTOMER", new CopyOnWriteArrayList<>());
        rooms.put("KITCHEN", new CopyOnWriteArrayList<>());
        rooms.put("CASHIER", new CopyOnWriteArrayList<>());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // We add them to rooms when they send a JOIN_ROOM message
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        // Ignore ping messages
        if ("ping".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            if (jsonNode.has("type") && "JOIN_ROOM".equals(jsonNode.get("type").asText())) {
                String room = jsonNode.get("room").asText();
                joinRoom(session, room);
            }
        } catch (Exception e) {
            // Not JSON or other error, ignore
        }
    }

    private void joinRoom(WebSocketSession session, String room) {
        if (rooms.containsKey(room)) {
            rooms.get(room).add(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        rooms.values().forEach(list -> list.remove(session));
    }

    public void broadcastToRoom(String room, String message) {
        if (rooms.containsKey(room)) {
            CopyOnWriteArrayList<WebSocketSession> roomSessions = rooms.get(room);
            for (WebSocketSession session : roomSessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void broadcast(String message) {
        rooms.values().forEach(roomSessions -> {
            for (WebSocketSession session : roomSessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}

